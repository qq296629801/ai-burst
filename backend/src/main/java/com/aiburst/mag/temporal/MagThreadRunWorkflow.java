package com.aiburst.mag.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MagThreadRunWorkflow {

    @WorkflowMethod
    String execute(long threadId, long triggerUserId);
}
