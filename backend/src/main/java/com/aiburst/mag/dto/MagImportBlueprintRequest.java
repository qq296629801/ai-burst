package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MagImportBlueprintRequest {

    /** ARCHIVE | KB */
    @NotBlank
    private String sourceType;

    @NotNull
    private Long sourceId;
}
