package com.yuzhijie.career.trace.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @Schema(description = "昵称，当前作为登录账号", example = "陈默")
    private String nickname;
    @Schema(description = "密码", example = "123456")
    private String password;
}
