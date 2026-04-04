package com.aiburst.llm.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class LlmChatRequest {
    @NotNull
    private Long channelId;
    @NotEmpty
    @Valid
    private List<LlmChatMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}
