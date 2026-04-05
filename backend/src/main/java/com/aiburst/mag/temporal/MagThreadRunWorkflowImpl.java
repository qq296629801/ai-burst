package com.aiburst.mag.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public final class MagThreadRunWorkflowImpl implements MagThreadRunWorkflow {

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
    public String execute(long threadId, long triggerUserId, int activityStartToCloseMinutes) {
        int m = clampActivityMinutes(activityStartToCloseMinutes);
        MagOrchestrationActivities activities =
                Workflow.newActivityStub(
                        MagOrchestrationActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofMinutes(m))
                                .build());
        return activities.executeThreadRun(threadId, triggerUserId);
    }
}
