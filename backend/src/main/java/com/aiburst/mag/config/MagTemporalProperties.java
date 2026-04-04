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
}
