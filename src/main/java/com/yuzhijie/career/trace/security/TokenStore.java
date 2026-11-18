package com.yuzhijie.career.trace.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单 Token 黑名单：登出/刷新轮换后把旧 token 放进来。
 * 仅适用于单机演示；生产应使用 Redis 并设置过期时间。
 */
@Component
public class TokenStore {

    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            revoked.add(token.trim());
        }
    }

    public boolean isRevoked(String token) {
        return token != null && revoked.contains(token.trim());
    }
}
