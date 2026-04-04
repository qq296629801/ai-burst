package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagScheduledJobConfig {
    private Long id;
    private String jobKey;
    private String cronExpr;
    private Integer enabled;
    private Long projectId;
    private LocalDateTime lastRunAt;
}
