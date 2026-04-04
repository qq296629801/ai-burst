package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagAgentImprovementLog {
    private Long id;
    private Long projectId;
    private Long agentId;
    private String changeType;
    private String summary;
    private String detailJson;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}
