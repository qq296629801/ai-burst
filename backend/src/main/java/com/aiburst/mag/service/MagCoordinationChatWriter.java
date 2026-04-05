package com.aiburst.mag.service;

import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向项目「需求与派工协调」线程写入系统协调类消息（如 A2A、根级编排进入）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagCoordinationChatWriter {

    private static final int INSTRUCTION_MAX = 16_000;

    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 记录 A2A：调用方 Agent 通过工具触发被调用方 Agent 的嵌套编排。
     */
    @Transactional
    public void recordA2aInvoke(
            long projectId,
            long callerAgentId,
            long calleeAgentId,
            long triggerUserId,
            String instruction) {
        MagThread coord = ensureCoordThread(projectId);
        MagMessage m = new MagMessage();
        m.setThreadId(coord.getId());
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
                coord.getId(),
                callerAgentId,
                calleeAgentId);
    }

    /**
     * 根级编排进入 ReAct：用户/任务触发的一次 Agent 编排开始（非 A2A 嵌套层）。
     */
    @Transactional
    public void recordOrchestrationEnter(long projectId, long agentId, long triggerUserId, String instruction) {
        MagThread coord = ensureCoordThread(projectId);
        MagMessage m = new MagMessage();
        m.setThreadId(coord.getId());
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
        log.debug("MAG ORCH_ENTER coord message threadId={} agentId={}", coord.getId(), agentId);
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
}
