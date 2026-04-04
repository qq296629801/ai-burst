package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagExternalFetchAudit {
    private Long id;
    private Long projectId;
    private Long userId;
    private String normalizedUrl;
    private Integer httpStatus;
    private String bodyHash;
    private LocalDateTime createdAt;
}
