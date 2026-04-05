package com.aiburst.mag.event;

/**
 * 任务核查结论为 PASS 并已写入 DONE 后，事务提交前已发布：供监听器触发项目经理复盘编排。
 *
 * @param projectId    项目 id
 * @param taskId       刚结项的任务 id
 * @param actingUserId 与核查提交/工具调用一致的触发用户（用于 PM 编排通道归属）
 */
public record MagTaskVerifiedPassEvent(long projectId, long taskId, long actingUserId) {}
