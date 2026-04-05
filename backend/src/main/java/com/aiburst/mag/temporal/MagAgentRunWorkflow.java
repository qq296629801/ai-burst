package com.aiburst.mag.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MagAgentRunWorkflow {

    /**
     * @param taskContextTaskId 关联 {@code mag_task.id}，无则传 0（沟通落库与任务线程解析）
     */
    @WorkflowMethod
    String execute(long agentId, long triggerUserId, String instruction, long taskContextTaskId);
}
