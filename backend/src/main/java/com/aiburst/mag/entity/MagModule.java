package com.aiburst.mag.entity;

import lombok.Data;

@Data
public class MagModule {
    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String tag;
    private Integer sortOrder;
}
