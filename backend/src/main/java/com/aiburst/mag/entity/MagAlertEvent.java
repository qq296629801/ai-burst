package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagAlertEvent {
    private Long id;
    private Long projectId;
    private Long taskId;
    private String alertType;
    private String level;
    private String payloadJson;
    private Integer acknowledged;
    private LocalDateTime createdAt;
}
