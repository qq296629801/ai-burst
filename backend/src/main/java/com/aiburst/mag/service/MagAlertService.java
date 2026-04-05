package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagAlertEvent;
import com.aiburst.mag.mapper.MagAlertEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagAlertService {

    private final MagAlertEventMapper alertEventMapper;
    private final MagAccessHelper accessHelper;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> listByProject(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return alertEventMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    /**
     * 写入告警事件（无需当前用户上下文），供大屏/告警列表消费。
     */
    @Transactional
    public void raise(Long projectId, Long taskId, String alertType, String level, Map<String, Object> payload) {
        MagAlertEvent e = new MagAlertEvent();
        e.setProjectId(projectId);
        e.setTaskId(taskId);
        e.setAlertType(alertType);
        e.setLevel(level != null ? level : "WARN");
        try {
            e.setPayloadJson(
                    payload == null || payload.isEmpty()
                            ? "{}"
                            : objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            e.setPayloadJson("{\"error\":\"payload_json\"}");
        }
        e.setAcknowledged(0);
        alertEventMapper.insert(e);
    }

    @Transactional
    public void acknowledge(Long alertId, Long userId) {
        MagAlertEvent e = alertEventMapper.selectById(alertId);
        if (e == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        if (e.getProjectId() != null) {
            accessHelper.requireMember(e.getProjectId(), userId);
        }
        alertEventMapper.updateAcknowledged(alertId, 1);
    }

    private Map<String, Object> toRow(MagAlertEvent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("projectId", e.getProjectId());
        m.put("taskId", e.getTaskId());
        m.put("alertType", e.getAlertType());
        m.put("level", e.getLevel());
        m.put("payloadJson", e.getPayloadJson());
        m.put("acknowledged", e.getAcknowledged());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
