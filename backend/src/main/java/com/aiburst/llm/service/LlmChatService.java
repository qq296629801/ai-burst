package com.aiburst.llm.service;

import com.aiburst.llm.chat.AnthropicLlmClient;
import com.aiburst.llm.chat.OpenAiCompatibleLlmClient;
import com.aiburst.llm.crypto.LlmCryptoService;
import com.aiburst.llm.dto.LlmChatRequest;
import com.aiburst.llm.dto.LlmChatResponse;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.model.LlmProtocol;
import com.aiburst.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final LlmChannelService llmChannelService;
    private final LlmCryptoService llmCryptoService;
    private final OpenAiCompatibleLlmClient openAiCompatibleLlmClient;
    private final AnthropicLlmClient anthropicLlmClient;

    public LlmChatResponse chat(LlmChatRequest req) {
        Long uid = SecurityUtils.currentUserId();
        LlmChannel ch = llmChannelService.requireOwned(req.getChannelId(), uid);
        String apiKey = llmCryptoService.decrypt(ch.getApiKeyCipher());
        LlmProtocol p = LlmProtocol.valueOf(ch.getProtocol());
        switch (p) {
            case OPENAI_COMPAT:
                return openAiCompatibleLlmClient.chat(ch, apiKey, req);
            case ANTHROPIC:
                return anthropicLlmClient.chat(ch, apiKey, req);
            default:
                throw new IllegalStateException("unsupported protocol: " + p);
        }
    }
}
