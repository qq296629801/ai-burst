package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagImprovementCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String changeType;

    @NotBlank
    @Size(max = 512)
    private String summary;

    private String detailJson;
}
