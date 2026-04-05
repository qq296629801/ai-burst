package com.aiburst.mag.dto;

import lombok.Data;

/**
 * 触发 Agent 编排时的可选入参（如 PM 派工自然语言说明）。
 */
@Data
public class MagAgentRunRequest {

    /** 传给模型的用户补充说明；PM 派工场景下描述要派给谁、做什么 */
    private String instruction;
}
