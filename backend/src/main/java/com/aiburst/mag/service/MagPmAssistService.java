package com.aiburst.mag.service;

import com.aiburst.mag.dto.MagPmAssistCreateRequest;
import com.aiburst.mag.entity.MagPmAssistRecord;
import com.aiburst.mag.mapper.MagPmAssistRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagPmAssistService {

    private final MagPmAssistRecordMapper pmAssistRecordMapper;
    private final MagAccessHelper accessHelper;

    public List<Map<String, Object>> list(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return pmAssistRecordMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long projectId, MagPmAssistCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagPmAssistRecord r = new MagPmAssistRecord();
        r.setProjectId(projectId);
        r.setProblemType(req.getProblemType());
        r.setRootCauseSummary(req.getRootCauseSummary());
        r.setActionTaken(req.getActionTaken());
        r.setAssistedAgentIdsJson(req.getAssistedAgentIdsJson());
        r.setResolved(req.getResolved() != null ? req.getResolved() : 0);
        pmAssistRecordMapper.insert(r);
        return toRow(r);
    }

    private Map<String, Object> toRow(MagPmAssistRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("projectId", r.getProjectId());
        m.put("problemType", r.getProblemType());
        m.put("rootCauseSummary", r.getRootCauseSummary());
        m.put("actionTaken", r.getActionTaken());
        m.put("assistedAgentIdsJson", r.getAssistedAgentIdsJson());
        m.put("resolved", r.getResolved());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}
