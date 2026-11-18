package com.yuzhijie.career.trace.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "刷新 Token 请求")
public class RefreshTokenRequest {
    @Schema(description = "登录/刷新返回的 refreshToken")
    private String refreshToken;
}
