package com.yuzhijie.career.trace.security;

import com.yuzhijie.career.trace.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 轻量 JWT（HS256）实现，不引入额外 JWT 依赖。
 *
 * <p>双 Token：access 短有效期，refresh 长有效期；载荷带 type 防止混用。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final byte[] secret;
    private final long accessExpireMinutes;
    private final long refreshExpireMinutes;

    public JwtUtil(@Value("${career-trace.jwt.secret}") String secret,
                   @Value("${career-trace.jwt.access-expire-minutes:30}") long accessExpireMinutes,
                   @Value("${career-trace.jwt.refresh-expire-minutes:10080}") long refreshExpireMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessExpireMinutes = accessExpireMinutes <= 0 ? 30 : accessExpireMinutes;
        this.refreshExpireMinutes = refreshExpireMinutes <= 0 ? 10080 : refreshExpireMinutes;
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, TYPE_ACCESS, accessExpireMinutes);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, TYPE_REFRESH, refreshExpireMinutes);
    }

    /**
     * 兼容旧调用：默认生成 access token。
     */
    public String generateToken(Long userId) {
        return generateAccessToken(userId);
    }

    public TokenInfo parseAccessToken(String token) {
        return parseToken(token, TYPE_ACCESS);
    }

    public TokenInfo parseRefreshToken(String token) {
        return parseToken(token, TYPE_REFRESH);
    }

    /**
     * 兼容旧调用：仅解析 access token 并返回 userId。
     */
    public Long parseUserId(String token) {
        return parseAccessToken(token).userId();
    }

    public long getAccessExpireSeconds() {
        return accessExpireMinutes * 60;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireMinutes * 60;
    }

    private String generateToken(Long userId, String type, long expireMinutes) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expireMinutes * 60;
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("sub", String.valueOf(userId));
        payload.put("uid", userId);
        payload.put("type", type);
        payload.put("iat", now);
        payload.put("exp", exp);

        String header = base64Url(HEADER.getBytes(StandardCharsets.UTF_8));
        String body = base64Url(MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
        String signature = base64Url(hmac(header + "." + body));
        return header + "." + body + "." + signature;
    }

    private TokenInfo parseToken(String token, String expectedType) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw BizException.unauthorized("Token 格式非法");
            }
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw BizException.unauthorized("Token 签名无效");
            }
            JsonNode payload = MAPPER.readTree(new String(Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8));
            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && exp < Instant.now().getEpochSecond()) {
                throw BizException.unauthorized("Token 已过期");
            }
            String type = payload.path("type").asString("");
            if (!expectedType.equals(type)) {
                throw BizException.unauthorized("Token 类型错误");
            }
            long userId = payload.path("uid").asLong(0);
            if (userId <= 0) {
                throw BizException.unauthorized("Token 缺少用户信息");
            }
            return new TokenInfo(userId, type, exp);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.debug("parse token failed", e);
            throw BizException.unauthorized("Token 无效");
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenInfo(Long userId, String type, long expiresAt) {
    }
}
