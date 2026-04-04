package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagRequirementRevision {
    private Long id;
    private Long docId;
    private Integer version;
    private String content;
    private Long authorUserId;
    private LocalDateTime createdAt;
}
