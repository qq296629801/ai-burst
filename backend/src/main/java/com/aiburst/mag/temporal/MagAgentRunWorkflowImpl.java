package com.aiburst.mag.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Activity 超时由 {@link #execute} 入参决定（启动端从 {@code aiburst.mag.temporal.activity-start-to-close-minutes} 传入），
 * 写入调度事件历史，避免仅改 Worker 默认值导致回放非确定性。
 */
public final class MagAgentRunWorkflowImpl implements MagAgentRunWorkflow {

    private static int clampActivityMinutes(int minutes) {
        int m = minutes;
        if (m < 1) {
            m = 1;
        }
        if (m > 480) {
            m = 480;
        }
        return m;
    }

    @Override
    public String execute(
            long agentId,
            long triggerUserId,
            String instruction,
            long taskContextTaskId,
            int activityStartToCloseMinutes) {
        int m = clampActivityMinutes(activityStartToCloseMinutes);
        MagOrchestrationActivities activities =
                Workflow.newActivityStub(
                        MagOrchestrationActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofMinutes(m))
                                .build());
        return activities.executeAgentRun(agentId, triggerUserId, instruction, taskContextTaskId);
    }
}
