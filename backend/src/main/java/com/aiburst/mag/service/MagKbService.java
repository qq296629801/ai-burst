package com.aiburst.mag.service;

import com.aiburst.dto.PageResult;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagKbEntryCreateRequest;
import com.aiburst.mag.dto.MagKbUpdateRequest;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.entity.MagKbEntry;
import com.aiburst.mag.mapper.MagKbEntryMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagKbService {

    private final MagKbEntryMapper kbEntryMapper;

    public PageResult<Map<String, Object>> page(String keyword, MagPageQuery q) {
        PageHelper.startPage(q.getPageNum(), q.getPageSize());
        List<MagKbEntry> list = kbEntryMapper.selectAll(keyword);
        PageInfo<MagKbEntry> info = new PageInfo<>(list);
        List<Map<String, Object>> rows = list.stream().map(this::toRow).collect(Collectors.toList());
        return new PageResult<>(info.getTotal(), rows);
    }

    @Transactional
    public Map<String, Object> create(MagKbEntryCreateRequest req) {
        MagKbEntry e = new MagKbEntry();
        e.setSource(MagConstants.KB_SOURCE_MANUAL);
        e.setArchiveId(null);
        e.setTitle(req.getTitle().trim());
        e.setBody(req.getBody());
        e.setTagsJson(req.getTagsJson());
        e.setKeywords(req.getKeywords());
        kbEntryMapper.insert(e);
        return toRow(kbEntryMapper.selectById(e.getId()));
    }

    public Map<String, Object> getById(Long id) {
        MagKbEntry e = kbEntryMapper.selectById(id);
        if (e == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        return toRow(e);
    }

    @Transactional
    public Map<String, Object> update(Long id, MagKbUpdateRequest req) {
        MagKbEntry e = kbEntryMapper.selectById(id);
        if (e == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        if (MagConstants.KB_SOURCE_ARCHIVE_REFLOW.equals(e.getSource())) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN, "archive reflow entries are read-only");
        }
        e.setTitle(req.getTitle().trim());
        e.setBody(req.getBody());
        e.setTagsJson(req.getTagsJson());
        e.setKeywords(req.getKeywords());
        kbEntryMapper.update(e);
        return toRow(kbEntryMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        MagKbEntry e = kbEntryMapper.selectById(id);
        if (e == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        if (MagConstants.KB_SOURCE_ARCHIVE_REFLOW.equals(e.getSource())) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN, "archive reflow entries cannot be deleted");
        }
        kbEntryMapper.deleteById(id);
    }

    private Map<String, Object> toRow(MagKbEntry e) {
        Map<String, Object> m = new HashMap<>();
        if (e == null) {
            return m;
        }
        m.put("id", e.getId());
        m.put("source", e.getSource());
        m.put("archiveId", e.getArchiveId());
        m.put("title", e.getTitle());
        m.put("body", e.getBody());
        m.put("tagsJson", e.getTagsJson());
        m.put("keywords", e.getKeywords());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }
}
