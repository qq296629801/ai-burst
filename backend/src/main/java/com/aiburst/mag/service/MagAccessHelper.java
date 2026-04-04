package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagProject;
import com.aiburst.mag.entity.MagRequirementPoolItem;
import com.aiburst.mag.mapper.MagProjectMapper;
import com.aiburst.mag.mapper.MagProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MagAccessHelper {

    private final MagProjectMapper projectMapper;
    private final MagProjectMemberMapper memberMapper;

    public MagProject requireProject(Long projectId) {
        MagProject p = projectMapper.selectById(projectId);
        if (p == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        return p;
    }

    public void requireMember(Long projectId, Long userId) {
        if (userId == null) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        if (memberMapper.countMember(projectId, userId) == 0) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_PROJECT_MEMBER);
        }
    }

    public String memberRole(Long projectId, Long userId) {
        requireMember(projectId, userId);
        return memberMapper.selectRole(projectId, userId);
    }

    public boolean isOwner(Long projectId, Long userId) {
        return MagConstants.ROLE_OWNER.equals(memberMapper.selectRole(projectId, userId));
    }

    /**
     * §16.2：具备 mag:pool:decide 前提下，OWNER 或 未指派/指派给自己 可见。
     */
    public boolean canSeePoolItem(MagRequirementPoolItem item, Long userId, String role, boolean hasPoolDecidePerm) {
        if (!hasPoolDecidePerm) {
            return false;
        }
        if (MagConstants.ROLE_OWNER.equals(role)) {
            return true;
        }
        if (item.getAssignedDeciderUserId() == null) {
            return true;
        }
        return item.getAssignedDeciderUserId().equals(userId);
    }
}
