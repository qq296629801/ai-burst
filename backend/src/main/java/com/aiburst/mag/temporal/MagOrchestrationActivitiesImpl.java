package com.aiburst.mag.temporal;

import com.aiburst.mag.agentscope.MagAgentScopeRunService;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.aiburst.mag.service.MagOrchestrationRunService;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * MAG 编排 Activity：Agent run 经 AgentScope 调用绑定通道；线程编排仍为占位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagOrchestrationActivitiesImpl implements MagOrchestrationActivities {

    private final MagAgentMapper agentMapper;
    private final MagThreadMapper threadMapper;
    private final MagOrchestrationRunService orchestrationRunService;
    private final MagAgentScopeRunService magAgentScopeRunService;

    @Override
    public String executeAgentRun(long agentId, long triggerUserId, String instruction) {
        String workflowId = Activity.getExecutionContext().getInfo().getWorkflowId();
        orchestrationRunService.markActivityStarted(workflowId);
        try {
            MagAgent a = agentMapper.selectById(agentId);
            if (a == null) {
                throw new IllegalArgumentException("agent not found: " + agentId);
            }
            String safeInstr = instruction != null ? instruction : "";
            String result = magAgentScopeRunService.executeAgentRun(a, triggerUserId, safeInstr);
            orchestrationRunService.markActivitySucceeded(workflowId, result);
            return result;
        } catch (Exception e) {
            orchestrationRunService.markActivityFailed(
                    workflowId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public String executeThreadRun(long threadId, long triggerUserId) {
        String workflowId = Activity.getExecutionContext().getInfo().getWorkflowId();
        orchestrationRunService.markActivityStarted(workflowId);
        try {
            MagThread t = threadMapper.selectById(threadId);
            if (t == null) {
                throw new IllegalArgumentException("thread not found: " + threadId);
            }
            log.info(
                    "MAG Temporal Activity executeThreadRun threadId={} projectId={} triggerUserId={} (AgentScope 未接线程侧通道，占位)",
                    threadId,
                    t.getProjectId(),
                    triggerUserId);
            String result = "OK";
            orchestrationRunService.markActivitySucceeded(workflowId, result);
            return result;
        } catch (Exception e) {
            orchestrationRunService.markActivityFailed(
                    workflowId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            throw e;
        }
    }
}
