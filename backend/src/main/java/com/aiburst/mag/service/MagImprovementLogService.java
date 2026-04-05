package com.aiburst.mag.service;

import com.aiburst.mag.dto.MagImprovementCreateRequest;
import com.aiburst.mag.entity.MagAgentImprovementLog;
import com.aiburst.mag.mapper.MagAgentImprovementLogMapper;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagImprovementLogService {

    private final MagAgentImprovementLogMapper improvementLogMapper;
    private final MagAgentMapper agentMapper;
    private final MagAccessHelper accessHelper;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> list(Long projectId, Long agentId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        requireAgentInProject(agentId, projectId);
        return improvementLogMapper.selectByProjectAndAgent(projectId, agentId).stream()
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> append(Long projectId, Long agentId, MagImprovementCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        requireAgentInProject(agentId, projectId);
        MagAgentImprovementLog log = new MagAgentImprovementLog();
        log.setProjectId(projectId);
        log.setAgentId(agentId);
        log.setChangeType(req.getChangeType());
        log.setSummary(req.getSummary());
        log.setDetailJson(normalizeDetailJsonForMysql(req.getDetailJson()));
        log.setCreatedByUserId(userId);
        improvementLogMapper.insert(log);
        return toRow(log);
    }

    private void requireAgentInProject(Long agentId, Long projectId) {
        var ag = agentMapper.selectById(agentId);
        if (ag == null || !Objects.equals(ag.getProjectId(), projectId)) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
    }

    /**
     * {@code mag_agent_improvement_log.detail_json} 为 MySQL JSON 列，须为合法 JSON。
     * 工具/API 常传入 Markdown 正文，此处包一层 {@code {"markdown":"..."}}；已是 JSON 的字符串则规范化后原样存储。
     */
    private String normalizeDetailJsonForMysql(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "{}";
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ignored) {
            try {
                Map<String, String> wrapper = new LinkedHashMap<>();
                wrapper.put("markdown", raw);
                return objectMapper.writeValueAsString(wrapper);
            } catch (JsonProcessingException e) {
                return "{\"markdown\":\"\"}";
            }
        }
    }

    private Map<String, Object> toRow(MagAgentImprovementLog log) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", log.getId());
        m.put("projectId", log.getProjectId());
        m.put("agentId", log.getAgentId());
        m.put("changeType", log.getChangeType());
        m.put("summary", log.getSummary());
        m.put("detailJson", log.getDetailJson());
        m.put("createdByUserId", log.getCreatedByUserId());
        m.put("createdAt", log.getCreatedAt());
        return m;
    }
}
