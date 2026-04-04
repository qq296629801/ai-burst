package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagRequirementChangeAnalyzeRequest {

    @NotBlank
    private String changeSummary;

    private String notes;
}
