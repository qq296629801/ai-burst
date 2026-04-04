package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagReleaseCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String versionLabel;

    private String snapshotJson;

    @Size(max = 512)
    private String minioObjectKey;

    /** 1 = 优质候选，触发知识库回流（§16.1） */
    private Integer qualityFlag;
}
