package com.aiburst.mag.agentscope;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.dto.MagTaskDispatchRequest;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.service.MagTaskService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 项目经理（PM）Agent 的 AgentScope 工具：列出可指派 Agent、派发任务（写入 {@code mag_task} 与协调线程）。
 */
@RequiredArgsConstructor
public final class MagPmDispatchTools {

    private final long projectId;
    private final long triggerUserId;
    private final long pmAgentId;
    private final MagTaskService taskService;
    private final MagAgentMapper agentMapper;

    @Tool(
            name = "list_dispatchable_agents",
            description =
                    "列出当前项目中可被指派为任务执行人的 Agent（不含核查 VERIFY）。"
                            + "派工前应先调用本工具取得合法的 assigneeAgentId。")
    public String listDispatchableAgents() {
        List<MagAgent> list = agentMapper.selectByProjectId(projectId);
        if (list == null || list.isEmpty()) {
            return "EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        for (MagAgent a : list) {
            if ("VERIFY".equals(a.getRoleType())) {
                continue;
            }
            sb.append("id=")
                    .append(a.getId())
                    .append(" name=")
                    .append(a.getName() != null ? a.getName() : "")
                    .append(" role=")
                    .append(a.getRoleType())
                    .append("; ");
        }
        return sb.length() > 0 ? sb.toString().trim() : "EMPTY";
    }

    @Tool(
            name = "dispatch_task",
            description =
                    "为当前项目创建任务并指派执行 Agent（项目经理派工）。"
                            + "assigneeAgentId 必须来自 list_dispatchable_agents，且不得为 VERIFY。")
    public String dispatchTask(
            @ToolParam(name = "title", description = "任务标题") String title,
            @ToolParam(name = "assigneeAgentId", description = "执行 Agent 的数字 id（本项目、非 VERIFY）")
                    long assigneeAgentId,
            @ToolParam(name = "description", description = "任务说明，可空", required = false) String description,
            @ToolParam(name = "moduleId", description = "关联功能模块 id，可空", required = false) Long moduleId) {
        if (!StringUtils.hasText(title)) {
            return "ERROR title required";
        }
        try {
            MagTaskDispatchRequest req = new MagTaskDispatchRequest();
            req.setTitle(title.trim());
            req.setAssigneeAgentId(assigneeAgentId);
            if (StringUtils.hasText(description)) {
                req.setDescription(description.trim());
            }
            if (moduleId != null) {
                req.setModuleId(moduleId);
            }
            Map<String, Object> row = taskService.dispatch(projectId, req, triggerUserId, pmAgentId);
            Object id = row.get("id");
            return "SUCCESS taskId=" + id + " state=PENDING assigneeAgentId=" + assigneeAgentId;
        } catch (MagBusinessException e) {
            return "ERROR "
                    + e.getResultCode().name()
                    + " "
                    + (e.getMessage() != null ? e.getMessage() : "");
        } catch (Exception e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
