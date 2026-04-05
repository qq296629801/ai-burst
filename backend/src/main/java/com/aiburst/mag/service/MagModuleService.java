package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagImportBlueprintRequest;
import com.aiburst.mag.dto.MagModuleUpsertRequest;
import com.aiburst.mag.entity.MagKbEntry;
import com.aiburst.mag.entity.MagModule;
import com.aiburst.mag.mapper.MagKbEntryMapper;
import com.aiburst.mag.mapper.MagModuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagModuleService {

    private final MagModuleMapper moduleMapper;
    private final MagAccessHelper accessHelper;
    private final MagKbEntryMapper kbEntryMapper;

    public List<Map<String, Object>> list(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return moduleMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long projectId, MagModuleUpsertRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagModule m = new MagModule();
        m.setProjectId(projectId);
        m.setParentId(req.getParentId() != null ? req.getParentId() : 0L);
        m.setName(req.getName().trim());
        m.setTag(req.getTag());
        m.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        moduleMapper.insert(m);
        return toRow(moduleMapper.selectById(m.getId()));
    }

    @Transactional
    public Map<String, Object> update(Long moduleId, MagModuleUpsertRequest req, Long userId) {
        MagModule existing = moduleMapper.selectById(moduleId);
        if (existing == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(existing.getProjectId(), userId);
        if (req.getParentId() != null) {
            existing.setParentId(req.getParentId());
        }
        existing.setName(req.getName().trim());
        existing.setTag(req.getTag());
        if (req.getSortOrder() != null) {
            existing.setSortOrder(req.getSortOrder());
        }
        moduleMapper.update(existing);
        return toRow(moduleMapper.selectById(moduleId));
    }

    @Transactional
    public void delete(Long moduleId, Long userId) {
        MagModule existing = moduleMapper.selectById(moduleId);
        if (existing == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(existing.getProjectId(), userId);
        moduleMapper.deleteById(moduleId);
    }

    @Transactional
    public List<Map<String, Object>> importBlueprint(Long projectId, MagImportBlueprintRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        List<Map<String, Object>> created = new ArrayList<>();
        if ("KB".equalsIgnoreCase(req.getSourceType())) {
            MagKbEntry kb = kbEntryMapper.selectById(req.getSourceId());
            if (kb == null) {
                throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
            }
            MagModule m = new MagModule();
            m.setProjectId(projectId);
            m.setParentId(0L);
            m.setName("KB:" + kb.getTitle());
            m.setTag("from_kb_" + kb.getId());
            m.setSortOrder(0);
            moduleMapper.insert(m);
            created.add(toRow(moduleMapper.selectById(m.getId())));
            return created;
        }
        throw new MagBusinessException(MagResultCode.MAG_UNKNOWN, "sourceType must be KB");
    }

    private Map<String, Object> toRow(MagModule m) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", m.getId());
        row.put("projectId", m.getProjectId());
        row.put("parentId", m.getParentId());
        row.put("name", m.getName());
        row.put("tag", m.getTag());
        row.put("sortOrder", m.getSortOrder());
        return row;
    }
}
