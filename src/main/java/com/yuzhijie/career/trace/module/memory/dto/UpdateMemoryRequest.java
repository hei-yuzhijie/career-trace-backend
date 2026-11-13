package com.yuzhijie.career.trace.module.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新记忆原文请求")
public class UpdateMemoryRequest {

    @Schema(description = "记忆原文，长度 1-2000", example = "想留在杭州，能接受大小周，希望有带教老师",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
