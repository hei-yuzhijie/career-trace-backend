package com.yuzhijie.career.trace.module.auth.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.module.auth.dto.LoginRequest;
import com.yuzhijie.career.trace.module.auth.dto.LogoutRequest;
import com.yuzhijie.career.trace.module.auth.dto.RefreshTokenRequest;
import com.yuzhijie.career.trace.module.auth.service.AuthService;
import com.yuzhijie.career.trace.module.auth.vo.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录认证", description = "双 Token 登录、刷新、登出")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "昵称 + 密码，返回 accessToken 和 refreshToken")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "刷新 Token", description = "用 refreshToken 换新的一对 Token；旧 refreshToken 立即失效")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request == null ? null : request.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "注销当前 accessToken，并可同时注销 refreshToken")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        authService.logout(extractBearer(authorization),
                request == null ? null : request.getRefreshToken());
        return ApiResponse.ok();
    }

    private static String extractBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring("Bearer ".length()).trim();
        }
        return value;
    }
}
