package com.aiburst.mag.config;

import com.aiburst.mag.temporal.MagAgentRunWorkflow;
import com.aiburst.mag.temporal.MagThreadRunWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 在 Temporal 可用时启动 Agent/线程编排 Workflow；不可用时返回与 {@link MagTemporalGate} 一致的阻断结构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagTemporalTriggerService {

    private static final String MSG_STARTED = "已提交 Temporal Workflow，Worker 将执行 Activity。";
    private static final String MSG_NO_CLIENT = "Temporal 已启用但未创建 WorkflowClient，请检查配置类是否加载。";

    private final MagTemporalGate gate;
    private final MagTemporalProperties properties;
    private final ObjectProvider<WorkflowClient> workflowClientProvider;

    public Map<String, Object> triggerAgentRun(long agentId, long userId, String hint, String workflowInstruction) {
        return triggerAgentRun(agentId, userId, hint, workflowInstruction, null);
    }

    /**
     * @param taskContextTaskId 非空则传入 Workflow，供 Activity 将对话气泡写入对应任务的沟通线程
     */
    public Map<String, Object> triggerAgentRun(
            long agentId, long userId, String hint, String workflowInstruction, Long taskContextTaskId) {
        Optional<Map<String, Object>> block = gate.blockIfAny(hint);
        if (block.isPresent()) {
            Map<String, Object> m = new HashMap<>(block.get());
            m.put("agentId", agentId);
            return m;
        }
        WorkflowClient wc = workflowClientProvider.getIfAvailable();
        if (wc == null) {
            return clientMissingResponse(hint, "agentId", agentId);
        }
        String workflowId = "mag-agent-" + agentId + "-" + UUID.randomUUID();
        WorkflowOptions options =
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(properties.getTaskQueue())
                        .build();
        MagAgentRunWorkflow stub = wc.newWorkflowStub(MagAgentRunWorkflow.class, options);
        try {
            String instr = workflowInstruction != null ? workflowInstruction : "";
            long tid = taskContextTaskId != null ? taskContextTaskId : 0L;
            int actMin = properties.getEffectiveActivityStartToCloseMinutes();
            WorkflowClient.start(stub::execute, agentId, userId, instr, tid, actMin);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("MAG agent workflow already started: {}", workflowId);
            return successResponse(workflowId, MSG_STARTED, "agentId", agentId);
        } catch (Exception e) {
            log.error("MAG start agent workflow failed", e);
            return failureResponse(
                    hint,
                    "启动 Workflow 失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    "agentId",
                    agentId);
        }
        return successResponse(workflowId, MSG_STARTED, "agentId", agentId);
    }

    public Map<String, Object> triggerThreadRun(long threadId, long userId, String hint) {
        Optional<Map<String, Object>> block = gate.blockIfAny(hint);
        if (block.isPresent()) {
            Map<String, Object> m = new HashMap<>(block.get());
            m.put("threadId", threadId);
            return m;
        }
        WorkflowClient wc = workflowClientProvider.getIfAvailable();
        if (wc == null) {
            return clientMissingResponse(hint, "threadId", threadId);
        }
        String workflowId = "mag-thread-" + threadId + "-" + UUID.randomUUID();
        WorkflowOptions options =
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(properties.getTaskQueue())
                        .build();
        MagThreadRunWorkflow stub = wc.newWorkflowStub(MagThreadRunWorkflow.class, options);
        try {
            int actMin = properties.getEffectiveActivityStartToCloseMinutes();
            WorkflowClient.start(stub::execute, threadId, userId, actMin);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("MAG thread workflow already started: {}", workflowId);
            return successResponse(workflowId, MSG_STARTED, "threadId", threadId);
        } catch (Exception e) {
            log.error("MAG start thread workflow failed", e);
            return failureResponse(
                    hint,
                    "启动 Workflow 失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    "threadId",
                    threadId);
        }
        return successResponse(workflowId, MSG_STARTED, "threadId", threadId);
    }

    private static Map<String, Object> clientMissingResponse(String hint, String idKey, long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("accepted", false);
        m.put("temporalEnabled", true);
        m.put("temporalReachable", true);
        m.put("message", MSG_NO_CLIENT);
        m.put(idKey, id);
        if (hint != null) {
            m.put("hint", hint);
        }
        return m;
    }

    private static Map<String, Object> failureResponse(String hint, String message, String idKey, long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("accepted", false);
        m.put("temporalEnabled", true);
        m.put("temporalReachable", true);
        m.put("message", message);
        m.put(idKey, id);
        if (hint != null) {
            m.put("hint", hint);
        }
        return m;
    }

    private static Map<String, Object> successResponse(String workflowId, String message, String idKey, long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("accepted", true);
        m.put("temporalEnabled", true);
        m.put("temporalReachable", true);
        m.put("workflowId", workflowId);
        m.put("message", message);
        m.put(idKey, id);
        return m;
    }
}
