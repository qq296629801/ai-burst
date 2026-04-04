package com.aiburst.mag.service;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagSubmitCompleteRequest;
import com.aiburst.mag.dto.MagTaskBlockRequest;
import com.aiburst.mag.dto.MagTaskCreateRequest;
import com.aiburst.mag.dto.MagTaskRequestNextRequest;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.entity.MagTaskVerification;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.mapper.MagTaskVerificationMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagTaskService {

    private final MagTaskMapper taskMapper;
    private final MagTaskVerificationMapper verificationMapper;
    private final MagAccessHelper accessHelper;
    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> listByProject(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return taskMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long projectId, MagTaskCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagTask t = new MagTask();
        t.setProjectId(projectId);
        t.setModuleId(req.getModuleId());
        t.setTitle(req.getTitle().trim());
        t.setDescription(req.getDescription());
        t.setState(MagConstants.TASK_PENDING);
        t.setAssigneeAgentId(req.getAssigneeAgentId());
        t.setReporterAgentId(req.getReporterAgentId());
        t.setRequirementRef(req.getRequirementRef());
        t.setRowVersion(0);
        taskMapper.insert(t);
        return toRow(taskMapper.selectById(t.getId()));
    }

    @Transactional
    public void start(Long taskId, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        if (!MagConstants.TASK_PENDING.equals(task.getState())) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID);
        }
        task.setState(MagConstants.TASK_IN_PROGRESS);
        taskMapper.update(task);
    }

    @Transactional
    public void submitComplete(Long taskId, MagSubmitCompleteRequest req, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        if (!MagConstants.TASK_IN_PROGRESS.equals(task.getState())) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID);
        }
        int expected = req.getRowVersion() != null ? req.getRowVersion() : task.getRowVersion();
        String wfId = "stub-" + taskId + "-" + UUID.randomUUID();
        int updated = taskMapper.updateStateWithVersion(taskId, MagConstants.TASK_PENDING_VERIFY, expected, wfId);
        if (updated == 0) {
            throw new MagBusinessException(MagResultCode.MAG_ROW_VERSION_CONFLICT);
        }
    }

    @Transactional
    public void block(Long taskId, MagTaskBlockRequest req, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        if (MagConstants.TASK_DONE.equals(task.getState())) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID);
        }
        task.setState(MagConstants.TASK_BLOCKED);
        task.setBlockReason(req.getReason());
        task.setBlockedByAgentId(req.getBlockedByAgentId());
        taskMapper.update(task);
        MagThread coord = ensureCoordThread(task.getProjectId());
        MagMessage m = new MagMessage();
        m.setThreadId(coord.getId());
        m.setSenderType(req.getBlockedByAgentId() != null ? "AGENT" : "USER");
        m.setSenderAgentId(req.getBlockedByAgentId());
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kind", "BLOCK");
            payload.put("taskId", taskId);
            payload.put("reason", req.getReason());
            m.setContent(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            m.setContent("{\"kind\":\"BLOCK\",\"taskId\":" + taskId + "}");
        }
        messageMapper.insert(m);
    }

    @Transactional
    public void requestNext(Long taskId, MagTaskRequestNextRequest req, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        MagThread coord = ensureCoordThread(task.getProjectId());
        MagMessage m = new MagMessage();
        m.setThreadId(coord.getId());
        m.setSenderType("AGENT");
        m.setSenderAgentId(req.getAgentId());
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kind", "REQUEST_NEXT");
            payload.put("taskId", taskId);
            payload.put("agentId", req.getAgentId());
            m.setContent(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            m.setContent("{\"kind\":\"REQUEST_NEXT\",\"taskId\":" + taskId + "}");
        }
        messageMapper.insert(m);
    }

    private MagThread ensureCoordThread(Long projectId) {
        List<MagThread> threads = threadMapper.selectByProjectId(projectId);
        for (MagThread t : threads) {
            if ("需求与派工协调".equals(t.getTitle())) {
                return t;
            }
        }
        MagThread t = new MagThread();
        t.setProjectId(projectId);
        t.setTitle("需求与派工协调");
        threadMapper.insert(t);
        return threadMapper.selectById(t.getId());
    }

    public List<Map<String, Object>> listVerifications(Long taskId, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        return verificationMapper.selectByTaskId(taskId).stream().map(this::verRow).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(MagTask t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("projectId", t.getProjectId());
        m.put("moduleId", t.getModuleId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("state", t.getState());
        m.put("assigneeAgentId", t.getAssigneeAgentId());
        m.put("reporterAgentId", t.getReporterAgentId());
        m.put("requirementRef", t.getRequirementRef());
        m.put("temporalWorkflowId", t.getTemporalWorkflowId());
        m.put("blockReason", t.getBlockReason());
        m.put("blockedByAgentId", t.getBlockedByAgentId());
        m.put("rowVersion", t.getRowVersion());
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }

    private Map<String, Object> verRow(MagTaskVerification v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("taskId", v.getTaskId());
        m.put("result", v.getResult());
        m.put("verifierAgentId", v.getVerifierAgentId());
        m.put("rationale", v.getRationale());
        m.put("evidenceSummary", v.getEvidenceSummary());
        m.put("searchTraceJson", v.getSearchTraceJson());
        m.put("createdAt", v.getCreatedAt());
        return m;
    }
}
