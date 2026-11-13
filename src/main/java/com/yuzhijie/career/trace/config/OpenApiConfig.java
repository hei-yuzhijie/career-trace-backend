package com.yuzhijie.career.trace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置。
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private final boolean authEnabled;

    public OpenApiConfig(@Value("${career-trace.auth.enabled:true}") boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    @Bean
    public OpenAPI careerTraceOpenApi() {
        String description = "职业记忆 → 大模型抽取标签 → 岗位两段式匹配"
                + (authEnabled ? "（需要 Bearer Token）" : "（测试环境：JWT 鉴权已关闭）");

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("职迹 CareerTrace · 记忆库与岗位匹配 API")
                        .description(description)
                        .version("v0.1"));

        if (authEnabled) {
            openAPI.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                            new SecurityScheme()
                                    .name(SECURITY_SCHEME_NAME)
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("在 Knife4j 右上角 Authorize 中填入登录返回的 token（不需要手写 Bearer 前缀）")))
                    .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        }
        return openAPI;
    }
}
