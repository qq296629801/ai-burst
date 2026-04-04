package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagTask {
    private Long id;
    private Long projectId;
    private Long moduleId;
    private String title;
    private String description;
    private String state;
    private Long assigneeAgentId;
    private Long reporterAgentId;
    private String requirementRef;
    private String temporalWorkflowId;
    private String blockReason;
    private Long blockedByAgentId;
    private Integer rowVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
