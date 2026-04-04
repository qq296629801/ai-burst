package com.aiburst.mag.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MagMessage {
    private Long id;
    private Long threadId;
    private String senderType;
    private Long senderAgentId;
    private String content;
    private LocalDateTime createdAt;
}
