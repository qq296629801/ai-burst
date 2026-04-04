package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagMessagePostRequest {

    /** USER, AGENT, SYSTEM */
    @NotBlank
    private String senderType;

    private Long senderAgentId;

    @NotBlank
    private String content;
}
