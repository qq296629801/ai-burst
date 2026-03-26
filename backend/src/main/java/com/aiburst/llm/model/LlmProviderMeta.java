package com.aiburst.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmProviderMeta {
    private String code;
    private String name;
    private String defaultBaseUrl;
    /** 拼在 Base 后的路径，如 /v1/chat/completions 或 /chat/completions */
    private String completionPath;
    private LlmProtocol protocol;
    private String docHint;
}
