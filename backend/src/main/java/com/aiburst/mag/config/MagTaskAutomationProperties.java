package com.aiburst.mag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 派工与申报完成后的自动化（Temporal + AgentScope）。
 */
@Data
@ConfigurationProperties(prefix = "aiburst.mag.task")
public class MagTaskAutomationProperties {

    /**
     * 项目经理派工（及待处理态改派）后，是否自动开始任务并触发 assignee Agent 执行。
     */
    private boolean autoStartOnDispatch = true;

    /**
     * 执行方「申报完成」进入待核查后，是否自动将任务置为核查中并触发项目内 VERIFY Agent 编排。
     * 若无可用 VERIFY Agent（未启用或未绑通道），仅记告警，任务保持 PENDING_VERIFY，可由人工调用核查 API。
     */
    private boolean autoVerifyOnSubmitComplete = true;
}
