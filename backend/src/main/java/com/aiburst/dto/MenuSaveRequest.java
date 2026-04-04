package com.aiburst.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class MenuSaveRequest {
    private Long id;
    @NotNull
    private Long parentId;
    @NotBlank
    private String permCode;
    @NotBlank
    private String permName;
    @NotNull
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder = 0;
    private Integer status = 1;
}
