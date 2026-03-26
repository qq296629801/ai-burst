package com.aiburst.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmChatResponse {
    private String content;
    private String model;
    private String providerCode;
    private String protocol;
    private Object usage;
}
