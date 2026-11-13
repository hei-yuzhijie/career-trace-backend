package com.yuzhijie.career.trace.security;

/**
 * 当前请求用户上下文（由 AuthInterceptor 写入，finally 清理）。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static Long require() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求未认证");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
