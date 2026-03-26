package com.aiburst.security;

import com.aiburst.common.constants.AuthRedisKeys;
import com.aiburst.mapper.PermissionMapper;
import com.aiburst.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final long TTL_MINUTES = 30;

    private final StringRedisTemplate redis;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public List<String> getPermCodes(Long userId) {
        String key = AuthRedisKeys.USER_PERMS_PREFIX + userId;
        String cached = redis.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            if (AuthRedisKeys.PERM_CACHE_EMPTY_MARKER.equals(cached)) {
                return Collections.emptyList();
            }
            String[] parts = cached.split(",");
            List<String> list = new java.util.ArrayList<>();
            for (String p : parts) {
                if (StringUtils.hasText(p)) {
                    list.add(p.trim());
                }
            }
            return list;
        }
        List<String> fromDb = permissionMapper.selectPermCodesByUserId(userId);
        if (CollectionUtils.isEmpty(fromDb)) {
            fromDb = Collections.emptyList();
        }
        String toStore = fromDb.isEmpty()
                ? AuthRedisKeys.PERM_CACHE_EMPTY_MARKER
                : fromDb.stream().collect(Collectors.joining(","));
        redis.opsForValue().set(key, toStore, TTL_MINUTES, TimeUnit.MINUTES);
        return fromDb;
    }

    public void evictUser(Long userId) {
        redis.delete(AuthRedisKeys.USER_PERMS_PREFIX + userId);
    }

    public void evictUsersByRoleId(Long roleId) {
        List<Long> userIds = permissionMapper.selectUserIdsByRoleId(roleId);
        for (Long uid : userIds) {
            evictUser(uid);
        }
    }

    public void evictAllUsers() {
        for (Long uid : userMapper.selectAllIds()) {
            evictUser(uid);
        }
    }
}
