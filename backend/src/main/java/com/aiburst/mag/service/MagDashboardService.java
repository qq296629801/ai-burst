package com.aiburst.mag.service;

import com.aiburst.mag.mapper.MagTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MagDashboardService {

    private final MagTaskMapper taskMapper;
    private final MagAccessHelper accessHelper;

    public Map<String, Object> snapshot(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        List<Map<String, Object>> rows = taskMapper.countByState(projectId);
        Map<String, Long> byState = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object st = row.get("state");
            Object cnt = row.get("cnt");
            if (st != null && cnt instanceof Number) {
                byState.put(String.valueOf(st), ((Number) cnt).longValue());
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("projectId", projectId);
        out.put("taskCountByState", byState);
        return out;
    }
}
