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
     * 关联任务的 Agent 编排（Temporal Activity）成功结束后，若任务仍为进行中且编排最终回复非空，是否自动申报完成
     * （进入已完成），见 {@link com.aiburst.mag.service.MagTaskService#tryAutoSubmitCompleteAfterSuccessfulAgentOrchestration}。
     * 不替代人工「申报完成」接口。
     */
    private boolean autoSubmitCompleteOnOrchestrationSuccess = true;

    /**
     * 任务进入 DONE 后，是否自动触发项目经理 Agent 一轮复盘（list 任务/模块、按需继续派工或声明阶段闭环）。
     */
    private boolean autoPmReviewAfterTaskDone = true;

    /**
     * 当项目内全部任务均为 DONE 时，是否写入 {@code mag_alert_event}（INFO / PROJECT_ALL_TASKS_DONE），
     * 供工作台「告警」列表提示用户：本阶段已处理完毕，需再派发新任务。
     */
    private boolean notifyWhenAllTasksDone = true;
}
