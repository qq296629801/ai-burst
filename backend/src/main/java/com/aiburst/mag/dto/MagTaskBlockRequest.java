package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagTaskBlockRequest {

    @NotBlank
    private String reason;

    private Long blockedByAgentId;
}
