package com.aiburst.mag.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Temporal 触发前置条件：未启用或 Frontend 不可达时返回阻断结果。
 */
@Component
@RequiredArgsConstructor
public class MagTemporalGate {

    private static final String MSG_DISABLED =
            "未启用 Temporal：请在配置中设置 aiburst.mag.temporal.enabled=true，"
                    + "并启动 Temporal Server（可参考仓库 docker-compose.temporal.yml）。";

    private final MagTemporalProperties temporalProperties;
    private final MagTemporalConnectivityChecker connectivityChecker;

    /**
     * @return 有值表示不应启动 Workflow，应将该 Map 直接作为接口 data 返回
     */
    public Optional<Map<String, Object>> blockIfAny(String resourceHint) {
        if (!temporalProperties.isEnabled()) {
            return Optional.of(baseBlock(false, false, MSG_DISABLED, resourceHint));
        }
        Optional<String> err = connectivityChecker.verifyReachable();
        if (err.isPresent()) {
            String msg =
                    "已启用 Temporal，但无法连接 "
                            + temporalProperties.getTarget()
                            + "："
                            + err.get();
            return Optional.of(baseBlock(true, false, msg, resourceHint));
        }
        return Optional.empty();
    }

    private static Map<String, Object> baseBlock(
            boolean temporalEnabled, boolean temporalReachable, String message, String hint) {
        Map<String, Object> out = new HashMap<>();
        out.put("accepted", false);
        out.put("temporalEnabled", temporalEnabled);
        out.put("temporalReachable", temporalReachable);
        out.put("message", message);
        if (hint != null) {
            out.put("hint", hint);
        }
        return out;
    }
}
