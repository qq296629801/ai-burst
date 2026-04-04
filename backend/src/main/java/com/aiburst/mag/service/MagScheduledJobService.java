package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagScheduledJobUpsertRequest;
import com.aiburst.mag.entity.MagScheduledJobConfig;
import com.aiburst.mag.mapper.MagScheduledJobConfigMapper;
import com.aiburst.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagScheduledJobService {

    private final MagScheduledJobConfigMapper jobConfigMapper;

    public List<Map<String, Object>> list(Long projectId) {
        List<MagScheduledJobConfig> rows;
        if (projectId != null) {
            rows = jobConfigMapper.selectByProjectId(projectId);
        } else {
            rows = jobConfigMapper.selectAll();
        }
        return rows.stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> upsert(MagScheduledJobUpsertRequest req) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        if (req.getId() != null) {
            MagScheduledJobConfig existing = jobConfigMapper.selectById(req.getId());
            if (existing == null) {
                throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
            }
            existing.setJobKey(req.getJobKey());
            existing.setCronExpr(req.getCronExpr());
            existing.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
            existing.setProjectId(req.getProjectId());
            jobConfigMapper.update(existing);
            return toRow(jobConfigMapper.selectById(req.getId()));
        }
        MagScheduledJobConfig dup = jobConfigMapper.selectByJobKey(req.getJobKey());
        if (dup != null) {
            dup.setCronExpr(req.getCronExpr());
            dup.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
            dup.setProjectId(req.getProjectId());
            jobConfigMapper.update(dup);
            return toRow(jobConfigMapper.selectById(dup.getId()));
        }
        MagScheduledJobConfig n = new MagScheduledJobConfig();
        n.setJobKey(req.getJobKey());
        n.setCronExpr(req.getCronExpr());
        n.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
        n.setProjectId(req.getProjectId());
        jobConfigMapper.insert(n);
        return toRow(jobConfigMapper.selectByJobKey(req.getJobKey()));
    }

    private Map<String, Object> toRow(MagScheduledJobConfig c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("jobKey", c.getJobKey());
        m.put("cronExpr", c.getCronExpr());
        m.put("enabled", c.getEnabled());
        m.put("projectId", c.getProjectId());
        m.put("lastRunAt", c.getLastRunAt());
        return m;
    }
}
