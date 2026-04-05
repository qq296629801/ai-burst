package com.aiburst.mag.agentscope;

import com.aiburst.mag.dto.MagImprovementCreateRequest;
import com.aiburst.mag.service.MagImprovementLogService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 测试/核查（VERIFY）：记录单元测试与断言设计，落库改进日志；具体执行测试仍由 CI/本地工具链完成。
 */
@RequiredArgsConstructor
public final class MagVerifyTestTools {

    private final long projectId;
    private final long triggerUserId;
    private final long agentId;
    private final MagImprovementLogService improvementLogService;

    @Tool(
            name = "mag_record_unit_test_plan",
            description =
                    "根据开发交付说明，记录单元测试范围、用例要点与关键断言（Markdown）。"
                            + "用于测试职能与项目经理 visibility。")
    public String recordUnitTestPlan(
            @ToolParam(name = "scopeSummary", description = "测试范围一句话") String scopeSummary,
            @ToolParam(name = "casesMarkdown", description = "用例与断言说明（Markdown）") String casesMarkdown) {
        if (!StringUtils.hasText(scopeSummary) || !StringUtils.hasText(casesMarkdown)) {
            return "ERROR scopeSummary and casesMarkdown required";
        }
        try {
            MagImprovementCreateRequest req = new MagImprovementCreateRequest();
            req.setChangeType("VERIFY_UNIT_PLAN");
            req.setSummary(
                    scopeSummary.trim().length() > 512
                            ? scopeSummary.trim().substring(0, 512)
                            : scopeSummary.trim());
            req.setDetailJson(casesMarkdown);
            var row = improvementLogService.append(projectId, agentId, req, triggerUserId);
            return "SUCCESS improvementId=" + row.get("id");
        } catch (Exception e) {
            return "ERROR "
                    + e.getClass().getSimpleName()
                    + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
        }
    }
}
