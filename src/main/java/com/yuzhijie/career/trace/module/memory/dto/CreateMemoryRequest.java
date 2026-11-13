package com.yuzhijie.career.trace.module.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增记忆请求")
public class CreateMemoryRequest {

    @Schema(description = "记忆原文，长度 1-2000", example = "想留在杭州，能接受大小周，希望有带教",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "来源类型", allowableValues = {"resume", "interview", "manual", "review"},
            example = "manual", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceType;

    @Schema(description = "来源对象 ID，如简历 ID", example = "1001")
    private Long sourceId;
}
