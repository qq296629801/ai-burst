package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MagTaskPmReassignRequest {

    @NotNull
    private Long assigneeAgentId;
}
