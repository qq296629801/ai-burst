package com.aiburst.mag.service;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.entity.MagOrchestrationRun;
import com.aiburst.mag.mapper.MagOrchestrationRunMapper;
import com.aiburst.mag.ws.MagWebSocketHub;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记录 Agent/线程「触发 run」后的编排执行情况，供列表查询与 WS 推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagOrchestrationRunService {

    private static final int MSG_MAX = 512;
    private static final int SUMMARY_MAX = 1024;

    private final MagOrchestrationRunMapper runMapper;
    private final MagAccessHelper accessHelper;
    private final MagWebSocketHub webSocketHub;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordAgentTrigger(long projectId, long agentId, long userId, Map<String, Object> triggerResult) {
        insertFromTrigger(
                projectId,
                MagConstants.ORCH_RUN_KIND_AGENT,
                agentId,
                null,
                userId,
                triggerResult);
    }

    @Transactional
    public void recordThreadTrigger(long projectId, long threadId, long userId, Map<String, Object> triggerResult) {
        insertFromTrigger(
                projectId,
                MagConstants.ORCH_RUN_KIND_THREAD,
                null,
                threadId,
                userId,
                triggerResult);
    }

    private void insertFromTrigger(
            long projectId,
            String runKind,
            Long agentId,
            Long threadId,
            long userId,
            Map<String, Object> triggerResult) {
        boolean accepted = Boolean.TRUE.equals(triggerResult.get("accepted"));
        String workflowId = triggerResult.get("workflowId") != null
                ? String.valueOf(triggerResult.get("workflowId"))
                : null;
        String msg = trim(stringOrEmpty(triggerResult.get("message")), MSG_MAX);
        LocalDateTime now = LocalDateTime.now();
        MagOrchestrationRun row = new MagOrchestrationRun();
        row.setProjectId(projectId);
        row.setRunKind(runKind);
        row.setAgentId(agentId);
        row.setThreadId(threadId);
        row.setWorkflowId(workflowId);
        row.setStatus(accepted ? MagConstants.ORCH_STATUS_SUBMITTED : MagConstants.ORCH_STATUS_REJECTED);
        row.setMessage(msg);
        row.setResultSummary(null);
        row.setTriggerUserId(userId);
        row.setStartedAt(now);
        row.setFinishedAt(accepted ? null : now);
        runMapper.insert(row);
        broadcast(projectId, row.getId());
    }

    @Transactional
    public void markActivityStarted(String workflowId) {
        if (workflowId == null || workflowId.isEmpty()) {
            return;
        }
        int n = runMapper.updateRunning(workflowId);
        if (n == 0) {
            log.debug("mag orchestration run: no row to mark RUNNING for workflowId={}", workflowId);
            return;
        }
        MagOrchestrationRun row = runMapper.selectByWorkflowId(workflowId);
        if (row != null) {
            broadcast(row.getProjectId(), row.getId());
        }
    }

    @Transactional
    public void markActivitySucceeded(String workflowId, String resultSummary) {
        finish(workflowId, MagConstants.ORCH_STATUS_SUCCEEDED, trim(resultSummary, SUMMARY_MAX));
    }

    @Transactional
    public void markActivityFailed(String workflowId, String errorDetail) {
        finish(workflowId, MagConstants.ORCH_STATUS_FAILED, trim(errorDetail, SUMMARY_MAX));
    }

    private void finish(String workflowId, String status, String resultSummary) {
        if (workflowId == null || workflowId.isEmpty()) {
            return;
        }
        runMapper.updateFinished(workflowId, status, resultSummary, LocalDateTime.now());
        MagOrchestrationRun row = runMapper.selectByWorkflowId(workflowId);
        if (row != null) {
            broadcast(row.getProjectId(), row.getId());
        }
    }

    public List<Map<String, Object>> listByProject(long projectId, long userId, int limit) {
        accessHelper.requireMember(projectId, userId);
        int cap = Math.min(Math.max(limit, 1), 100);
        return runMapper.selectByProjectId(projectId, cap).stream()
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toRow(MagOrchestrationRun r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("projectId", r.getProjectId());
        m.put("runKind", r.getRunKind());
        m.put("agentId", r.getAgentId());
        m.put("threadId", r.getThreadId());
        m.put("workflowId", r.getWorkflowId());
        m.put("status", r.getStatus());
        m.put("message", r.getMessage());
        m.put("resultSummary", r.getResultSummary());
        m.put("triggerUserId", r.getTriggerUserId());
        m.put("startedAt", r.getStartedAt());
        m.put("finishedAt", r.getFinishedAt());
        return m;
    }

    private void broadcast(long projectId, Long runId) {
        if (runId == null) {
            return;
        }
        MagOrchestrationRun row = runMapper.selectById(runId);
        if (row == null) {
            return;
        }
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("event", "mag.orchestration.run.updated");
            envelope.put("projectId", projectId);
            envelope.put("run", toRow(row));
            webSocketHub.broadcast("project:" + projectId, objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            log.warn("mag orchestration run broadcast json failed", e);
        }
    }

    private static String stringOrEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
