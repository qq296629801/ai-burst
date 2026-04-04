package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagPmAssistCreateRequest {

    private String problemType;

    @NotBlank
    private String rootCauseSummary;

    private String actionTaken;

    /** JSON 数组字符串，如 [1,2] */
    private String assistedAgentIdsJson;

    private Integer resolved;
}
