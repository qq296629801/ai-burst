package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MagPoolDecideRequest {

    /** APPROVE_AS_IS, APPROVE_WITH_CHANGE, REJECT, DEFER */
    @NotBlank
    private String decision;

    private String note;
}
