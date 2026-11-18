package com.yuzhijie.career.trace.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登出请求")
public class LogoutRequest {
    @Schema(description = "可选：同时注销 refreshToken")
    private String refreshToken;
}
