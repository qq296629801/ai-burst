package com.aiburst.mag.agentscope;

import com.aiburst.mag.dto.MagTaskVerifyDecisionRequest;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.service.MagTaskService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * VERIFY 职能：对「待核查 / 核查中」任务提交结论，写入 {@code mag_task_verification} 并推进状态。
 */
@RequiredArgsConstructor
public final class MagVerifyTaskTools {

    private final long projectId;
    private final long triggerUserId;
    private final long verifierAgentId;
    private final MagTaskMapper taskMapper;
    private final MagTaskService magTaskService;

    @Tool(
            name = "mag_submit_task_verification",
            description =
                    "任务核查裁定：对 taskId 对应任务提交 PASS（通过结项）或 FAIL（退回执行方）。"
                            + "任务须处于待核查(PENDING_VERIFY)或核查中(VERIFYING)；"
                            + "须由 VERIFY 角色 Agent 调用；rationale 为必填结论说明。")
    public String submitTaskVerification(
            @ToolParam(name = "taskId", description = "任务数字 id") long taskId,
            @ToolParam(name = "result", description = "PASS 或 FAIL（大小写不敏感）") String result,
            @ToolParam(name = "rationale", description = "核查结论与依据") String rationale,
            @ToolParam(name = "evidenceSummary", description = "可选：证据或抽查范围摘要")
                    String evidenceSummary) {
        if (!StringUtils.hasText(result) || !StringUtils.hasText(rationale)) {
            return "ERROR result and rationale required";
        }
        MagTask task = taskMapper.selectById(taskId);
        if (task == null || !java.util.Objects.equals(task.getProjectId(), projectId)) {
            return "ERROR task not in project";
        }
        MagTaskVerifyDecisionRequest req = new MagTaskVerifyDecisionRequest();
        req.setResult(result.trim());
        req.setVerifierAgentId(verifierAgentId);
        req.setRationale(rationale.trim());
        req.setEvidenceSummary(StringUtils.hasText(evidenceSummary) ? evidenceSummary.trim() : null);
        req.setRowVersion(task.getRowVersion());
        try {
            magTaskService.submitVerificationDecision(taskId, req, triggerUserId, true);
        } catch (Exception e) {
            return "ERROR "
                    + e.getClass().getSimpleName()
                    + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
        }
        return "SUCCESS submitted verification for taskId="
                + taskId
                + " (PASS→DONE, FAIL→IN_PROGRESS)";
    }
}
