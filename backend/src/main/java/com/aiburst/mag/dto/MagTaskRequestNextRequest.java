package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MagTaskRequestNextRequest {

    /** 发起要活的 Agent 实例 */
    @NotNull
    private Long agentId;
}
