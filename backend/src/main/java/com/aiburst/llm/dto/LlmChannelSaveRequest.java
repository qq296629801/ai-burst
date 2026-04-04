package com.aiburst.llm.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class LlmChannelSaveRequest {
    private Long id;
    @NotBlank
    private String providerCode;
    @NotBlank
    private String channelName;
    @NotBlank
    private String baseUrl;
    /** 新建必填；编辑时留空表示不修改 */
    private String apiKey;
    private String defaultModel;
    private String extraJson;
    @NotNull
    private Integer status = 1;
}
