package com.aiburst.mag.agentscope;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.service.MagTaskDispatchGateService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 本子线「主 Agent」（{@code parent_agent_id} 为空）向项目经理 Agent 请求派工或调整优先级：
 * 解析项目内 PM 实例并嵌套触发其编排（与 {@link MagPeerInvokeTools} 一致走 A2A 栈与协调留痕）。
 */
@RequiredArgsConstructor
public final class MagMainAgentPmRequestTools {

    private final long projectId;
    private final long triggerUserId;
    private final long callerAgentId;
    private final MagAgentMapper agentMapper;
    private final MagNestedAgentRunner nestedRunner;
    private final MagTaskDispatchGateService taskDispatchGateService;

    @Tool(
            name = "mag_ask_pm_for_next_tasks",
            description =
                    "本子线主 Agent 专用：当子线任务已饱和、无活可分，或需要项目经理补充/调整派工与优先级时调用。"
                            + "将触发项目中项目经理（PM）Agent 的一轮编排，由其使用派工工具处理。"
                            + "situationSummary 须说明当前进度、缺口或请求（勿为空）。"
                            + "门禁：需求正文未就绪时仅 PRODUCT 主 Agent 可调；TEST 主 Agent 须待 FRONTEND/BACKEND 任务均已结项。")
    public String askPmForNextTasks(
            @ToolParam(
                            name = "situationSummary",
                            description = "本子线当前情况与向项目经理提出的派工/调整诉求（自然语言）")
                    String situationSummary) {
        if (!StringUtils.hasText(situationSummary) || situationSummary.trim().length() < 4) {
            return "ERROR situationSummary required (at least a few characters)";
        }
        MagAgent self = agentMapper.selectById(callerAgentId);
        if (self == null || !Objects.equals(self.getProjectId(), projectId)) {
            return "ERROR caller agent not in project";
        }
        if (self.getParentAgentId() != null) {
            return "ERROR only main-line agents (no parent_agent_id) may call mag_ask_pm_for_next_tasks; use invoke_peer_agent to reach your lead agent";
        }
        if ("PM".equals(self.getRoleType())) {
            return "ERROR PM agent should use list_dispatchable_agents / dispatch_task directly";
        }
        try {
            taskDispatchGateService.checkMainAgentMayRequestPmDispatch(projectId, callerAgentId);
        } catch (MagBusinessException e) {
            return "ERROR "
                    + e.getResultCode().name()
                    + " "
                    + (e.getMessage() != null ? e.getMessage() : "");
        }
        List<MagAgent> all = agentMapper.selectByProjectId(projectId);
        if (all == null || all.isEmpty()) {
            return "ERROR no agents in project";
        }
        MagAgent pm =
                all.stream()
                        .filter(a -> "PM".equals(a.getRoleType()))
                        .filter(a -> a.getStatus() == null || a.getStatus() != 0)
                        .filter(a -> a.getLlmChannelId() != null)
                        .min(Comparator.comparing(MagAgent::getId))
                        .orElse(null);
        if (pm == null) {
            return "ERROR no active PM agent with llm channel in this project";
        }
        if (pm.getId().longValue() == callerAgentId) {
            return "ERROR cannot target self as PM";
        }
        String role = self.getRoleType() != null ? self.getRoleType() : "";
        String name = StringUtils.hasText(self.getName()) ? self.getName().trim() : ("agent-" + callerAgentId);
        String instr =
                "【主/职能线 Agent 请求派工】\n"
                        + "请求方 agentId="
                        + callerAgentId
                        + " 角色="
                        + role
                        + " 名称="
                        + name
                        + "\n"
                        + "说明与诉求：\n"
                        + situationSummary.trim()
                        + "\n\n"
                        + "请你作为项目经理：先 list_project_tasks、list_project_modules 分析进度与未完项，"
                        + "再结合 list_dispatchable_agents，按需 dispatch_task 补充派工或说明当前等待执行方推进；"
                        + "并简短回复已采取的动作或等待点。";

        MagA2aCallerStack.push(callerAgentId);
        try {
            String out = nestedRunner.run(pm, triggerUserId, instr);
            if (out == null) {
                return "OK (PM orchestration returned empty)";
            }
            return out.length() > 12_000 ? out.substring(0, 12_000) + "\n...[truncated]" : out;
        } catch (Exception e) {
            return "ERROR "
                    + e.getClass().getSimpleName()
                    + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
        } finally {
            MagA2aCallerStack.pop();
        }
    }
}
