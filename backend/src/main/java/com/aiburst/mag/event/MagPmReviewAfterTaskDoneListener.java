package com.aiburst.mag.event;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.service.MagAgentService;
import com.aiburst.mag.service.MagAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务标记为已完成后：可选自动拉起项目经理 Agent 一轮复盘——查看全项目任务与模块，继续派工或声明阶段闭环。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagPmReviewAfterTaskDoneListener {

    private final MagTaskMapper taskMapper;
    private final MagAgentMapper agentMapper;
    private final MagAgentService agentService;
    private final MagAlertService alertService;
    private final MagTaskAutomationProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskMarkedDone(MagTaskMarkedDoneEvent ev) {
        if (!props.isAutoPmReviewAfterTaskDone()) {
            return;
        }
        MagTask task = taskMapper.selectById(ev.taskId());
        if (task == null || ev.projectId() != task.getProjectId()) {
            return;
        }
        if (!MagConstants.TASK_DONE.equals(task.getState())) {
            log.debug("skip PM review: taskId={} state not DONE", ev.taskId());
            return;
        }
        List<MagAgent> pmAgents =
                agentMapper.selectByProjectId(ev.projectId()).stream()
                        .filter(a -> "PM".equals(a.getRoleType()))
                        .filter(a -> a.getStatus() == null || a.getStatus() != 0)
                        .filter(a -> a.getLlmChannelId() != null)
                        .sorted(Comparator.comparing(MagAgent::getId))
                        .collect(Collectors.toList());
        if (pmAgents.isEmpty()) {
            log.warn("MAG PM review skipped: no PM agent with channel projectId={} taskId={}", ev.projectId(), ev.taskId());
            alertService.raise(
                    ev.projectId(),
                    ev.taskId(),
                    "TASK_DONE_PM_REVIEW_NO_PM",
                    "WARN",
                    Map.of(
                            "hint",
                            "任务已结项，但项目内无已启用且绑定通道的项目经理 Agent，无法自动复盘"));
            return;
        }
        MagAgent pm = pmAgents.get(0);
        String instruction = buildPmReviewInstruction(task);
        try {
            Map<String, Object> runRes =
                    agentService.requestAgentRun(pm.getId(), ev.actingUserId(), instruction, null);
            if (!Boolean.TRUE.equals(runRes.get("accepted"))) {
                alertService.raise(
                        ev.projectId(),
                        ev.taskId(),
                        "TASK_DONE_PM_REVIEW_ORCH_REJECTED",
                        "WARN",
                        Map.of(
                                "message",
                                runRes.get("message") != null ? String.valueOf(runRes.get("message")) : "编排未接受",
                                "pmAgentId",
                                pm.getId()));
            } else {
                log.info(
                        "MAG PM review orchestration accepted projectId={} after taskId={} pmAgentId={}",
                        ev.projectId(),
                        ev.taskId(),
                        pm.getId());
            }
        } catch (Exception e) {
            log.warn("MAG PM review agent run failed projectId={} taskId={}", ev.projectId(), ev.taskId(), e);
            alertService.raise(
                    ev.projectId(),
                    ev.taskId(),
                    "TASK_DONE_PM_REVIEW_RUN_EXCEPTION",
                    "ERROR",
                    Map.of(
                            "reason",
                            e.getClass().getSimpleName(),
                            "message",
                            e.getMessage() != null ? e.getMessage() : "",
                            "pmAgentId",
                            pm.getId()));
        }
    }

    private static String buildPmReviewInstruction(MagTask task) {
        String title = StringUtils.hasText(task.getTitle()) ? task.getTitle().trim() : "(无标题)";
        return "【结项后自动复盘】刚有一项任务已标记为已完成，请你作为项目经理立即复盘全项目进度。\n"
                + "projectId="
                + task.getProjectId()
                + " completedTaskId="
                + task.getId()
                + "\n结项任务标题："
                + title
                + "\n\n必须按顺序用工具完成判断：\n"
                + "1) 调用 list_project_tasks，检查是否仍存在 PENDING、IN_PROGRESS、BLOCKED 等非 DONE 状态的任务；\n"
                + "2) 调用 list_project_modules，对照功能模块与任务覆盖是否仍有缺口；\n"
                + "3) 若存在可执行的缺口，调用 list_dispatchable_agents 再 dispatch_task 继续派工；\n"
                + "4) 若所有任务状态均为 DONE，或当前阶段已无需新增派工，在回复中**明确结论**：本阶段项目任务已全部处理完毕（或说明剩余仅为何种等待/例外）；\n"
                + "5) 若仍有任务在执行链上，说明下一步由谁推进。\n"
                + "回答须简洁，并体现你已读取 list_project_tasks（及必要时 list_project_modules）后的实际判断。";
    }
}
