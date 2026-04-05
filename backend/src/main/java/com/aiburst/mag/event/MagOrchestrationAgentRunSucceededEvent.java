package com.aiburst.mag.event;

import java.time.LocalDateTime;

/**
 * Agent 编排 Activity 成功结束且数据库行已更新为 SUCCEEDED 之后、由事务提交后监听器消费：
 * 用于在「任务关联的执行方编排成功 + 有产出物」时自动申报完成。
 *
 * @param projectId          项目 id
 * @param taskId             编排关联任务（仅派工/自动执行等写入 task_id 的场景非空）
 * @param agentId            本次编排的 Agent
 * @param triggerUserId      触发编排的用户（与申报完成时的 acting user 一致）
 * @param outputWindowStart  产出物时间窗起点（通常取 {@code mag_orchestration_run.started_at}）
 * @param resultSummary      Activity 成功时的结果摘要（编排最终回复）
 */
public record MagOrchestrationAgentRunSucceededEvent(
        long projectId,
        long taskId,
        long agentId,
        long triggerUserId,
        LocalDateTime outputWindowStart,
        String resultSummary) {}
