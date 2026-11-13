package com.yuzhijie.career.trace.module.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "手动修正记忆标签请求")
public class UpdateMemoryTagRequest {

    @Schema(description = "熟练度 0-4，仅 skill / 部分 experience 有效", example = "4")
    private Integer proficiency;

    @Schema(description = "极性：1 正向，-1 排除", allowableValues = {"1", "-1"}, example = "1")
    private Integer polarity;
}
