package com.aiburst.mag.service;

import com.aiburst.dto.PageResult;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.dto.MagProjectCreateRequest;
import com.aiburst.mag.dto.MagProjectUpdateRequest;
import com.aiburst.mag.entity.MagProject;
import com.aiburst.mag.entity.MagProjectMember;
import com.aiburst.mag.entity.MagRequirementDoc;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagProjectMapper;
import com.aiburst.mag.mapper.MagProjectMemberMapper;
import com.aiburst.mag.mapper.MagRequirementDocMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.aiburst.security.SecurityUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagProjectService {

    private final MagProjectMapper projectMapper;
    private final MagProjectMemberMapper memberMapper;
    private final MagRequirementDocMapper requirementDocMapper;
    private final MagAccessHelper accessHelper;
    private final MagAgentMapper agentMapper;
    private final MagThreadMapper threadMapper;

    public PageResult<Map<String, Object>> page(MagPageQuery q) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        PageHelper.startPage(q.getPageNum(), q.getPageSize());
        List<MagProject> list = projectMapper.selectByUserId(userId);
        PageInfo<MagProject> info = new PageInfo<>(list);
        List<Map<String, Object>> rows = list.stream().map(this::toRow).collect(Collectors.toList());
        return new PageResult<>(info.getTotal(), rows);
    }

    private Map<String, Object> toRow(MagProject p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("status", p.getStatus());
        m.put("configJson", p.getConfigJson());
        m.put("currentReqDocId", p.getCurrentReqDocId());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        m.put("agentCount", agentMapper.countByProjectId(p.getId()));
        m.put("lastActivityAt", threadMapper.selectLatestMessageAtByProjectId(p.getId()));
        MagRequirementDoc doc = requirementDocMapper.selectByProjectId(p.getId());
        m.put("currentRequirementVersion", doc != null ? doc.getCurrentVersion() : 0);
        return m;
    }

    @Transactional
    public Map<String, Object> create(MagProjectCreateRequest req) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        MagProject project = new MagProject();
        project.setName(req.getName().trim());
        project.setStatus(1);
        projectMapper.insert(project);

        MagRequirementDoc doc = new MagRequirementDoc();
        doc.setProjectId(project.getId());
        doc.setCurrentVersion(0);
        requirementDocMapper.insert(doc);
        projectMapper.updateCurrentReqDocId(project.getId(), doc.getId());

        MagProjectMember owner = new MagProjectMember();
        owner.setProjectId(project.getId());
        owner.setUserId(userId);
        owner.setRoleInProject(MagConstants.ROLE_OWNER);
        memberMapper.insert(owner);

        return get(project.getId(), userId);
    }

    public Map<String, Object> get(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagProject p = accessHelper.requireProject(projectId);
        return toRow(p);
    }

    @Transactional
    public void update(Long projectId, MagProjectUpdateRequest req) {
        Long userId = SecurityUtils.currentUserId();
        accessHelper.requireMember(projectId, userId);
        MagProject p = accessHelper.requireProject(projectId);
        if (StringUtils.hasText(req.getName())) {
            p.setName(req.getName().trim());
        }
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        }
        projectMapper.update(p);
    }

    /**
     * 软删除：归档项目。
     */
    @Transactional
    public void archive(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        accessHelper.requireMember(projectId, userId);
        MagProject p = accessHelper.requireProject(projectId);
        p.setStatus(0);
        projectMapper.update(p);
    }
}
