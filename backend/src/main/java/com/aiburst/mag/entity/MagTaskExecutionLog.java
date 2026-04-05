package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagTaskExecutionLog {
    private Long id;
    private Long projectId;
    private Long taskId;
    private Long agentId;
    private Long orchestrationRunId;
    private String workflowId;
    /** SUCCEEDED | FAILED | TRIGGER_REJECTED */
    private String executionOutcome;
    private String resultSummary;
    private Long triggerUserId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
