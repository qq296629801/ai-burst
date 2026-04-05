package com.aiburst.mag.agentscope;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.dto.MagTaskDispatchRequest;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.service.MagModuleService;
import com.aiburst.mag.service.MagTaskService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 项目经理（PM）Agent 的 AgentScope 工具：列出可指派 Agent、派发任务、查看任务与模块以分析进度（写入 {@code mag_task}
 * 与协调线程）。
 */
@RequiredArgsConstructor
public final class MagPmDispatchTools {

    private static final int TASK_LIST_MAX_LINES = 120;

    private final long projectId;
    private final long triggerUserId;
    private final long pmAgentId;
    private final MagTaskService taskService;
    private final MagModuleService moduleService;
    private final MagAgentMapper agentMapper;

    @Tool(
            name = "list_dispatchable_agents",
            description =
                    "列出当前项目中可被指派为任务执行人的 Agent（已启用者）。"
                            + "派工前应先调用本工具取得合法的 assigneeAgentId。")
    public String listDispatchableAgents() {
        List<MagAgent> list = agentMapper.selectByProjectId(projectId);
        if (list == null || list.isEmpty()) {
            return "EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        for (MagAgent a : list) {
            if (a.getStatus() != null && a.getStatus() == 0) {
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
            name = "list_project_tasks",
            description =
                    "列出当前项目全部任务（id、state、title、assigneeAgentId、moduleId）。"
                            + "在派工后、或需要判断还有哪些工作未完成、是否继续派工时调用；"
                            + "结合 state（PENDING/IN_PROGRESS/BLOCKED/DONE 等）识别缺口。")
    public String listProjectTasks() {
        try {
            List<Map<String, Object>> rows = taskService.listByProject(projectId, triggerUserId);
            if (rows == null || rows.isEmpty()) {
                return "EMPTY (no tasks)";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("count=").append(rows.size()).append('\n');
            int n = 0;
            for (Map<String, Object> r : rows) {
                if (n++ >= TASK_LIST_MAX_LINES) {
                    sb.append("...[truncated, show first ")
                            .append(TASK_LIST_MAX_LINES)
                            .append(" tasks]");
                    break;
                }
                sb.append("taskId=")
                        .append(r.get("id"))
                        .append(" state=")
                        .append(r.get("state"))
                        .append(" assignee=")
                        .append(r.get("assigneeAgentId"))
                        .append(" moduleId=")
                        .append(r.get("moduleId"))
                        .append(" title=")
                        .append(r.get("title") != null ? String.valueOf(r.get("title")).replace('\n', ' ') : "")
                        .append('\n');
            }
            return sb.toString().trim();
        } catch (MagBusinessException e) {
            return "ERROR "
                    + e.getResultCode().name()
                    + " "
                    + (e.getMessage() != null ? e.getMessage() : "");
        } catch (Exception e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Tool(
            name = "list_project_modules",
            description =
                    "列出当前项目功能模块（id、name、parentId、tag）。"
                            + "用于对照需求拆解、发现尚未覆盖的模块或派工时的 moduleId 选择。")
    public String listProjectModules() {
        try {
            List<Map<String, Object>> rows = moduleService.list(projectId, triggerUserId);
            if (rows == null || rows.isEmpty()) {
                return "EMPTY (no modules)";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("count=").append(rows.size()).append('\n');
            for (Map<String, Object> r : rows) {
                sb.append("moduleId=")
                        .append(r.get("id"))
                        .append(" parentId=")
                        .append(r.get("parentId"))
                        .append(" name=")
                        .append(r.get("name") != null ? String.valueOf(r.get("name")).replace('\n', ' ') : "")
                        .append(" tag=")
                        .append(r.get("tag") != null ? r.get("tag") : "")
                        .append('\n');
            }
            return sb.toString().trim();
        } catch (MagBusinessException e) {
            return "ERROR "
                    + e.getResultCode().name()
                    + " "
                    + (e.getMessage() != null ? e.getMessage() : "");
        } catch (Exception e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Tool(
            name = "dispatch_task",
            description =
                    "为当前项目创建任务并指派执行 Agent（项目经理派工）。"
                            + "assigneeAgentId 必须来自 list_dispatchable_agents。"
                            + "流水线：需求正文为空时仅可派给 PRODUCT；PRODUCT 有未结项任务时勿再派 PRODUCT；"
                            + "派给 TEST 前须无 FRONTEND/BACKEND 未结项任务。")
    public String dispatchTask(
            @ToolParam(name = "title", description = "任务标题") String title,
            @ToolParam(name = "assigneeAgentId", description = "执行 Agent 的数字 id（本项目）")
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
