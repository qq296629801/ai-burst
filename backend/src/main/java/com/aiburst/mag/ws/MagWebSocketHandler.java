package com.aiburst.mag.ws;

import com.aiburst.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MagWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final MagWebSocketHub hub;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("missing token"));
            return;
        }
        try {
            jwtService.parse(token);
        } catch (Exception e) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid token"));
            return;
        }
        session.getAttributes().put("channels", ConcurrentHashMap.<String>newKeySet());
        session.sendMessage(new TextMessage("{\"event\":\"connected\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String op = root.path("op").asText("");
        @SuppressWarnings("unchecked")
        Set<String> chans = (Set<String>) session.getAttributes().get("channels");
        if (chans == null) {
            return;
        }
        switch (op) {
            case "SUBSCRIBE" -> {
                String channel = root.path("channel").asText("");
                if (!channel.isBlank()) {
                    hub.subscribe(channel, session);
                    chans.add(channel);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("event", "subscribed", "channel", channel))));
                }
            }
            case "UNSUBSCRIBE" -> {
                String channel = root.path("channel").asText("");
                if (!channel.isBlank()) {
                    hub.unsubscribe(channel, session);
                    chans.remove(channel);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("event", "unsubscribed", "channel", channel))));
                }
            }
            case "PING" -> session.sendMessage(new TextMessage("{\"event\":\"PONG\"}"));
            default -> session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("event", "error", "message", "unknown op: " + op))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        @SuppressWarnings("unchecked")
        Set<String> chans = (Set<String>) session.getAttributes().get("channels");
        if (chans != null) {
            for (String c : chans) {
                hub.unsubscribe(c, session);
            }
            chans.clear();
        }
    }

    private static String extractToken(WebSocketSession session) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        String q = session.getUri().getQuery();
        for (String p : q.split("&")) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = URLDecoder.decode(p.substring(0, i), StandardCharsets.UTF_8);
                if ("token".equals(k) || "accessToken".equals(k)) {
                    return URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
