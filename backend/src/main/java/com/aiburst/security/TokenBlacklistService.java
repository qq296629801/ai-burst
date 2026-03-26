package com.aiburst.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "auth:token:blacklist:";

    private final StringRedisTemplate redis;

    public void blacklist(String jti, Date tokenExpiration) {
        long ttlMs = tokenExpiration.getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
