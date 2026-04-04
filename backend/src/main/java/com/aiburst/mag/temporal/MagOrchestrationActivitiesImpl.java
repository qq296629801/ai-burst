package com.aiburst.mag.temporal;

import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * MAG 编排 Activity：可在此接入 LangChain4j / 任务状态机等；当前先落日志便于验证 Worker 已执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagOrchestrationActivitiesImpl implements MagOrchestrationActivities {

    private final MagAgentMapper agentMapper;
    private final MagThreadMapper threadMapper;

    @Override
    public String executeAgentRun(long agentId, long triggerUserId) {
        MagAgent a = agentMapper.selectById(agentId);
        if (a == null) {
            throw new IllegalArgumentException("agent not found: " + agentId);
        }
        log.info(
                "MAG Temporal Activity executeAgentRun agentId={} projectId={} triggerUserId={}",
                agentId,
                a.getProjectId(),
                triggerUserId);
        return "OK";
    }

    @Override
    public String executeThreadRun(long threadId, long triggerUserId) {
        MagThread t = threadMapper.selectById(threadId);
        if (t == null) {
            throw new IllegalArgumentException("thread not found: " + threadId);
        }
        log.info(
                "MAG Temporal Activity executeThreadRun threadId={} projectId={} triggerUserId={}",
                threadId,
                t.getProjectId(),
                triggerUserId);
        return "OK";
    }
}
