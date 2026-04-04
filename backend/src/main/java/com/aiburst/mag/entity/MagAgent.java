package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagAgent {
    private Long id;
    private Long projectId;
    private Long parentAgentId;
    private String roleType;
    private String name;
    private Long llmChannelId;
    private String systemPromptProfile;
    private String extraJson;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
