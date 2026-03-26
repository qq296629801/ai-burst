package com.aiburst.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LlmChannel {
    private Long id;
    private Long ownerUserId;
    private String providerCode;
    private String protocol;
    private String channelName;
    private String baseUrl;
    private String apiKeyCipher;
    private String defaultModel;
    private String extraJson;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
