package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagTaskDispatchRequest {

    private Long moduleId;

    @NotBlank
    @Size(max = 256)
    private String title;

    private String description;

    @NotNull
    private Long assigneeAgentId;

    private Long reporterAgentId;

    private String requirementRef;
}
