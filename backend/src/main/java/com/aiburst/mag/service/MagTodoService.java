package com.aiburst.mag.service;

import com.aiburst.dto.PageResult;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.entity.MagRequirementPoolItem;
import com.aiburst.mag.mapper.MagRequirementPoolItemMapper;
import com.aiburst.security.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MagTodoService {

    private final MagRequirementPoolItemMapper poolMapper;
    private final MagAccessHelper accessHelper;
    private final PermissionCacheService permissionCacheService;

    public PageResult<Map<String, Object>> page(Long userId, MagPageQuery q) {
        if (userId == null) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        List<String> perms = permissionCacheService.getPermCodes(userId);
        if (!perms.contains("mag:pool:decide")) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        List<MagRequirementPoolItem> raw = poolMapper.selectTodosForUser(userId, MagConstants.POOL_PENDING_USER);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (MagRequirementPoolItem it : raw) {
            String role = accessHelper.memberRole(it.getProjectId(), userId);
            if (accessHelper.canSeePoolItem(it, userId, role, true)) {
                filtered.add(poolRow(it));
            }
        }
        long total = filtered.size();
        int from = Math.max(0, (q.getPageNum() - 1) * q.getPageSize());
        int to = Math.min(from + q.getPageSize(), filtered.size());
        List<Map<String, Object>> page = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        return new PageResult<>(total, page);
    }

    private Map<String, Object> poolRow(MagRequirementPoolItem it) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", it.getId());
        m.put("projectId", it.getProjectId());
        m.put("state", it.getState());
        m.put("revisionId", it.getRevisionId());
        m.put("anchorJson", it.getAnchorJson());
        m.put("payloadJson", it.getPayloadJson());
        m.put("assignedDeciderUserId", it.getAssignedDeciderUserId());
        m.put("createdAt", it.getCreatedAt());
        return m;
    }
}
