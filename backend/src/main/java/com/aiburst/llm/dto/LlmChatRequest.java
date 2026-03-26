package com.aiburst.llm.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
