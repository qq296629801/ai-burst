package com.aiburst.mag.service;

import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目协调消息与任务沟通气泡：有任务专属线程（项目经理派工创建）则写入该线程，否则写入「需求与派工协调」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagCoordinationChatWriter {

    private static final int INSTRUCTION_MAX = 16_000;
    private static final int CHAT_CONTENT_MAX = 120_000;

    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 解析消息落库目标线程 id：任务线程优先，否则全局协调线程。
     */
    public long resolveThreadIdForCoordOrPmTask(long projectId, Long taskContextTaskId) {
        return resolveThread(projectId, taskContextTaskId).getId();
    }

    /**
     * 将一次 ReAct 流中的 USER/ASSISTANT 多轮落为气泡（纯文本 content，供前端线程室展示）。
     */
    @Transactional
    public void appendAgentScopeChatTurns(long threadId, long speakingAgentId, List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Set<String> seenMsgIds = new HashSet<>();
        for (Event ev : events) {
            Msg msg = ev.getMessage();
            if (msg == null) {
                continue;
            }
            String mid = msg.getId();
            if (mid != null && !mid.isEmpty() && !seenMsgIds.add(mid)) {
                continue;
            }
            MsgRole role = msg.getRole();
            if (role != MsgRole.USER && role != MsgRole.ASSISTANT) {
                continue;
            }
            String text = msg.getTextContent();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            text = trimChatContent(text.trim());
            MagMessage m = new MagMessage();
            m.setThreadId(threadId);
            if (role == MsgRole.USER) {
                m.setSenderType("USER");
                m.setSenderAgentId(null);
            } else {
                m.setSenderType("AGENT");
                m.setSenderAgentId(speakingAgentId);
            }
            m.setContent(text);
            messageMapper.insert(m);
        }
    }

    /**
     * 记录 A2A：调用方 Agent 通过工具触发被调用方 Agent 的嵌套编排。
     *
     * @param taskContextTaskId 根编排关联任务，非空且存在任务线程时写入该线程
     */
    @Transactional
    public void recordA2aInvoke(
            long projectId,
            long callerAgentId,
            long calleeAgentId,
            long triggerUserId,
            String instruction,
            Long taskContextTaskId) {
        MagThread thread = resolveThread(projectId, taskContextTaskId);
        MagMessage m = new MagMessage();
        m.setThreadId(thread.getId());
        m.setSenderType("AGENT");
        m.setSenderAgentId(callerAgentId);
        String instr = instruction != null ? instruction : "";
        if (instr.length() > INSTRUCTION_MAX) {
            instr = instr.substring(0, INSTRUCTION_MAX) + "…[truncated]";
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kind", "A2A_INVOKE");
            payload.put("callerAgentId", callerAgentId);
            payload.put("calleeAgentId", calleeAgentId);
            payload.put("triggerUserId", triggerUserId);
            payload.put("instruction", instr);
            m.setContent(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            m.setContent(
                    "{\"kind\":\"A2A_INVOKE\",\"callerAgentId\":"
                            + callerAgentId
                            + ",\"calleeAgentId\":"
                            + calleeAgentId
                            + "}");
        }
        messageMapper.insert(m);
        log.debug(
                "MAG A2A coord message threadId={} caller={} callee={}",
                thread.getId(),
                callerAgentId,
                calleeAgentId);
    }

    /**
     * 根级编排进入 ReAct：用户/任务触发的一次 Agent 编排开始（非 A2A 嵌套层）。
     */
    @Transactional
    public void recordOrchestrationEnter(
            long projectId, long agentId, long triggerUserId, String instruction, Long taskContextTaskId) {
        MagThread thread = resolveThread(projectId, taskContextTaskId);
        MagMessage m = new MagMessage();
        m.setThreadId(thread.getId());
        m.setSenderType("AGENT");
        m.setSenderAgentId(agentId);
        String instr = instruction != null ? instruction : "";
        if (instr.length() > INSTRUCTION_MAX) {
            instr = instr.substring(0, INSTRUCTION_MAX) + "…[truncated]";
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kind", "ORCH_ENTER");
            payload.put("agentId", agentId);
            payload.put("triggerUserId", triggerUserId);
            payload.put("instruction", instr);
            m.setContent(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            m.setContent(
                    "{\"kind\":\"ORCH_ENTER\",\"agentId\":"
                            + agentId
                            + ",\"triggerUserId\":"
                            + triggerUserId
                            + "}");
        }
        messageMapper.insert(m);
        log.debug("MAG ORCH_ENTER coord message threadId={} agentId={}", thread.getId(), agentId);
    }

    private MagThread resolveThread(long projectId, Long taskId) {
        if (taskId != null) {
            MagThread t = threadMapper.selectLatestByTaskId(taskId);
            if (t != null) {
                return t;
            }
        }
        return ensureCoordThread(projectId);
    }

    private MagThread ensureCoordThread(Long projectId) {
        List<MagThread> threads = threadMapper.selectByProjectId(projectId);
        for (MagThread t : threads) {
            if ("需求与派工协调".equals(t.getTitle())) {
                return t;
            }
        }
        MagThread t = new MagThread();
        t.setProjectId(projectId);
        t.setTitle("需求与派工协调");
        threadMapper.insert(t);
        return threadMapper.selectById(t.getId());
    }

    private static String trimChatContent(String s) {
        if (s.length() <= CHAT_CONTENT_MAX) {
            return s;
        }
        return s.substring(0, CHAT_CONTENT_MAX - 1) + "…";
    }
}
