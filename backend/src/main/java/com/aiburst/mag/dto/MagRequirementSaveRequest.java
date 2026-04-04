package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagRequirementSaveRequest {

    @NotBlank
    private String content;
}
