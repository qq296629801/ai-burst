package com.aiburst.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UserSaveRequest {
    private Long id;
    @NotBlank
    private String username;
    private String password;
    private String nickname;
    private Integer status = 1;
    private List<Long> roleIds;
}
