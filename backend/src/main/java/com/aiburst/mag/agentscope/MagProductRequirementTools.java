package com.aiburst.mag.agentscope;

import com.aiburst.mag.service.MagRequirementService;
import com.aiburst.mag.service.MagTaskService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;

/**
 * 产品（PRODUCT）职能：读取项目需求文档、将开发需求说明合并进需求文档新版本。
 */
@RequiredArgsConstructor
public final class MagProductRequirementTools {

    private final long projectId;
    private final long triggerUserId;
    /** 当前执行编排的 Agent（须与任务 assignee 一致方可自动结项） */
    private final long agentId;
    private final MagRequirementService requirementService;
    private final MagTaskService taskService;

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
                    "根据需求文档整理「开发侧需求说明」，直接合并进需求文档新版本（无需求池）。"
                            + "summary 必填；proposedMarkdown 为建议正文（会并入 mag_requirement_revision.content）。"
                            + "若当前编排关联了任务且写入了新版本，系统可在配置允许时自动将该任务申报为已完成。"
                            + "后续以需求文档编辑与版本列表为确认来源。")
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
                    requirementService.mergeDevRequirementProposedFromAgent(
                            projectId, triggerUserId, summary, proposedMarkdown, anchorJson);
            Object revId = row.get("revisionId");
            Long taskCtx = MagAgentRunTaskContext.get();
            if (taskCtx != null && revId != null) {
                long rid = revId instanceof Number ? ((Number) revId).longValue() : Long.parseLong(String.valueOf(revId));
                if (rid > 0) {
                    taskService.tryAutoSubmitAfterProductRequirementMerge(taskCtx, agentId, triggerUserId, rid);
                }
            }
            return revId != null ? "SUCCESS revisionId=" + revId : "SUCCESS (no content merge)";
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
