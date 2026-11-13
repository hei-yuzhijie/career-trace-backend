package com.yuzhijie.career.trace.integration.llm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 大模型抽取出的单个标签（未归一化）。
 */
@Data
@Schema(description = "大模型抽取出的原始标签")
public class LlmTag {

    @Schema(description = "标签名", example = "内容策划")
    private String name;

    @Schema(description = "类别：skill/experience/preference/location/seniority", example = "skill")
    private String category;

    @Schema(description = "极性：1 正向，-1 排除", example = "1")
    private Integer polarity;

    @Schema(description = "熟练度 0-4，仅 skill/experience 有意义", example = "3")
    private Integer proficiency;

    @Schema(description = "置信度 0-1", example = "0.92")
    private BigDecimal confidence;

    @Schema(description = "支撑该标签的原文片段", example = "做了 3 篇小红书笔记")
    private String evidence;
}
