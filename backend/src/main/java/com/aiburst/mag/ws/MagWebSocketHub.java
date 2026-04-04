package com.aiburst.mag.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 按 channel（如 project:1）管理订阅；业务侧可调用 {@link #broadcast} 推送 §8 事件。
 */
@Component
public class MagWebSocketHub {

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();

    public void subscribe(String channel, WebSocketSession session) {
        channelSessions.computeIfAbsent(channel, c -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unsubscribe(String channel, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> set = channelSessions.get(channel);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                channelSessions.remove(channel);
            }
        }
    }

    public void broadcast(String channel, String json) {
        CopyOnWriteArraySet<WebSocketSession> set = channelSessions.get(channel);
        if (set == null) {
            return;
        }
        TextMessage tm = new TextMessage(json);
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(tm);
                }
            } catch (Exception ignored) {
                // 单连接失败不影响其他订阅者
            }
        }
    }
}
