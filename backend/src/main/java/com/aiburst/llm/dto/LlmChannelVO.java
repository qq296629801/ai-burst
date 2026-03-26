package com.aiburst.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmChannelVO {
    private Long id;
    private String providerCode;
    private String providerName;
    private String protocol;
    private String channelName;
    private String baseUrl;
    private String defaultModel;
    private String extraJson;
    private Integer status;
}
