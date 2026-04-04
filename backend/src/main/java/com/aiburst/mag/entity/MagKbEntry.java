package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagKbEntry {
    private Long id;
    private String source;
    private Long archiveId;
    private String title;
    private String body;
    private String tagsJson;
    private String keywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
