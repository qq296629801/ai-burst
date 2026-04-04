package com.aiburst.mag.service;

import com.aiburst.mag.dto.MagImprovementCreateRequest;
import com.aiburst.mag.entity.MagAgentImprovementLog;
import com.aiburst.mag.mapper.MagAgentImprovementLogMapper;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
        log.setDetailJson(req.getDetailJson());
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
