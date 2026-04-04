package com.aiburst.mag.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public final class MagThreadRunWorkflowImpl implements MagThreadRunWorkflow {

    private final MagOrchestrationActivities activities =
            Workflow.newActivityStub(
                    MagOrchestrationActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).build());

    @Override
    public String execute(long threadId, long triggerUserId) {
        return activities.executeThreadRun(threadId, triggerUserId);
    }
}
