package com.aiburst.mag.agentscope;

import com.aiburst.mag.dto.MagImprovementCreateRequest;
import com.aiburst.mag.service.MagImprovementLogService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 开发职能：按前端/后端分层记录实现要点，落库到改进日志，供测试与项目经理跟踪。
 */
@RequiredArgsConstructor
public final class MagDevLayerTools {

    private final long projectId;
    private final long triggerUserId;
    private final long agentId;
    /** 写入改进日志的 changeType（≤32 字符） */
    private final String improvementChangeType;
    private final MagImprovementLogService improvementLogService;

    @Tool(
            name = "mag_record_implementation_plan",
            description =
                    "记录前端或后端实现计划、接口/模块拆分要点，落库改进日志，供测试与项目经理跟踪。")
    public String recordImplementationPlan(
            @ToolParam(name = "title", description = "短标题") String title,
            @ToolParam(
                            name = "detailMarkdown",
                            description = "详细说明（Markdown），可空",
                            required = false)
                    String detailMarkdown) {
        if (!StringUtils.hasText(title)) {
            return "ERROR title required";
        }
        try {
            MagImprovementCreateRequest req = new MagImprovementCreateRequest();
            String ct =
                    improvementChangeType.length() > 32
                            ? improvementChangeType.substring(0, 32)
                            : improvementChangeType;
            req.setChangeType(ct);
            req.setSummary(title.trim().length() > 512 ? title.trim().substring(0, 512) : title.trim());
            req.setDetailJson(detailMarkdown != null ? detailMarkdown : "");
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
