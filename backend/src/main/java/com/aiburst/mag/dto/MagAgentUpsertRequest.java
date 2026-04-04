package com.aiburst.mag.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagAgentUpsertRequest {

    private Long parentAgentId;

    @Size(max = 32)
    private String roleType;

    @Size(max = 128)
    private String name;

    private Long llmChannelId;

    /**
     * 为 true 时按 {@link #llmChannelId} 更新绑定（含 null 表示解绑）；未传或 false 则本次不改通道。
     */
    private Boolean applyLlmChannelId;

    @Size(max = 64)
    private String systemPromptProfile;

    private String extraJson;

    private Integer status;
}
