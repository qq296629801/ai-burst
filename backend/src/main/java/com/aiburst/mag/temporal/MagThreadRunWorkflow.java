package com.aiburst.mag.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MagThreadRunWorkflow {

    /**
     * @param activityStartToCloseMinutes 同 {@link MagAgentRunWorkflow#execute(long, long, String, long, int)}
     */
    @WorkflowMethod
    String execute(long threadId, long triggerUserId, int activityStartToCloseMinutes);
}
