package com.yuzhijie.career.trace.security;

import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.module.user.entity.User;
import com.yuzhijie.career.trace.module.user.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析 Authorization: Bearer &lt;accessToken&gt;，校验 token 类型、签名、过期时间和 userId。
 *
 * <p>测试环境可设置 career-trace.auth.enabled=false 关闭校验，
 * 此时所有请求使用 career-trace.auth.dev-user-id 对应的测试用户。</p>
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "careerTraceUserId";

    private final JwtUtil jwtUtil;
    private final TokenStore tokenStore;
    private final UserMapper userMapper;
    private final boolean authEnabled;
    private final long devUserId;

    public AuthInterceptor(JwtUtil jwtUtil,
                           TokenStore tokenStore,
                           UserMapper userMapper,
                           @Value("${career-trace.auth.enabled:true}") boolean authEnabled,
                           @Value("${career-trace.auth.dev-user-id:1}") long devUserId) {
        this.jwtUtil = jwtUtil;
        this.tokenStore = tokenStore;
        this.userMapper = userMapper;
        this.authEnabled = authEnabled;
        this.devUserId = devUserId;
    }

    @PostConstruct
    public void logAuthMode() {
        if (!authEnabled) {
            log.warn("Token 鉴权已关闭（测试环境），所有 /api/** 请求使用测试用户 userId={}；"
                    + "上线前请设置 career-trace.auth.enabled=true", devUserId);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
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
        if (tokenStore.isRevoked(token)) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
        JwtUtil.TokenInfo info = jwtUtil.parseAccessToken(token);
        User user = userMapper.selectById(info.userId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        UserContext.set(info.userId());
        request.setAttribute(USER_ID_ATTRIBUTE, info.userId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
