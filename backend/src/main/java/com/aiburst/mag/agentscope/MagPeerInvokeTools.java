package com.aiburst.mag.agentscope;

import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Agent2Agent（同项目）：大模型通过工具调用另一 MagAgent 实例，形成多 Agent 协作链；嵌套深度由 {@link MagAgentScopeRunService} 限制。
 */
@RequiredArgsConstructor
public final class MagPeerInvokeTools {

    private final long projectId;
    private final long triggerUserId;
    private final long callerAgentId;
    private final MagAgentMapper agentMapper;
    private final MagNestedAgentRunner nestedRunner;

    @Tool(
            name = "invoke_peer_agent",
            description =
                    "Agent2Agent：调用同一项目内的另一 MagAgent（peerAgentId）执行子任务。"
                            + "对方须已绑定大模型通道；用于产品/开发/测试/项目经理之间的协作请求。"
                            + "instruction 应清晰说明需要对方产出什么。")
    public String invokePeerAgent(
            @ToolParam(name = "peerAgentId", description = "目标 Agent 的数字 id（须属于当前项目）")
                    long peerAgentId,
            @ToolParam(name = "instruction", description = "交给对方 Agent 的任务说明") String instruction) {
        if (!StringUtils.hasText(instruction)) {
            return "ERROR instruction required";
        }
        MagAgent peer = agentMapper.selectById(peerAgentId);
        if (peer == null || !Objects.equals(peer.getProjectId(), projectId)) {
            return "ERROR peer not in project";
        }
        if (peer.getId().longValue() == callerAgentId) {
            return "ERROR cannot invoke self";
        }
        if (peer.getStatus() != null && peer.getStatus() == 0) {
            return "ERROR peer inactive";
        }
        if (peer.getLlmChannelId() == null) {
            return "ERROR peer missing llm channel";
        }
        MagA2aCallerStack.push(callerAgentId);
        try {
            String out = nestedRunner.run(peer, triggerUserId, instruction.trim());
            if (out == null) {
                return "OK";
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
