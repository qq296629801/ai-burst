package com.aiburst.mag.event;

/**
 * 任务申报完成并进入 {@code PENDING_VERIFY} 后，事务提交后触发：可选自动进入核查中并拉起 VERIFY Agent 编排。
 */
public record MagTaskPendingVerifyEvent(long taskId, long projectId, long actingUserId) {}
