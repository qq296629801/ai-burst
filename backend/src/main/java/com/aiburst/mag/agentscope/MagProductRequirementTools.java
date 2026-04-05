package com.aiburst.mag.agentscope;

import com.aiburst.mag.service.MagRequirementService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;

/**
 * 产品（PRODUCT）职能：读取项目需求文档、提交「开发需求说明」候选到需求池，供评审合入。
 */
@RequiredArgsConstructor
public final class MagProductRequirementTools {

    private final long projectId;
    private final long triggerUserId;
    private final MagRequirementService requirementService;

    @Tool(
            name = "mag_read_requirement_doc",
            description = "读取当前项目已落库的需求文档正文（可截断）。仅能访问本 projectId 对应数据。")
    public String readRequirementDoc(
            @ToolParam(
                            name = "maxChars",
                            description = "最大字符数，默认 8000，上限 120000",
                            required = false)
                    Integer maxChars) {
        try {
            int n = maxChars != null ? maxChars : 8000;
            return requirementService.readRequirementDocExcerpt(projectId, triggerUserId, n);
        } catch (Exception e) {
            return "ERROR "
                    + e.getClass().getSimpleName()
                    + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
        }
    }

    @Tool(
            name = "mag_submit_dev_requirement_candidate",
            description =
                    "根据需求文档整理「开发侧需求说明」候选，写入需求池（payload），"
                            + "需经人工/项目经理决策后合入正式需求。summary 必填；proposedMarkdown 为建议正文。")
    public String submitDevRequirementCandidate(
            @ToolParam(name = "summary", description = "一句话摘要") String summary,
            @ToolParam(
                            name = "proposedMarkdown",
                            description = "建议的开发需求说明（Markdown），可空",
                            required = false)
                    String proposedMarkdown,
            @ToolParam(name = "anchorJson", description = "锚点 JSON 字符串，可空", required = false)
                    String anchorJson) {
        try {
            java.util.Map<String, Object> row =
                    requirementService.submitRequirementPoolCandidateFromAgent(
                            projectId, triggerUserId, summary, proposedMarkdown, anchorJson);
            return "SUCCESS poolItemId=" + row.get("id");
        } catch (IllegalArgumentException e) {
            return "ERROR " + e.getMessage();
        } catch (Exception e) {
            return "ERROR "
                    + e.getClass().getSimpleName()
                    + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
        }
    }
}
