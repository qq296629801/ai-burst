package com.aiburst.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class RoleSaveRequest {
    private Long id;
    @NotBlank
    private String roleCode;
    @NotBlank
    private String roleName;
    private String remark;
    private List<Long> permissionIds;
}
