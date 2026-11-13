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
 * 轻量 JWT（HS256）实现，避免为当前模块额外引入 JWT 依赖。
 *
 * <p>Token 载荷：{"sub":"1","uid":1,"iat":...,"exp":...}。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final byte[] secret;
    private final long expireMinutes;

    public JwtUtil(@Value("${career-trace.jwt.secret}") String secret,
                   @Value("${career-trace.jwt.expire-minutes:10080}") long expireMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(Long userId) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expireMinutes * 60;
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("sub", String.valueOf(userId));
        payload.put("uid", userId);
        payload.put("iat", now);
        payload.put("exp", exp);

        String header = base64Url(HEADER.getBytes(StandardCharsets.UTF_8));
        String body = base64Url(MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
        String signature = base64Url(hmac(header + "." + body));
        return header + "." + body + "." + signature;
    }

    /**
     * 校验签名与过期时间，返回用户 ID；失败一律按 401 处理。
     */
    public Long parseUserId(String token) {
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
            long userId = payload.path("uid").asLong(0);
            if (userId <= 0) {
                throw BizException.unauthorized("Token 缺少用户信息");
            }
            return userId;
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
}
