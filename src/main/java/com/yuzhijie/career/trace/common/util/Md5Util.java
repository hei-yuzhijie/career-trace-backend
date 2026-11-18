package com.yuzhijie.career.trace.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 工具。仅按要求用于演示登录，生产环境请使用 BCrypt/Argon2。
 */
public final class Md5Util {

    private Md5Util() {
    }

    public static String md5(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }
}
