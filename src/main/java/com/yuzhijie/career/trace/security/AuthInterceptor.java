package com.yuzhijie.career.trace.security;

import com.yuzhijie.career.trace.common.exception.BizException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析 Authorization: Bearer &lt;token&gt;，并把 userId 放入请求上下文。
 *
 * <p>测试环境可设置 career-trace.auth.enabled=false 关闭 JWT 校验，
 * 此时所有请求使用 career-trace.auth.dev-user-id 对应的测试用户。</p>
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "careerTraceUserId";

    private final JwtUtil jwtUtil;
    private final boolean authEnabled;
    private final long devUserId;

    public AuthInterceptor(JwtUtil jwtUtil,
                           @Value("${career-trace.auth.enabled:true}") boolean authEnabled,
                           @Value("${career-trace.auth.dev-user-id:1}") long devUserId) {
        this.jwtUtil = jwtUtil;
        this.authEnabled = authEnabled;
        this.devUserId = devUserId;
    }

    @PostConstruct
    public void logAuthMode() {
        if (!authEnabled) {
            log.warn("JWT 鉴权已关闭（测试环境），所有 /api/** 请求使用测试用户 userId={}；"
                    + "上线前请设置 career-trace.auth.enabled=true", devUserId);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
        // CORS 预检请求不带 Authorization，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!authEnabled) {
            UserContext.set(devUserId);
            request.setAttribute(USER_ID_ATTRIBUTE, devUserId);
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        Long userId = jwtUtil.parseUserId(token);
        UserContext.set(userId);
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
