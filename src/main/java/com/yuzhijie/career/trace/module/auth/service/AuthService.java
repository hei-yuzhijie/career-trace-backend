package com.yuzhijie.career.trace.module.auth.service;

import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.util.Md5Util;
import com.yuzhijie.career.trace.module.auth.dto.LoginRequest;
import com.yuzhijie.career.trace.module.auth.vo.AuthResponse;
import com.yuzhijie.career.trace.module.user.entity.User;
import com.yuzhijie.career.trace.module.user.mapper.UserMapper;
import com.yuzhijie.career.trace.security.JwtUtil;
import com.yuzhijie.career.trace.security.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录、刷新、登出。双 Token + MD5 密码 + 内存黑名单，保持简单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenStore tokenStore;

    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getNickname() == null || request.getNickname().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw BizException.badRequest("昵称和密码不能为空");
        }
        User user = userMapper.selectByNickname(request.getNickname().trim());
        if (user == null || user.getPassword() == null
                || !user.getPassword().equalsIgnoreCase(Md5Util.md5(request.getPassword()))) {
            throw BizException.unauthorized("昵称或密码错误");
        }
        return issue(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BizException.unauthorized("refreshToken 不能为空");
        }
        if (tokenStore.isRevoked(refreshToken)) {
            throw BizException.unauthorized("refreshToken 已失效");
        }
        JwtUtil.TokenInfo info = jwtUtil.parseRefreshToken(refreshToken);
        User user = userMapper.selectById(info.userId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        // 简单轮换：旧 refreshToken 立即作废
        tokenStore.revoke(refreshToken);
        return issue(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenStore.revoke(accessToken);
        tokenStore.revoke(refreshToken);
    }

    private AuthResponse issue(User user) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtUtil.generateAccessToken(user.getId()));
        response.setRefreshToken(jwtUtil.generateRefreshToken(user.getId()));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtUtil.getAccessExpireSeconds());
        response.setRefreshExpiresIn(jwtUtil.getRefreshExpireSeconds());
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        return response;
    }
}
