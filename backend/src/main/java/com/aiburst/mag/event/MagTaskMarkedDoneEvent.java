package com.aiburst.mag.event;

/**
 * 任务经申报完成等路径进入 {@code DONE} 后发布（事务提交前）：供监听器触发项目经理复盘、全任务完成通知等。
 *
 * @param actingUserId 触发用户（人工申报或编排归属用户）
 */
public record MagTaskMarkedDoneEvent(long projectId, long taskId, long actingUserId) {}
