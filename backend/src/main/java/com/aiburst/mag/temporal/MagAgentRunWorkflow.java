package com.aiburst.mag.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MagAgentRunWorkflow {

    @WorkflowMethod
    String execute(long agentId, long triggerUserId);
}
