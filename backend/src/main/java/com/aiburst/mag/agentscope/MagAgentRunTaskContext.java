package com.aiburst.mag.agentscope;

/**
 * 根级 Agent 编排关联的 {@code mag_task.id}（Temporal Activity 传入），供嵌套 A2A 写入同一任务沟通线程。
 */
public final class MagAgentRunTaskContext {

    private static final ThreadLocal<Long> TASK_ID = new ThreadLocal<>();

    private MagAgentRunTaskContext() {
    }

    public static void set(Long taskId) {
        if (taskId != null) {
            TASK_ID.set(taskId);
        } else {
            TASK_ID.remove();
        }
    }

    public static void clear() {
        TASK_ID.remove();
    }

    public static Long get() {
        return TASK_ID.get();
    }
}
