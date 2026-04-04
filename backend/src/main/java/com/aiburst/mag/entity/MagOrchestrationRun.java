package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagOrchestrationRun {
    private Long id;
    private Long projectId;
    private String runKind;
    private Long agentId;
    private Long threadId;
    private String workflowId;
    private String status;
    private String message;
    private String resultSummary;
    private Long triggerUserId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
