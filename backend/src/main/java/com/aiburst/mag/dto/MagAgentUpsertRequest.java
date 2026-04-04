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

    @Size(max = 64)
    private String systemPromptProfile;

    private String extraJson;

    private Integer status;
}
