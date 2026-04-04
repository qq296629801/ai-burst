package com.aiburst.llm.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class LlmChatMessage {
    @NotBlank
    private String role;
    @NotBlank
    private String content;
}
