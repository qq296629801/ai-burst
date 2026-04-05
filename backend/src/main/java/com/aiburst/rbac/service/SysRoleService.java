package com.aiburst.rbac.service;

import com.aiburst.mapper.UserMapper;
import com.aiburst.rbac.dto.RoleSaveRequest;
import com.aiburst.rbac.entity.SysRole;
import com.aiburst.rbac.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final PermissionCacheService permissionCacheService;

    public List<Map<String, Object>> listAll() {
        return roleMapper.selectAll().stream().map(this::toRow).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(SysRole r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("roleCode", r.getRoleCode());
        m.put("roleName", r.getRoleName());
        m.put("remark", r.getRemark());
        m.put("permissionIds", roleMapper.selectPermissionIdsByRoleId(r.getId()));
        return m;
    }

    @Transactional
    public void save(RoleSaveRequest req) {
        if (roleMapper.countByCode(req.getRoleCode(), req.getId()) > 0) {
            throw new IllegalArgumentException("role code exists");
        }
        SysRole r = new SysRole();
        r.setRoleCode(req.getRoleCode());
        r.setRoleName(req.getRoleName());
        r.setRemark(req.getRemark());
        Long roleId;
        if (req.getId() == null) {
            roleMapper.insert(r);
            roleId = r.getId();
        } else {
            r.setId(req.getId());
            roleMapper.update(r);
            roleId = req.getId();
        }
        roleMapper.deleteRolePermissions(roleId);
        if (req.getPermissionIds() != null) {
            for (Long pid : req.getPermissionIds()) {
                if (pid != null) {
                    roleMapper.insertRolePermission(roleId, pid);
                }
            }
        }
        permissionCacheService.evictUsersByRoleId(roleId);
    }

    @Transactional
    public void delete(Long id) {
        if (id != null && id == 1L) {
            throw new IllegalArgumentException("cannot delete built-in admin role");
        }
        permissionCacheService.evictUsersByRoleId(id);
        userMapper.deleteUsersByRoleId(id);
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteById(id);
    }
}
