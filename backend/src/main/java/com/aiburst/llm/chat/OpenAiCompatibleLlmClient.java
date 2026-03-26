package com.aiburst.llm.chat;

import com.aiburst.llm.catalog.LlmProviderCatalog;
import com.aiburst.llm.dto.LlmChatMessage;
import com.aiburst.llm.dto.LlmChatRequest;
import com.aiburst.llm.dto.LlmChatResponse;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.exception.LlmInvocationException;
import com.aiburst.llm.util.OpenAiStyleUpstreamErrorParser;
import com.aiburst.llm.util.UrlJoinUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class OpenAiCompatibleLlmClient {

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProviderCatalog catalog;

    public OpenAiCompatibleLlmClient(
            @Qualifier("llmRestTemplate") RestTemplate llmRestTemplate,
            ObjectMapper objectMapper,
            LlmProviderCatalog catalog) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
        this.catalog = catalog;
    }

    public LlmChatResponse chat(LlmChannel channel, String apiKey, LlmChatRequest req) {
        String model = firstNonBlank(req.getModel(), channel.getDefaultModel());
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("model is required (set default model on channel or pass in request)");
        }
        String path = catalog.resolveCompletionPath(channel.getProviderCode());
        String url = UrlJoinUtil.join(channel.getBaseUrl(), path);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmChatMessage m : req.getMessages()) {
            Map<String, Object> row = new HashMap<>();
            row.put("role", m.getRole());
            row.put("content", m.getContent());
            messages.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        if (req.getTemperature() != null) {
            body.put("temperature", req.getTemperature());
        } else {
            body.put("temperature", 0.7);
        }
        if (req.getMaxTokens() != null) {
            body.put("max_tokens", req.getMaxTokens());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> resp = llmRestTemplate.postForEntity(url, entity, String.class);
            return parseOpenAi(resp.getBody(), model, channel.getProviderCode());
        } catch (HttpStatusCodeException e) {
            String human = OpenAiStyleUpstreamErrorParser.toHumanMessage(e.getResponseBodyAsString(), objectMapper);
            throw new LlmInvocationException(HttpStatus.BAD_GATEWAY.value(), human);
        } catch (Exception e) {
            throw new LlmInvocationException("upstream request failed: " + e.getMessage(), e);
        }
    }

    private LlmChatResponse parseOpenAi(String json, String model, String provider) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.path("choices");
            String content = "";
            if (choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message");
                content = msg.path("content").asText("");
            }
            return LlmChatResponse.builder()
                    .content(content)
                    .model(root.path("model").asText(model))
                    .providerCode(provider)
                    .protocol("OPENAI_COMPAT")
                    .usage(root.path("usage").isMissingNode() ? null : objectMapper.convertValue(root.get("usage"), Object.class))
                    .build();
        } catch (Exception e) {
            throw new LlmInvocationException("parse openai response failed: " + e.getMessage(), e);
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

}
