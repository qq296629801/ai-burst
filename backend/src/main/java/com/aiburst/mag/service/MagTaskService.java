package com.aiburst.mag.service;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.MagTaskFlowEventType;
import com.aiburst.mag.dto.MagSubmitCompleteRequest;
import com.aiburst.mag.dto.MagTaskBlockRequest;
import com.aiburst.mag.dto.MagTaskCreateRequest;
import com.aiburst.mag.dto.MagTaskDispatchRequest;
import com.aiburst.mag.dto.MagTaskPmReassignRequest;
import com.aiburst.mag.dto.MagTaskRequestNextRequest;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagModule;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagModuleMapper;
import com.aiburst.mag.entity.MagTaskFlowEvent;
import com.aiburst.mag.entity.MagTaskExecutionLog;
import com.aiburst.mag.mapper.MagTaskExecutionLogMapper;
import com.aiburst.mag.mapper.MagTaskFlowEventMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.event.MagTaskAutoOrchestrateEvent;
import com.aiburst.mag.event.MagTaskMarkedDoneEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MagTaskService {

    /** {@code mag_task.block_reason} 列为 VARCHAR(512) */
    private static final int BLOCK_REASON_MAX = 512;

    private final MagTaskMapper taskMapper;
    private final MagAccessHelper accessHelper;
    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final MagAgentMapper agentMapper;
    private final MagModuleMapper moduleMapper;
    private final ObjectMapper objectMapper;
    private final MagTaskFlowRecorder flowRecorder;
    private final MagTaskFlowEventMapper flowEventMapper;
    private final MagTaskExecutionLogMapper taskExecutionLogMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MagTaskAutomationProperties taskAutomationProperties;
    private final MagAlertService alertService;
    private final MagCoordinationChatWriter coordinationChatWriter;

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
        Long createdId = t.getId();
        Map<String, Object> createdDetail = new LinkedHashMap<>();
        createdDetail.put("title", t.getTitle());
        createdDetail.put("assigneeAgentId", t.getAssigneeAgentId());
        flowRecorder.record(
                projectId,
                createdId,
                MagTaskFlowEventType.TASK_CREATED,
                "USER",
                null,
                "创建任务（可未指派）",
                createdDetail);
        return toRow(taskMapper.selectById(createdId));
    }

    /**
     * 项目经理派工：创建任务并必须指定执行 Agent（产品 §4.2）。
     */
    @Transactional
    public Map<String, Object> dispatch(Long projectId, MagTaskDispatchRequest req, Long userId) {
        return dispatch(projectId, req, userId, null);
    }

    /**
     * @param dispatchingPmAgentId 非空表示由 PM AgentScope 工具派工，流程上记为 AGENT 行为体
     */
    @Transactional
    public Map<String, Object> dispatch(Long projectId, MagTaskDispatchRequest req, Long userId, Long dispatchingPmAgentId) {
        accessHelper.requireMember(projectId, userId);
        requireModuleInProject(projectId, req.getModuleId());
        requireAssignableAgent(projectId, req.getAssigneeAgentId());
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
        Long taskId = t.getId();
        if (dispatchingPmAgentId != null && threadMapper.selectLatestByTaskId(taskId) == null) {
            MagThread chat = new MagThread();
            chat.setProjectId(projectId);
            chat.setTaskId(taskId);
            chat.setTitle(taskCommunicationThreadTitle(req.getTitle().trim()));
            threadMapper.insert(chat);
        }
        postCoordAssignMessage(projectId, taskId, req.getAssigneeAgentId(), req.getTitle().trim());
        boolean byPmAgent = dispatchingPmAgentId != null;
        Map<String, Object> dispDetail = new LinkedHashMap<>();
        dispDetail.put("title", t.getTitle());
        dispDetail.put("assigneeAgentId", req.getAssigneeAgentId());
        dispDetail.put("moduleId", req.getModuleId());
        dispDetail.put("channel", byPmAgent ? "PM_AGENT_TOOL" : "HTTP");
        flowRecorder.record(
                projectId,
                taskId,
                MagTaskFlowEventType.TASK_DISPATCHED,
                byPmAgent ? "AGENT" : "USER",
                byPmAgent ? dispatchingPmAgentId : null,
                byPmAgent ? "PM Agent 派工 → 待处理" : "项目经理派工 → 待处理",
                dispDetail);
        if (taskAutomationProperties.isAutoStartOnDispatch()) {
            eventPublisher.publishEvent(new MagTaskAutoOrchestrateEvent(taskId, projectId, userId, "DISPATCH"));
        }
        return toRow(taskMapper.selectById(taskId));
    }

    /**
     * 项目经理改派：待处理、进行中、阻塞状态下可调整执行 Agent。
     */
    @Transactional
    public Map<String, Object> pmReassign(Long taskId, MagTaskPmReassignRequest req, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        String st = task.getState();
        if (!MagConstants.TASK_PENDING.equals(st)
                && !MagConstants.TASK_IN_PROGRESS.equals(st)
                && !MagConstants.TASK_BLOCKED.equals(st)) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID);
        }
        requireAssignableAgent(task.getProjectId(), req.getAssigneeAgentId());
        task.setAssigneeAgentId(req.getAssigneeAgentId());
        taskMapper.update(task);
        postCoordAssignMessage(task.getProjectId(), taskId, req.getAssigneeAgentId(), task.getTitle());
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_PM_REASSIGNED,
                "USER",
                null,
                "项目经理改派执行 Agent",
                Map.of("newAssigneeAgentId", req.getAssigneeAgentId(), "state", st));
        if (taskAutomationProperties.isAutoStartOnDispatch() && MagConstants.TASK_PENDING.equals(st)) {
            eventPublisher.publishEvent(
                    new MagTaskAutoOrchestrateEvent(taskId, task.getProjectId(), userId, "REASSIGN"));
        }
        return toRow(taskMapper.selectById(taskId));
    }

    /**
     * Agent 编排 Activity 失败落库后调用：若任务仍为进行中且执行人与本次编排 Agent 一致，则置为阻塞并写入原因。
     * <p>不重复发 {@code TASK_BLOCKED} 告警（编排侧已有 {@code ORCH_ACTIVITY_FAILED}）。异常吞掉以免掩盖 Temporal 失败路径。
     */
    @Transactional
    public void applyAgentOrchestrationActivityFailure(
            Long taskId,
            String runKind,
            Long orchestrationAgentId,
            Long triggerUserId,
            String errorSummary) {
        try {
            if (taskId == null
                    || orchestrationAgentId == null
                    || !MagConstants.ORCH_RUN_KIND_AGENT.equals(runKind)) {
                return;
            }
            MagTask task = taskMapper.selectById(taskId);
            if (task == null) {
                return;
            }
            if (!MagConstants.TASK_IN_PROGRESS.equals(task.getState())) {
                return;
            }
            if (!Objects.equals(task.getAssigneeAgentId(), orchestrationAgentId)) {
                return;
            }
            if (triggerUserId != null) {
                accessHelper.requireMember(task.getProjectId(), triggerUserId);
            }
            String prefix = "Agent编排执行失败：";
            String tail =
                    StringUtils.hasText(errorSummary)
                            ? errorSummary.trim()
                            : MagConstants.ORCH_STATUS_FAILED;
            String reason = trimBlockReason(prefix + tail);
            task.setState(MagConstants.TASK_BLOCKED);
            task.setBlockReason(reason);
            task.setBlockedByAgentId(orchestrationAgentId);
            taskMapper.update(task);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("assigneeAgentId", orchestrationAgentId);
            if (StringUtils.hasText(errorSummary)) {
                detail.put(
                        "errorSummary",
                        errorSummary.length() > 400 ? errorSummary.substring(0, 400) + "…" : errorSummary);
            }
            flowRecorder.record(
                    task.getProjectId(),
                    taskId,
                    MagTaskFlowEventType.TASK_ORCHESTRATION_FAILED,
                    "SYSTEM",
                    null,
                    "Agent 编排执行失败 → 阻塞（进行中 → 阻塞）",
                    detail);
        } catch (Exception e) {
            log.warn(
                    "MAG apply orchestration activity failure to task failed taskId={}: {}",
                    taskId,
                    e.toString());
        }
    }

    private static String trimBlockReason(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() <= BLOCK_REASON_MAX) {
            return s;
        }
        return s.substring(0, BLOCK_REASON_MAX - 1) + "…";
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
        Map<String, Object> startDetail = new LinkedHashMap<>();
        startDetail.put("assigneeAgentId", task.getAssigneeAgentId());
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_STARTED,
                "USER",
                null,
                "执行方开始干活（待处理 → 进行中）",
                startDetail);
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
        int updated =
                taskMapper.updateStateWithVersion(taskId, MagConstants.TASK_DONE, expected, null);
        if (updated == 0) {
            throw new MagBusinessException(MagResultCode.MAG_ROW_VERSION_CONFLICT);
        }
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_SUBMIT_COMPLETE,
                "USER",
                null,
                "申报完成（进行中 → 已完成）",
                Map.of());
        eventPublisher.publishEvent(new MagTaskMarkedDoneEvent(task.getProjectId(), taskId, userId));
    }

    /**
     * 在关联任务的 Agent 编排 Activity 已成功结束且事务已提交后调用：
     * 若任务仍为「进行中」、执行 Agent 与编排一致，且编排最终回复非空（trim 后），
     * 则自动申报完成（与 HTTP「申报完成」写入同一状态机与流程事件）。
     */
    public void tryAutoSubmitCompleteAfterSuccessfulAgentOrchestration(
            long taskId,
            long orchestrationAgentId,
            long actingUserId,
            LocalDateTime outputWindowStart,
            String orchestrationResultSummary) {
        if (!taskAutomationProperties.isAutoSubmitCompleteOnOrchestrationSuccess()) {
            return;
        }
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        if (!MagConstants.TASK_IN_PROGRESS.equals(task.getState())) {
            log.debug(
                    "skip auto submit-complete: taskId={} state={} (expected IN_PROGRESS)",
                    taskId,
                    task.getState());
            return;
        }
        if (!Objects.equals(task.getAssigneeAgentId(), orchestrationAgentId)) {
            log.debug(
                    "skip auto submit-complete: taskId={} assigneeAgentId={} orchAgentId={}",
                    taskId,
                    task.getAssigneeAgentId(),
                    orchestrationAgentId);
            return;
        }
        if (!hasNonEmptyOrchestrationReply(orchestrationResultSummary)) {
            log.debug(
                    "skip auto submit-complete: taskId={} empty orchestration reply",
                    taskId);
            return;
        }
        try {
            submitComplete(taskId, new MagSubmitCompleteRequest(), actingUserId);
            log.info(
                    "MAG auto submit-complete: taskId={} projectId={} after successful agent orchestration",
                    taskId,
                    task.getProjectId());
        } catch (Exception e) {
            log.warn(
                    "MAG auto submit-complete failed taskId={}: {}",
                    taskId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private static boolean hasNonEmptyOrchestrationReply(String orchestrationResultSummary) {
        return orchestrationResultSummary != null && !orchestrationResultSummary.trim().isEmpty();
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
        long threadId = coordinationChatWriter.resolveThreadIdForCoordOrPmTask(task.getProjectId(), taskId);
        MagMessage m = new MagMessage();
        m.setThreadId(threadId);
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
        boolean byAgent = req.getBlockedByAgentId() != null;
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_BLOCKED,
                byAgent ? "AGENT" : "USER",
                req.getBlockedByAgentId(),
                "任务阻塞",
                Map.of("reason", req.getReason() != null ? req.getReason() : ""));
        Map<String, Object> alertPayload = new LinkedHashMap<>();
        alertPayload.put("reason", req.getReason() != null ? req.getReason() : "");
        alertPayload.put("assigneeAgentId", task.getAssigneeAgentId());
        alertPayload.put("blockedByAgentId", req.getBlockedByAgentId());
        alertService.raise(task.getProjectId(), taskId, "TASK_BLOCKED", "WARN", alertPayload);
    }

    @Transactional
    public void requestNext(Long taskId, MagTaskRequestNextRequest req, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        long threadId = coordinationChatWriter.resolveThreadIdForCoordOrPmTask(task.getProjectId(), taskId);
        MagMessage m = new MagMessage();
        m.setThreadId(threadId);
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
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_REQUEST_NEXT,
                "AGENT",
                req.getAgentId(),
                "子/执行 Agent 要活（申请下一项工作）",
                Map.of("agentId", req.getAgentId()));
        alertService.raise(
                task.getProjectId(),
                taskId,
                "TASK_REQUEST_NEXT",
                "WARN",
                Map.of(
                        "agentId",
                        req.getAgentId(),
                        "hint",
                        "执行 Agent 反馈当前任务难以继续，需要协调或新派工"));
    }

    private void requireModuleInProject(Long projectId, Long moduleId) {
        if (moduleId == null) {
            return;
        }
        MagModule m = moduleMapper.selectById(moduleId);
        if (m == null || !projectId.equals(m.getProjectId())) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
    }

    private void requireAssignableAgent(Long projectId, Long agentId) {
        MagAgent a = agentMapper.selectById(agentId);
        if (a == null || !projectId.equals(a.getProjectId())) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
    }

    private void postCoordAssignMessage(Long projectId, Long taskId, Long assigneeAgentId, String title) {
        long threadId = coordinationChatWriter.resolveThreadIdForCoordOrPmTask(projectId, taskId);
        MagMessage m = new MagMessage();
        m.setThreadId(threadId);
        m.setSenderType("USER");
        m.setSenderAgentId(null);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kind", "ASSIGN");
            payload.put("taskId", taskId);
            payload.put("assigneeAgentId", assigneeAgentId);
            payload.put("title", title);
            m.setContent(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            m.setContent("{\"kind\":\"ASSIGN\",\"taskId\":" + taskId + ",\"assigneeAgentId\":" + assigneeAgentId + "}");
        }
        messageMapper.insert(m);
    }

    private static String taskCommunicationThreadTitle(String taskTitle) {
        String prefix = "任务沟通 · ";
        String t = taskTitle != null ? taskTitle.trim() : "";
        int maxRest = 256 - prefix.length();
        if (maxRest < 8) {
            return prefix.substring(0, Math.min(prefix.length(), 256));
        }
        if (t.length() > maxRest) {
            t = t.substring(0, maxRest - 1) + "…";
        }
        return prefix + t;
    }

    public List<Map<String, Object>> listTaskFlowEvents(Long taskId, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        return flowEventMapper.selectByTaskId(taskId).stream().map(this::flowRow).collect(Collectors.toList());
    }

    /** Agent 编排终态执行记录（成功 / 失败 / 触发被拒），按结束时间倒序 */
    public List<Map<String, Object>> listTaskExecutionLogs(Long taskId, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        return taskExecutionLogMapper.selectByTaskId(taskId).stream()
                .map(this::taskExecutionRow)
                .collect(Collectors.toList());
    }

    private Map<String, Object> flowRow(MagTaskFlowEvent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("projectId", e.getProjectId());
        m.put("taskId", e.getTaskId());
        m.put("eventType", e.getEventType());
        m.put("actorType", e.getActorType());
        m.put("actorAgentId", e.getActorAgentId());
        m.put("summary", e.getSummary());
        m.put("detailJson", e.getDetailJson());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> taskExecutionRow(MagTaskExecutionLog e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("projectId", e.getProjectId());
        m.put("taskId", e.getTaskId());
        m.put("agentId", e.getAgentId());
        m.put("orchestrationRunId", e.getOrchestrationRunId());
        m.put("workflowId", e.getWorkflowId());
        m.put("executionOutcome", e.getExecutionOutcome());
        m.put("resultSummary", e.getResultSummary());
        m.put("triggerUserId", e.getTriggerUserId());
        m.put("startedAt", e.getStartedAt());
        m.put("finishedAt", e.getFinishedAt());
        m.put("createdAt", e.getCreatedAt());
        return m;
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
}
