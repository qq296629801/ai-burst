package com.aiburst.mag.event;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagAgentMapper;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 申报完成进入待核查后：可选自动 begin-verify 并触发 VERIFY Agent 编排。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagTaskVerifyAutomationListener {

    private final MagTaskMapper taskMapper;
    private final MagAgentMapper agentMapper;
    private final MagTaskService taskService;
    private final MagAgentService agentService;
    private final MagAlertService alertService;
    private final MagTaskAutomationProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPendingVerify(MagTaskPendingVerifyEvent ev) {
        if (!props.isAutoVerifyOnSubmitComplete()) {
            return;
        }
        MagTask task = taskMapper.selectById(ev.taskId());
        if (task == null) {
            return;
        }
        if (ev.projectId() != task.getProjectId()) {
            return;
        }
        if (!MagConstants.TASK_PENDING_VERIFY.equals(task.getState())) {
            log.debug("skip auto verify taskId={} state={}", ev.taskId(), task.getState());
            return;
        }
        List<MagAgent> verifyAgents =
                agentMapper.selectByProjectId(task.getProjectId()).stream()
                        .filter(a -> "VERIFY".equals(a.getRoleType()))
                        .filter(a -> a.getStatus() == null || a.getStatus() != 0)
                        .filter(a -> a.getLlmChannelId() != null)
                        .collect(Collectors.toList());
        if (verifyAgents.isEmpty()) {
            log.warn("MAG auto verify skipped: no VERIFY agent with channel taskId={}", ev.taskId());
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_VERIFY_NO_AGENT",
                    "WARN",
                    Map.of(
                            "hint",
                            "任务已待核查，但项目内无已启用且绑定大模型通道的 VERIFY Agent；请人工核查或配置 VERIFY Agent"));
            return;
        }
        MagAgent verifyAgent = verifyAgents.get(0);
        try {
            taskService.beginVerify(ev.taskId(), ev.actingUserId());
        } catch (Exception e) {
            log.warn("MAG beginVerify failed taskId={}", ev.taskId(), e);
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_AUTO_BEGIN_VERIFY_FAILED",
                    "ERROR",
                    Map.of(
                            "reason",
                            e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() != null ? e.getMessage() : ""));
            return;
        }
        Map<String, Object> runRes;
        try {
            runRes =
                    agentService.requestAgentRun(
                            verifyAgent.getId(),
                            ev.actingUserId(),
                            buildVerifyInstruction(task),
                            task.getId());
        } catch (Exception e) {
            log.warn("MAG auto verify agent run failed taskId={}", ev.taskId(), e);
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_AUTO_VERIFY_AGENT_RUN_EXCEPTION",
                    "ERROR",
                    Map.of(
                            "reason",
                            e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() != null ? e.getMessage() : "",
                            "verifyAgentId",
                            verifyAgent.getId()));
            return;
        }
        if (!Boolean.TRUE.equals(runRes.get("accepted"))) {
            alertService.raise(
                    task.getProjectId(),
                    task.getId(),
                    "TASK_AUTO_VERIFY_ORCH_REJECTED",
                    "WARN",
                    Map.of(
                            "message",
                            runRes.get("message") != null ? String.valueOf(runRes.get("message")) : "编排未接受",
                            "verifyAgentId",
                            verifyAgent.getId()));
        }
    }

    private static String buildVerifyInstruction(MagTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("【系统自动核查】任务已申报完成，当前为核查中。请根据任务与交付物进行核查。\n");
        sb.append("taskId=").append(task.getId());
        sb.append(" projectId=").append(task.getProjectId()).append('\n');
        sb.append("标题：").append(task.getTitle()).append('\n');
        if (StringUtils.hasText(task.getDescription())) {
            sb.append("说明：").append(task.getDescription()).append('\n');
        }
        sb.append("请调用工具 mag_submit_task_verification：result 填 PASS 或 FAIL，rationale 写明依据；");
        sb.append("可选 evidenceSummary。PASS 将结项 DONE，FAIL 将退回执行方 IN_PROGRESS。");
        return sb.toString();
    }
}
