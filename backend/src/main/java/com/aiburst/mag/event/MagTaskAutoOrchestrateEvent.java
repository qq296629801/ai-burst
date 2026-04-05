package com.aiburst.mag.event;

/**
 * 事务提交后触发：对指定任务自动开始并拉起执行 Agent 编排。
 */
public record MagTaskAutoOrchestrateEvent(long taskId, long projectId, long dispatchUserId, String reason) {}
