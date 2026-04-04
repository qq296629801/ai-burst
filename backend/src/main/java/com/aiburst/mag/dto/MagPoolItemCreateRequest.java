package com.aiburst.mag.dto;

import lombok.Data;

@Data
public class MagPoolItemCreateRequest {

    private Long revisionId;

    private String anchorJson;

    private String payloadJson;

    private Long assignedDeciderUserId;
}
