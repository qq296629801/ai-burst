package com.aiburst.llm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 将 OpenAI 兼容及部分云厂商返回的错误 JSON 转为可读文案（提取 error.message 等）。
 */
public final class OpenAiStyleUpstreamErrorParser {

    private static final int MAX_FALLBACK_LEN = 800;

    private OpenAiStyleUpstreamErrorParser() {
    }

    public static String toHumanMessage(String responseBody, ObjectMapper objectMapper) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "upstream request failed";
        }
        String trimmed = responseBody.trim();
        if (!trimmed.startsWith("{")) {
            return shortenPlain(trimmed);
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            JsonNode err = root.get("error");
            if (err != null && err.isObject()) {
                String m = textNode(err.get("message"));
                if (m != null) {
                    return m;
                }
                m = textNode(err.get("type"));
                if (m != null) {
                    return m;
                }
                m = textNode(err.get("code"));
                if (m != null) {
                    return m;
                }
            }
            if (err != null && err.isTextual()) {
                return err.asText();
            }
            String top = textNode(root.get("message"));
            if (top != null) {
                return top;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return shortenPlain(trimmed);
    }

    private static String textNode(JsonNode n) {
        if (n == null || n.isNull() || !n.isTextual()) {
            return null;
        }
        String t = n.asText();
        return t != null && !t.isEmpty() ? t : null;
    }

    private static String shortenPlain(String s) {
        if (s.length() <= MAX_FALLBACK_LEN) {
            return s;
        }
        return s.substring(0, MAX_FALLBACK_LEN) + "...";
    }
}
