package com.aiburst.mag.event;

import com.aiburst.mag.config.MagTaskAutomationProperties;
import com.aiburst.mag.service.MagTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 执行方 Agent 编排成功落库后（事务提交后）：可选根据产出物自动申报完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagTaskAutoSubmitOnOrchestrationListener {

    private final MagTaskService taskService;
    private final MagTaskAutomationProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgentRunSucceeded(MagOrchestrationAgentRunSucceededEvent ev) {
        if (!props.isAutoSubmitCompleteOnOrchestrationSuccess()) {
            return;
        }
        taskService.tryAutoSubmitCompleteAfterSuccessfulAgentOrchestration(
                ev.taskId(),
                ev.agentId(),
                ev.triggerUserId(),
                ev.outputWindowStart(),
                ev.resultSummary());
    }
}
