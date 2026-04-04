package com.aiburst.mag.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagProjectUpdateRequest {

    @Size(max = 128)
    private String name;

    /** 1 active, 0 archived */
    private Integer status;
}
