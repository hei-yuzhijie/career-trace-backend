package com.yuzhijie.career.trace.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "双 Token 登录响应")
public class AuthResponse {
    @Schema(description = "访问令牌，放到 Authorization: Bearer <accessToken>")
    private String accessToken;
    @Schema(description = "刷新令牌，用于换取新 accessToken")
    private String refreshToken;
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";
    @Schema(description = "accessToken 有效期（秒）", example = "1800")
    private Long expiresIn;
    @Schema(description = "refreshToken 有效期（秒）", example = "604800")
    private Long refreshExpiresIn;
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    @Schema(description = "昵称", example = "陈默")
    private String nickname;
}
