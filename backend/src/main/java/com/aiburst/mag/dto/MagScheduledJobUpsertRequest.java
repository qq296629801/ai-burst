package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagScheduledJobUpsertRequest {

    private Long id;

    @NotBlank
    private String jobKey;

    @NotBlank
    private String cronExpr;

    private Integer enabled;

    private Long projectId;
}
