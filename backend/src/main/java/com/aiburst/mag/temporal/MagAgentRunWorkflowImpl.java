package com.aiburst.mag.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public final class MagAgentRunWorkflowImpl implements MagAgentRunWorkflow {

    private final MagOrchestrationActivities activities =
            Workflow.newActivityStub(
                    MagOrchestrationActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).build());

    @Override
    public String execute(long agentId, long triggerUserId, String instruction, long taskContextTaskId) {
        return activities.executeAgentRun(agentId, triggerUserId, instruction, taskContextTaskId);
    }
}
