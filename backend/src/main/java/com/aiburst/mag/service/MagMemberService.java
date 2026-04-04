package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagMemberAddRequest;
import com.aiburst.mag.entity.MagProjectMember;
import com.aiburst.mag.mapper.MagProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagMemberService {

    private final MagProjectMemberMapper memberMapper;
    private final MagAccessHelper accessHelper;

    public List<Map<String, Object>> list(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        return memberMapper.selectByProjectId(projectId).stream().map(this::toRow).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(MagProjectMember m) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", m.getId());
        row.put("userId", m.getUserId());
        row.put("username", m.getUsername());
        row.put("nickname", m.getNickname());
        row.put("roleInProject", m.getRoleInProject());
        row.put("createdAt", m.getCreatedAt());
        return row;
    }

    @Transactional
    public void add(Long projectId, MagMemberAddRequest req, Long operatorUserId) {
        accessHelper.requireMember(projectId, operatorUserId);
        if (memberMapper.countMember(projectId, req.getUserId()) > 0) {
            throw new MagBusinessException(MagResultCode.MAG_UNKNOWN, "user already in project");
        }
        MagProjectMember m = new MagProjectMember();
        m.setProjectId(projectId);
        m.setUserId(req.getUserId());
        m.setRoleInProject(req.getRoleInProject());
        memberMapper.insert(m);
    }

    @Transactional
    public void remove(Long projectId, Long targetUserId, Long operatorUserId) {
        accessHelper.requireMember(projectId, operatorUserId);
        String role = memberMapper.selectRole(projectId, targetUserId);
        if (role == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        if (MagConstants.ROLE_OWNER.equals(role)) {
            long owners = memberMapper.selectByProjectId(projectId).stream()
                    .filter(x -> MagConstants.ROLE_OWNER.equals(x.getRoleInProject()))
                    .count();
            if (owners <= 1) {
                throw new MagBusinessException(MagResultCode.MAG_UNKNOWN, "cannot remove last owner");
            }
        }
        memberMapper.deleteByProjectAndUser(projectId, targetUserId);
    }
}
