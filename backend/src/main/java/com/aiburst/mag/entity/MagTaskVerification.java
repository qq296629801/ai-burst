package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagTaskVerification {
    private Long id;
    private Long taskId;
    private String result;
    private Long verifierAgentId;
    private String rationale;
    private String evidenceSummary;
    private String searchTraceJson;
    private LocalDateTime createdAt;
}
