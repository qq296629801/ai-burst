package com.aiburst.rbac.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {
    private Long id;
    private Long parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
