package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagTaskFlowEvent {
    private Long id;
    private Long projectId;
    private Long taskId;
    private String eventType;
    private String actorType;
    private Long actorAgentId;
    private String summary;
    private String detailJson;
    private LocalDateTime createdAt;
}
