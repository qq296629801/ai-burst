package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagRequirementDoc {
    private Long id;
    private Long projectId;
    private Integer currentVersion;
    private LocalDateTime updatedAt;
}
