package com.aiburst.mag.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagThreadCreateRequest {

    private Long taskId;

    @Size(max = 256)
    private String title;
}
