package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.config.MagTemporalTriggerService;
import com.aiburst.mag.dto.MagAgentUpsertRequest;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagAgentService {

    private final MagAgentMapper agentMapper;
    private final MagAccessHelper accessHelper;
    private final MagTemporalTriggerService temporalTriggerService;
    private final MagOrchestrationRunService orchestrationRunService;

    public List<Map<String, Object>> listByProject(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return agentMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long projectId, MagAgentUpsertRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        if (!StringUtils.hasText(req.getRoleType()) || !StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("roleType and name required");
        }
        MagAgent a = new MagAgent();
        a.setProjectId(projectId);
        a.setParentAgentId(req.getParentAgentId());
        a.setRoleType(req.getRoleType());
        a.setName(req.getName().trim());
        a.setLlmChannelId(req.getLlmChannelId());
        a.setSystemPromptProfile(req.getSystemPromptProfile());
        a.setExtraJson(req.getExtraJson());
        a.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        agentMapper.insert(a);
        return toRow(agentMapper.selectById(a.getId()));
    }

    @Transactional
    public Map<String, Object> update(Long agentId, MagAgentUpsertRequest req, Long userId) {
        MagAgent existing = agentMapper.selectById(agentId);
        if (existing == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(existing.getProjectId(), userId);
        if (req.getParentAgentId() != null) {
            existing.setParentAgentId(req.getParentAgentId());
        }
        if (StringUtils.hasText(req.getRoleType())) {
            existing.setRoleType(req.getRoleType());
        }
        if (StringUtils.hasText(req.getName())) {
            existing.setName(req.getName().trim());
        }
        if (Boolean.TRUE.equals(req.getApplyLlmChannelId())) {
            existing.setLlmChannelId(req.getLlmChannelId());
        } else if (req.getLlmChannelId() != null) {
            existing.setLlmChannelId(req.getLlmChannelId());
        }
        if (req.getSystemPromptProfile() != null) {
            existing.setSystemPromptProfile(req.getSystemPromptProfile());
        }
        if (req.getExtraJson() != null) {
            existing.setExtraJson(req.getExtraJson());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        agentMapper.update(existing);
        return toRow(agentMapper.selectById(agentId));
    }

    @Transactional
    public Map<String, Object> requestAgentRun(Long agentId, Long userId, String instruction) {
        return requestAgentRun(agentId, userId, instruction, null);
    }

    /**
     * @param taskContextTaskId 派工自动执行等场景写入编排记录，便于失败告警关联任务；可为 null
     */
    @Transactional
    public Map<String, Object> requestAgentRun(
            Long agentId, Long userId, String instruction, Long taskContextTaskId) {
        MagAgent a = agentMapper.selectById(agentId);
        if (a == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(a.getProjectId(), userId);
        if (a.getLlmChannelId() == null) {
            throw new MagBusinessException(
                    MagResultCode.MAG_AGENT_LLM_CHANNEL_REQUIRED,
                    "Agent 未绑定大模型通道（llmChannelId），无法执行编排");
        }
        String instr = instruction != null ? instruction.trim() : "";
        Map<String, Object> res =
                temporalTriggerService.triggerAgentRun(agentId, userId, "agentId=" + agentId, instr);
        orchestrationRunService.recordAgentTrigger(a.getProjectId(), agentId, userId, res, taskContextTaskId);
        return res;
    }

    private Map<String, Object> toRow(MagAgent a) {
        Map<String, Object> m = new HashMap<>();
        if (a == null) {
            return m;
        }
        m.put("id", a.getId());
        m.put("projectId", a.getProjectId());
        m.put("parentAgentId", a.getParentAgentId());
        m.put("roleType", a.getRoleType());
        m.put("name", a.getName());
        m.put("llmChannelId", a.getLlmChannelId());
        m.put("systemPromptProfile", a.getSystemPromptProfile());
        m.put("extraJson", a.getExtraJson());
        m.put("status", a.getStatus());
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        return m;
    }
}
