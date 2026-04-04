package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagModuleUpsertRequest {

    private Long parentId;

    @NotBlank
    @Size(max = 256)
    private String name;

    @Size(max = 64)
    private String tag;

    private Integer sortOrder;
}
