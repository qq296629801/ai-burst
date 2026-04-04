package com.aiburst.mag.service;

import com.aiburst.mag.MagConstants;
import com.aiburst.mag.dto.MagReleaseCreateRequest;
import com.aiburst.mag.entity.MagKbEntry;
import com.aiburst.mag.entity.MagReleaseArchive;
import com.aiburst.mag.mapper.MagKbEntryMapper;
import com.aiburst.mag.mapper.MagReleaseArchiveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagReleaseService {

    private final MagReleaseArchiveMapper releaseArchiveMapper;
    private final MagKbEntryMapper kbEntryMapper;
    private final MagAccessHelper accessHelper;

    public List<Map<String, Object>> list(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return releaseArchiveMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long projectId, MagReleaseCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagReleaseArchive a = new MagReleaseArchive();
        a.setProjectId(projectId);
        a.setVersionLabel(req.getVersionLabel().trim());
        a.setSnapshotJson(req.getSnapshotJson());
        a.setMinioObjectKey(req.getMinioObjectKey());
        int qf = req.getQualityFlag() != null ? req.getQualityFlag() : 0;
        a.setQualityFlag(qf);
        releaseArchiveMapper.insert(a);
        if (qf == 1) {
            MagKbEntry kb = new MagKbEntry();
            kb.setSource(MagConstants.KB_SOURCE_ARCHIVE_REFLOW);
            kb.setArchiveId(a.getId());
            kb.setTitle("Release " + a.getVersionLabel());
            kb.setBody(req.getSnapshotJson() != null ? req.getSnapshotJson() : "");
            kb.setTagsJson(null);
            kb.setKeywords("release," + projectId);
            kbEntryMapper.insert(kb);
        }
        return toRow(releaseArchiveMapper.selectById(a.getId()));
    }

    private Map<String, Object> toRow(MagReleaseArchive a) {
        Map<String, Object> m = new HashMap<>();
        if (a == null) {
            return m;
        }
        m.put("id", a.getId());
        m.put("projectId", a.getProjectId());
        m.put("versionLabel", a.getVersionLabel());
        m.put("snapshotJson", a.getSnapshotJson());
        m.put("minioObjectKey", a.getMinioObjectKey());
        m.put("qualityFlag", a.getQualityFlag());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }
}
