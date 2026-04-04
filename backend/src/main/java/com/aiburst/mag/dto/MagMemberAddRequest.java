package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MagMemberAddRequest {

    @NotNull
    private Long userId;

    /** OWNER, MEMBER, VIEWER */
    @NotNull
    private String roleInProject;
}
