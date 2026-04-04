package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.config.MagTemporalTriggerService;
import com.aiburst.mag.dto.MagMessagePostRequest;
import com.aiburst.mag.dto.MagThreadCreateRequest;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagCollaborationService {

    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final MagAccessHelper accessHelper;
    private final MagTemporalTriggerService temporalTriggerService;

    public List<Map<String, Object>> listThreads(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return threadMapper.selectByProjectId(projectId).stream().map(this::threadRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createThread(Long projectId, MagThreadCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagThread t = new MagThread();
        t.setProjectId(projectId);
        t.setTaskId(req.getTaskId());
        t.setTitle(req.getTitle());
        threadMapper.insert(t);
        return threadRow(threadMapper.selectById(t.getId()));
    }

    public List<Map<String, Object>> listMessages(Long threadId, Long userId) {
        MagThread th = threadMapper.selectById(threadId);
        if (th == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(th.getProjectId(), userId);
        return messageMapper.selectByThreadId(threadId).stream().map(this::msgRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> postMessage(Long threadId, MagMessagePostRequest req, Long userId) {
        MagThread th = threadMapper.selectById(threadId);
        if (th == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(th.getProjectId(), userId);
        MagMessage m = new MagMessage();
        m.setThreadId(threadId);
        m.setSenderType(req.getSenderType());
        m.setSenderAgentId(req.getSenderAgentId());
        m.setContent(req.getContent());
        messageMapper.insert(m);
        return msgRow(m);
    }

    /**
     * 触发线程编排：先做 Temporal 启用与连通性说明；Workflow/Worker 接入前不会真正启动编排。
     */
    public Map<String, Object> requestThreadRun(Long threadId, Long userId) {
        MagThread th = threadMapper.selectById(threadId);
        if (th == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(th.getProjectId(), userId);
        return temporalTriggerService.triggerThreadRun(threadId, userId, "threadId=" + threadId);
    }

    private Map<String, Object> threadRow(MagThread t) {
        Map<String, Object> m = new HashMap<>();
        if (t == null) {
            return m;
        }
        m.put("id", t.getId());
        m.put("projectId", t.getProjectId());
        m.put("taskId", t.getTaskId());
        m.put("title", t.getTitle());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private Map<String, Object> msgRow(MagMessage x) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", x.getId());
        m.put("threadId", x.getThreadId());
        m.put("senderType", x.getSenderType());
        m.put("senderAgentId", x.getSenderAgentId());
        m.put("content", x.getContent());
        m.put("createdAt", x.getCreatedAt());
        return m;
    }
}
