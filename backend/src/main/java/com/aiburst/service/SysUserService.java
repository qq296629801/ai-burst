package com.aiburst.service;

import com.aiburst.dto.PageResult;
import com.aiburst.dto.UserPageQuery;
import com.aiburst.dto.UserSaveRequest;
import com.aiburst.entity.SysUser;
import com.aiburst.mapper.UserMapper;
import com.aiburst.security.PermissionCacheService;
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
public class SysUserService {

    private final UserMapper userMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final PermissionCacheService permissionCacheService;

    public PageResult<Map<String, Object>> page(UserPageQuery q) {
        PageHelper.startPage(q.getPageNum(), q.getPageSize());
        List<SysUser> list = userMapper.selectList(q.getUsername(), q.getStatus());
        PageInfo<SysUser> info = new PageInfo<>(list);
        List<Map<String, Object>> rows = list.stream().map(this::toRow).collect(Collectors.toList());
        return new PageResult<>(info.getTotal(), rows);
    }

    private Map<String, Object> toRow(SysUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("status", u.getStatus());
        m.put("roleIds", userMapper.selectRoleIdsByUserId(u.getId()));
        return m;
    }

    @Transactional
    public void save(UserSaveRequest req) {
        if (userMapper.countByUsername(req.getUsername(), req.getId()) > 0) {
            throw new IllegalArgumentException("username exists");
        }
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setNickname(req.getNickname());
        u.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        if (req.getId() == null) {
            if (!StringUtils.hasText(req.getPassword())) {
                throw new IllegalArgumentException("password required");
            }
            u.setPassword(passwordEncoder.encode(req.getPassword()));
            userMapper.insert(u);
            bindRoles(u.getId(), req.getRoleIds());
            return;
        }
        u.setId(req.getId());
        if (StringUtils.hasText(req.getPassword())) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        } else {
            u.setPassword(null);
        }
        userMapper.update(u);
        userMapper.deleteUserRoles(req.getId());
        bindRoles(req.getId(), req.getRoleIds());
        permissionCacheService.evictUser(req.getId());
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long rid : roleIds) {
            if (rid != null) {
                userMapper.insertUserRole(userId, rid);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new IllegalArgumentException("cannot delete self");
        }
        userMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
        permissionCacheService.evictUser(id);
    }

    public void resetPassword(Long id, String rawPassword) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.update(u);
        permissionCacheService.evictUser(id);
    }
}
