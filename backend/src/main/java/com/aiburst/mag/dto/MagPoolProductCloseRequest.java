package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagPoolProductCloseRequest {

    @NotBlank
    private String conclusionSummary;

    private String payloadExtensionJson;
}
