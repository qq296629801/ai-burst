package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagThread {
    private Long id;
    private Long projectId;
    private Long taskId;
    private String title;
    private LocalDateTime createdAt;
}
