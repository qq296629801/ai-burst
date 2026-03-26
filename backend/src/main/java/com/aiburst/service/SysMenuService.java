package com.aiburst.service;

import com.aiburst.dto.MenuSaveRequest;
import com.aiburst.dto.MenuVO;
import com.aiburst.entity.SysPermission;
import com.aiburst.mapper.PermissionMapper;
import com.aiburst.mapper.RoleMapper;
import com.aiburst.security.PermissionCacheService;
import com.aiburst.util.MenuTreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;
    private final PermissionCacheService permissionCacheService;

    public List<MenuVO> treeAll() {
        List<SysPermission> all = permissionMapper.selectAllOrderBySort();
        return MenuTreeUtil.build(all);
    }

    @Transactional
    public void save(MenuSaveRequest req) {
        if (permissionMapper.countByCode(req.getPermCode(), req.getId()) > 0) {
            throw new IllegalArgumentException("perm code exists");
        }
        SysPermission p = new SysPermission();
        p.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        p.setPermCode(req.getPermCode());
        p.setPermName(req.getPermName());
        p.setPermType(req.getPermType());
        p.setPath(req.getPath());
        p.setComponent(req.getComponent());
        p.setIcon(req.getIcon());
        p.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        p.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        if (req.getId() == null) {
            permissionMapper.insert(p);
        } else {
            p.setId(req.getId());
            permissionMapper.update(p);
        }
        permissionCacheService.evictAllUsers();
    }

    @Transactional
    public void delete(Long id) {
        if (permissionMapper.countChildren(id) > 0) {
            throw new IllegalArgumentException("has children");
        }
        roleMapper.deletePermissionLinksByPermId(id);
        permissionMapper.deleteById(id);
        permissionCacheService.evictAllUsers();
    }
}
