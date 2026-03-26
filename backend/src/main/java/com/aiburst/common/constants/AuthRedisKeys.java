package com.aiburst.common.constants;

/**
 * 认证相关 Redis Key 前缀（全小写，与《开发规范》一致，禁止在业务中散落拼字符串）。
 */
public final class AuthRedisKeys {

    private AuthRedisKeys() {
    }

    /** auth:token:blacklist:{jti} */
    public static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    /** auth:user:perms:{userId} */
    public static final String USER_PERMS_PREFIX = "auth:user:perms:";

    /** 权限列表缓存空占位，避免与「无缓存」混淆 */
    public static final String PERM_CACHE_EMPTY_MARKER = "__EMPTY__";
}
