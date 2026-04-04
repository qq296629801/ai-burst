package com.aiburst.mag.service;

import com.aiburst.mag.entity.MagExternalFetchAudit;
import com.aiburst.mag.mapper.MagExternalFetchAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagFetchAuditService {

    private final MagExternalFetchAuditMapper fetchAuditMapper;
    private final MagAccessHelper accessHelper;

    public List<Map<String, Object>> listByProject(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return fetchAuditMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(MagExternalFetchAudit a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("projectId", a.getProjectId());
        m.put("userId", a.getUserId());
        m.put("normalizedUrl", a.getNormalizedUrl());
        m.put("httpStatus", a.getHttpStatus());
        m.put("bodyHash", a.getBodyHash());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }
}
