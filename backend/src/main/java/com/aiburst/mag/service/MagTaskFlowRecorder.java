package com.aiburst.mag.service;

import com.aiburst.mag.entity.MagTaskFlowEvent;
import com.aiburst.mag.mapper.MagTaskFlowEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 任务流程事件落库，供工作台时间线与流程图可视化。
 */
@Component
@RequiredArgsConstructor
public class MagTaskFlowRecorder {

    private final MagTaskFlowEventMapper flowEventMapper;
    private final ObjectMapper objectMapper;

    public void record(
            long projectId,
            long taskId,
            String eventType,
            String actorType,
            Long actorAgentId,
            String summary,
            Map<String, Object> detail) {
        MagTaskFlowEvent e = new MagTaskFlowEvent();
        e.setProjectId(projectId);
        e.setTaskId(taskId);
        e.setEventType(eventType);
        e.setActorType(actorType);
        e.setActorAgentId(actorAgentId);
        e.setSummary(summary);
        if (detail != null && !detail.isEmpty()) {
            try {
                e.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (JsonProcessingException ex) {
                e.setDetailJson("{\"error\":\"detail_json_failed\"}");
            }
        }
        flowEventMapper.insert(e);
    }
}
