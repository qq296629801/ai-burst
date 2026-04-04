package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagPmAssistRecord {
    private Long id;
    private Long projectId;
    private String problemType;
    private String rootCauseSummary;
    private String actionTaken;
    private String assistedAgentIdsJson;
    private Integer resolved;
    private LocalDateTime createdAt;
}
