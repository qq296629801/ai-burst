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

    /**
     * 关联任务的 Agent 编排（Temporal Activity）成功结束后，若任务仍为进行中且存在产出物（本次编排时间窗内
     * {@code mag_agent_improvement_log} 至少一条，见
     * {@link com.aiburst.mag.service.MagTaskService#tryAutoSubmitCompleteAfterSuccessfulAgentOrchestration}），
     * 是否自动申报完成（进入待核查）。不替代人工「申报完成」接口；VERIFY Agent 的核查编排不满足执行方条件，不会误触发。
     */
    private boolean autoSubmitCompleteOnOrchestrationSuccess = true;

    /**
     * 核查 PASS 任务进入 DONE 后，是否自动触发项目经理 Agent 一轮复盘（list 任务/模块、按需继续派工或声明阶段闭环）。
     * 与派工→执行→申报→核查链衔接：PM 若继续派工，新任务再次走完闭环后会再次触发复盘，直至 PM 判断无需再派且任务均 DONE。
     */
    private boolean autoPmReviewAfterVerifiedPass = true;

    /**
     * 当项目内全部任务均为 DONE 时，是否写入 {@code mag_alert_event}（INFO / PROJECT_ALL_TASKS_DONE），
     * 供工作台「告警」列表提示用户：本阶段已处理完毕，需再派发新任务。
     */
    private boolean notifyWhenAllTasksDone = true;
}
