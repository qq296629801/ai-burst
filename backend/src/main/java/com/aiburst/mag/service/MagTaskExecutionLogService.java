package com.aiburst.mag.service;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.entity.MagOrchestrationRun;
import com.aiburst.mag.entity.MagTaskExecutionLog;
import com.aiburst.mag.mapper.MagTaskExecutionLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 任务维度的 Agent 编排执行留痕（与 {@code mag_orchestration_run} 终态对齐）。
 */
@Service
@RequiredArgsConstructor
public class MagTaskExecutionLogService {

    private static final int SUMMARY_MAX = 1024;

    private final MagTaskExecutionLogMapper executionLogMapper;

    /**
     * 在任务关联的 AGENT 编排达到终态时写入一条记录。
     *
     * @param executionOutcome {@link MagConstants#ORCH_STATUS_SUCCEEDED}、{@link MagConstants#ORCH_STATUS_FAILED}，
     *                         或触发拒绝 {@link MagConstants#EXECUTION_OUTCOME_TRIGGER_REJECTED}
     */
    @Transactional
    public void recordFromAgentOrchestration(MagOrchestrationRun row, String executionOutcome, String resultSummary) {
        if (row == null || row.getId() == null) {
            return;
        }
        if (row.getTaskId() == null || row.getAgentId() == null) {
            return;
        }
        if (!MagConstants.ORCH_RUN_KIND_AGENT.equals(row.getRunKind())) {
            return;
        }
        LocalDateTime started = row.getStartedAt() != null ? row.getStartedAt() : LocalDateTime.now();
        LocalDateTime finished = row.getFinishedAt() != null ? row.getFinishedAt() : LocalDateTime.now();
        MagTaskExecutionLog log = new MagTaskExecutionLog();
        log.setProjectId(row.getProjectId());
        log.setTaskId(row.getTaskId());
        log.setAgentId(row.getAgentId());
        log.setOrchestrationRunId(row.getId());
        log.setWorkflowId(row.getWorkflowId());
        log.setExecutionOutcome(executionOutcome);
        log.setResultSummary(trim(resultSummary, SUMMARY_MAX));
        log.setTriggerUserId(row.getTriggerUserId());
        log.setStartedAt(started);
        log.setFinishedAt(finished);
        executionLogMapper.insert(log);
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
