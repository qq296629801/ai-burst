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
import com.aiburst.mag.dto.MagTaskVerifyDecisionRequest;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagModule;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.entity.MagTaskVerification;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagModuleMapper;
import com.aiburst.mag.entity.MagTaskFlowEvent;
import com.aiburst.mag.mapper.MagTaskFlowEventMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.mapper.MagTaskVerificationMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.event.MagTaskAutoOrchestrateEvent;
import com.aiburst.mag.event.MagTaskPendingVerifyEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final MagAgentMapper agentMapper;
    private final MagModuleMapper moduleMapper;
    private final ObjectMapper objectMapper;
    private final MagTaskFlowRecorder flowRecorder;
    private final MagTaskFlowEventMapper flowEventMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MagTaskAutomationProperties taskAutomationProperties;
    private final MagAlertService alertService;

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
     * 项目经理派工：创建任务并必须指定执行 Agent（产品 §4.2）；核查 Agent 不可作为交付执行人。
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
        String wfId = "stub-" + taskId + "-" + UUID.randomUUID();
        int updated = taskMapper.updateStateWithVersion(taskId, MagConstants.TASK_PENDING_VERIFY, expected, wfId);
        if (updated == 0) {
            throw new MagBusinessException(MagResultCode.MAG_ROW_VERSION_CONFLICT);
        }
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_SUBMIT_COMPLETE,
                "USER",
                null,
                "申报完成（进行中 → 待核查）",
                Map.of("temporalWorkflowId", wfId));
        eventPublisher.publishEvent(new MagTaskPendingVerifyEvent(taskId, task.getProjectId(), userId));
    }

    /**
     * 待核查 → 核查中。供自动化或人工在触发 VERIFY 编排前调用。
     */
    @Transactional
    public void beginVerify(Long taskId, Long actingUserId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), actingUserId);
        if (!MagConstants.TASK_PENDING_VERIFY.equals(task.getState())) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID, "仅待核查状态可进入核查中");
        }
        int expected = task.getRowVersion() != null ? task.getRowVersion() : 0;
        String wf = task.getTemporalWorkflowId();
        int n = taskMapper.updateStateWithVersion(taskId, MagConstants.TASK_VERIFYING, expected, wf);
        if (n == 0) {
            throw new MagBusinessException(MagResultCode.MAG_ROW_VERSION_CONFLICT);
        }
        flowRecorder.record(
                task.getProjectId(),
                taskId,
                MagTaskFlowEventType.TASK_VERIFYING_STARTED,
                "USER",
                null,
                "进入核查中（待核查 → 核查中）",
                Map.of());
    }

    /**
     * 记录核查结论并推进状态：PASS→DONE，FAIL→IN_PROGRESS。须在 {@code PENDING_VERIFY} 或 {@code VERIFYING} 下调用。
     *
     * @param verifierActedViaAgentScopeTool true 表示由 VERIFY Agent 工具提交（流程事件行为体为 AGENT）
     */
    @Transactional
    public void submitVerificationDecision(
            Long taskId,
            MagTaskVerifyDecisionRequest req,
            Long actingUserId,
            boolean verifierActedViaAgentScopeTool) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), actingUserId);
        String state = task.getState();
        if (!MagConstants.TASK_PENDING_VERIFY.equals(state) && !MagConstants.TASK_VERIFYING.equals(state)) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID, "仅待核查或核查中可提交核查结论");
        }
        if (req.getVerifierAgentId() == null) {
            throw new IllegalArgumentException("verifierAgentId required");
        }
        MagAgent verifier = agentMapper.selectById(req.getVerifierAgentId());
        requireVerifierAgentInProject(verifier, task.getProjectId());

        String result = req.getResult() != null ? req.getResult().trim().toUpperCase() : "";
        if (!"PASS".equals(result) && !"FAIL".equals(result)) {
            throw new IllegalArgumentException("result must be PASS or FAIL");
        }
        if (!StringUtils.hasText(req.getRationale())) {
            throw new IllegalArgumentException("rationale required");
        }

        MagTaskVerification row = new MagTaskVerification();
        row.setTaskId(taskId);
        row.setResult(result);
        row.setVerifierAgentId(req.getVerifierAgentId());
        row.setRationale(req.getRationale().trim());
        row.setEvidenceSummary(
                StringUtils.hasText(req.getEvidenceSummary()) ? req.getEvidenceSummary().trim() : null);
        row.setSearchTraceJson(null);
        verificationMapper.insert(row);

        int expected = req.getRowVersion() != null ? req.getRowVersion() : task.getRowVersion();
        String newState = "PASS".equals(result) ? MagConstants.TASK_DONE : MagConstants.TASK_IN_PROGRESS;
        int updated = taskMapper.updateStateWithVersion(taskId, newState, expected, null);
        if (updated == 0) {
            throw new MagBusinessException(MagResultCode.MAG_ROW_VERSION_CONFLICT);
        }

        String actorType = verifierActedViaAgentScopeTool ? "AGENT" : "USER";
        Long actorAgentId = verifierActedViaAgentScopeTool ? req.getVerifierAgentId() : null;
        String evt =
                "PASS".equals(result)
                        ? MagTaskFlowEventType.TASK_VERIFICATION_PASS
                        : MagTaskFlowEventType.TASK_VERIFICATION_FAIL;
        String summary =
                "PASS".equals(result)
                        ? "核查通过 → 已完成"
                        : "核查不通过 → 退回执行中";
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("verificationId", row.getId());
        detail.put("verifierAgentId", req.getVerifierAgentId());
        detail.put("result", result);
        if (!verifierActedViaAgentScopeTool) {
            detail.put("submittedByUserId", actingUserId);
        }
        flowRecorder.record(task.getProjectId(), taskId, evt, actorType, actorAgentId, summary, detail);
    }

    private void requireVerifierAgentInProject(MagAgent agent, Long projectId) {
        if (agent == null || !projectId.equals(agent.getProjectId())) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND, "核查 Agent 不存在或不属于本项目");
        }
        if (!"VERIFY".equals(agent.getRoleType())) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID, "核查结论须由 VERIFY 角色 Agent 出具");
        }
        if (agent.getStatus() != null && agent.getStatus() == 0) {
            throw new MagBusinessException(MagResultCode.MAG_TASK_STATE_INVALID, "核查 Agent 已停用");
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
        if ("VERIFY".equals(a.getRoleType())) {
            throw new MagBusinessException(MagResultCode.MAG_VERIFY_ASSIGNEE_CONFLICT);
        }
    }

    private void postCoordAssignMessage(Long projectId, Long taskId, Long assigneeAgentId, String title) {
        MagThread coord = ensureCoordThread(projectId);
        MagMessage m = new MagMessage();
        m.setThreadId(coord.getId());
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

    public List<Map<String, Object>> listTaskFlowEvents(Long taskId, Long userId) {
        MagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(task.getProjectId(), userId);
        return flowEventMapper.selectByTaskId(taskId).stream().map(this::flowRow).collect(Collectors.toList());
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
