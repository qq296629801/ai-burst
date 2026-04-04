package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagProjectCreateRequest {

    @NotBlank
    @Size(max = 128)
    private String name;
}
