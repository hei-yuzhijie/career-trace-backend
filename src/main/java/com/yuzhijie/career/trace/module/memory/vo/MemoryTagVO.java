package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "记忆标签")
public class MemoryTagVO {

    @Schema(description = "标签 ID", example = "101")
    private Long tagId;

    @Schema(description = "标签名", example = "杭州")
    private String name;

    @Schema(description = "标签唯一标识", example = "hangzhou")
    private String slug;

    @Schema(description = "类别：skill/experience/preference/location/seniority", example = "location")
    private String category;

    @Schema(description = "极性：1 正向，-1 排除", example = "1")
    private Integer polarity;

    @Schema(description = "熟练度 0-4", example = "0")
    private Integer proficiency;

    @Schema(description = "置信度 0-1", example = "0.98")
    private java.math.BigDecimal confidence;

    @Schema(description = "支撑该标签的原文片段", example = "想留在杭州")
    private String evidence;

    @Schema(description = "来源：llm/manual/review", example = "llm")
    private String assessedBy;
}
