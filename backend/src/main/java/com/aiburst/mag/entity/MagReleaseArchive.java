package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagReleaseArchive {
    private Long id;
    private Long projectId;
    private String versionLabel;
    private String snapshotJson;
    private String minioObjectKey;
    private Integer qualityFlag;
    private LocalDateTime createdAt;
}
