package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagRequirementPoolItem {
    private Long id;
    private Long projectId;
    private String state;
    private Long revisionId;
    private String anchorJson;
    private String payloadJson;
    private Long assignedDeciderUserId;
    private String temporalWorkflowId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
