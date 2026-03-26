package com.aiburst.llm.chat;

import com.aiburst.llm.dto.LlmChatMessage;
import com.aiburst.llm.dto.LlmChatRequest;
import com.aiburst.llm.dto.LlmChatResponse;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.exception.LlmInvocationException;
import com.aiburst.llm.util.UrlJoinUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicLlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;

    public AnthropicLlmClient(
            @Qualifier("llmRestTemplate") RestTemplate llmRestTemplate,
            ObjectMapper objectMapper) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
    }

    public LlmChatResponse chat(LlmChannel channel, String apiKey, LlmChatRequest req) {
        String model = firstNonBlank(req.getModel(), channel.getDefaultModel());
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("model is required");
        }
        String url = UrlJoinUtil.join(channel.getBaseUrl(), "/v1/messages");

        StringBuilder system = new StringBuilder();
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (LlmChatMessage m : req.getMessages()) {
            if ("system".equalsIgnoreCase(m.getRole())) {
                if (system.length() > 0) {
                    system.append("\n");
                }
                system.append(m.getContent());
                continue;
            }
            String role = m.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                role = "user";
            }
            Map<String, Object> row = new HashMap<>();
            row.put("role", role);
            row.put("content", m.getContent());
            msgs.add(row);
        }
        if (msgs.isEmpty()) {
            throw new IllegalArgumentException("at least one user/assistant message required");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", req.getMaxTokens() != null ? req.getMaxTokens() : 1024);
        if (system.length() > 0) {
            body.put("system", system.toString());
        }
        body.put("messages", msgs);
        if (req.getTemperature() != null) {
            body.put("temperature", req.getTemperature());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> resp = llmRestTemplate.postForEntity(url, entity, String.class);
            return parseAnthropic(resp.getBody(), model, channel.getProviderCode());
        } catch (HttpStatusCodeException e) {
            throw new LlmInvocationException(e.getRawStatusCode(), shorten(e.getResponseBodyAsString()));
        } catch (Exception e) {
            throw new LlmInvocationException("anthropic request failed: " + e.getMessage(), e);
        }
    }

    private LlmChatResponse parseAnthropic(String json, String model, String provider) {
        try {
            JsonNode root = objectMapper.readTree(json);
            StringBuilder text = new StringBuilder();
            JsonNode content = root.path("content");
            if (content.isArray()) {
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText(""));
                    }
                }
            }
            return LlmChatResponse.builder()
                    .content(text.toString())
                    .model(root.path("model").asText(model))
                    .providerCode(provider)
                    .protocol("ANTHROPIC")
                    .usage(root.path("usage").isMissingNode() ? null : objectMapper.convertValue(root.get("usage"), Object.class))
                    .build();
        } catch (Exception e) {
            throw new LlmInvocationException("parse anthropic response failed: " + e.getMessage(), e);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }
}
