package com.aiburst.mag.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Optional;

/**
 * 通过 TCP 探测 Temporal Frontend 端口是否可达（不引入 Temporal Java SDK）。
 */
@Component
@RequiredArgsConstructor
public class MagTemporalConnectivityChecker {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    private final MagTemporalProperties properties;

    /**
     * @return empty 表示探测成功；否则为失败原因（短句，适合直接展示）
     */
    public Optional<String> verifyReachable() {
        String target = properties.getTarget();
        if (target == null || target.isBlank()) {
            return Optional.of("aiburst.mag.temporal.target 未配置");
        }
        int colon = target.lastIndexOf(':');
        if (colon <= 0 || colon >= target.length() - 1) {
            return Optional.of("target 格式应为 host:port，当前：" + target);
        }
        String host = target.substring(0, colon).trim();
        String portStr = target.substring(colon + 1).trim();
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return Optional.of("端口无效：" + portStr);
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT.toMillis());
            return Optional.empty();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Optional.of(msg);
        }
    }
}
