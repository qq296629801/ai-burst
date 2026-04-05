package com.aiburst.mag.event;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.service.MagAgentService;
import com.aiburst.mag.service.MagAlertService;
import com.aiburst.mag.service.MagTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 派工/待处理态改派提交后：自动触发执行 Agent 编排，编排受理成功后再将任务置为进行中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagTaskDispatchAutomationListener {

    private final MagTaskMapper taskMapper;
    private final MagTaskService taskService;
    private final MagAgentService agentService;
    private final MagAlertService alertService;
    private final MagTaskAutomationProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAutoOrchestrate(MagTaskAutoOrchestrateEvent ev) {
        if (!props.isAutoStartOnDispatch()) {
            return;
        }
        MagTask task = taskMapper.selectById(ev.taskId());
        if (task == null) {
            return;
        }
        if (ev.projectId() != task.getProjectId()) {
            return;
        }
        if (task.getAssigneeAgentId() == null) {
            return;
        }
        if (!MagConstants.TASK_PENDING.equals(task.getState())) {
            log.debug("skip auto orch taskId={} state={}", ev.taskId(), task.getState());
            return;
        }
        Map<String, Object> runRes;
        try {
            runRes =
                    agentService.requestAgentRun(
                            task.getAssigneeAgentId(),
                            ev.dispatchUserId(),
                            buildAgentInstruction(task),
                            task.getId());
        } catch (Exception e) {
            log.warn("MAG auto agent run failed taskId={}", ev.taskId(), e);
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_AUTO_AGENT_RUN_EXCEPTION",
                    "ERROR",
                    Map.of(
                            "reason",
                            e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() != null ? e.getMessage() : ""));
            return;
        }
        if (!Boolean.TRUE.equals(runRes.get("accepted"))) {
            return;
        }
        try {
            taskService.start(ev.taskId(), ev.dispatchUserId());
        } catch (Exception e) {
            log.warn("MAG auto start task failed taskId={}", ev.taskId(), e);
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_AUTO_START_FAILED",
                    "ERROR",
                    Map.of(
                            "reason",
                            e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() != null ? e.getMessage() : ""));
        }
    }

    private static String buildAgentInstruction(MagTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("【系统自动派工】请执行当前任务。\n");
        sb.append("taskId=").append(task.getId());
        sb.append(" projectId=").append(task.getProjectId()).append('\n');
        sb.append("标题：").append(task.getTitle()).append('\n');
        if (StringUtils.hasText(task.getDescription())) {
            sb.append("说明：").append(task.getDescription()).append('\n');
        }
        sb.append("请根据你的角色与工具推进实现；完成后由用户或流程「申报完成」。");
        return sb.toString();
    }
}
