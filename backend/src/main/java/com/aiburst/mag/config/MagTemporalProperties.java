package com.aiburst.mag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MAG 与 Temporal 对接开关；未启用时不尝试连接，触发编排会明确提示。
 */
@Data
@ConfigurationProperties(prefix = "aiburst.mag.temporal")
public class MagTemporalProperties {

    /**
     * 是否启用 Temporal 连通性检查与编排前置校验；false 时触发接口返回 accepted=false 并说明原因。
     */
    private boolean enabled = false;

    /**
     * Temporal 前端 gRPC 地址，如 127.0.0.1:7233（与 docker-compose.temporal.yml 一致）。
     */
    private String target = "127.0.0.1:7233";

    /** 命名空间；Workflow 接入后使用，当前仅作配置预留 */
    private String namespace = "default";

    /** Worker 与 startWorkflow 共用的任务队列名 */
    private String taskQueue = "mag-orchestration";

    /**
     * Activity {@code StartToClose} 超时（分钟），覆盖 Agent/线程编排单次 Activity（含 AgentScope 整段执行）。
     * 过短易导致 LLM 慢响应时出现 NOT_FOUND: activity already timed out；建议 ≥15。
     */
    private int activityStartToCloseMinutes = 30;

    /** 供 Worker 注册使用，限制在合理区间，避免配置误填 */
    public int getEffectiveActivityStartToCloseMinutes() {
        int m = activityStartToCloseMinutes;
        if (m < 1) {
            return 1;
        }
        if (m > 480) {
            return 480;
        }
        return m;
    }
}
