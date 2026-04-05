package com.aiburst.mag.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MagOrchestrationActivities {

    @ActivityMethod
    String executeAgentRun(long agentId, long triggerUserId, String instruction);

    @ActivityMethod
    String executeThreadRun(long threadId, long triggerUserId);
}
