package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagProject {
    private Long id;
    private String name;
    private Integer status;
    private String configJson;
    private Long currentReqDocId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
