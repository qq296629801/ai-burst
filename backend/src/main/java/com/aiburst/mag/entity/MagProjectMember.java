package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagProjectMember {
    private Long id;
    private Long projectId;
    private Long userId;
    private String roleInProject;
    private LocalDateTime createdAt;
    private String username;
    private String nickname;
}
