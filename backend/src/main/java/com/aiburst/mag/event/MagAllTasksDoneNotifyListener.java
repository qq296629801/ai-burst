package com.aiburst.mag.event;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.service.MagAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * 核查结项（PASS→DONE）后：若项目内**全部**任务均为已完成，写入告警供用户在项目「告警」中查看，
 * 提示本阶段任务已清空、需由用户或项目经理再派发新任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagAllTasksDoneNotifyListener {

    private final MagTaskMapper taskMapper;
    private final MagAlertService alertService;
    private final MagTaskAutomationProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerifiedPass(MagTaskVerifiedPassEvent ev) {
        if (!props.isNotifyWhenAllTasksDone()) {
            return;
        }
        List<MagTask> tasks = taskMapper.selectByProjectId(ev.projectId());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        boolean allDone =
                tasks.stream().allMatch(t -> t.getState() != null && MagConstants.TASK_DONE.equals(t.getState()));
        if (!allDone) {
            return;
        }
        alertService.raise(
                ev.projectId(),
                null,
                "PROJECT_ALL_TASKS_DONE",
                "INFO",
                Map.of(
                        "title",
                        "本阶段任务已全部完成",
                        "message",
                        "项目内全部任务状态均为「已完成」。若需继续推进，请由您或项目经理 Agent 派发新任务。",
                        "completedTaskId",
                        ev.taskId(),
                        "taskCount",
                        tasks.size()));
        log.info(
                "MAG notify user: all tasks DONE projectId={} taskCount={} triggerTaskId={}",
                ev.projectId(),
                tasks.size(),
                ev.taskId());
    }
}
