package com.aiburst.llm.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.llm.dto.LlmChatRequest;
import com.aiburst.llm.dto.LlmChatResponse;
import com.aiburst.llm.service.LlmChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmChatController {

    private final LlmChatService llmChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('llm:chat:invoke')")
    public ApiResult<LlmChatResponse> chat(@Valid @RequestBody LlmChatRequest req) {
        return ApiResult.ok(llmChatService.chat(req));
    }
}
