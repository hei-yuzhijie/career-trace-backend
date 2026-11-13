package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "语义匹配信息")
public class SemanticMatchVO {

    @Schema(description = "语义服务是否可用；false 表示已降级为纯结构化匹配", example = "true")
    private Boolean available;

    @Schema(description = "用户画像与岗位文本的余弦相似度", example = "0.7341")
    private BigDecimal similarity;

    @Schema(description = "归一化后的语义匹配度 0-1", example = "0.6137")
    private BigDecimal normalized;

    @Schema(description = "语义分（0-15）", example = "9.21")
    private BigDecimal score;

    @Schema(description = "向量模型", example = "text-embedding-v3")
    private String model;

    @Schema(description = "降级说明", example = "语义服务不可用，已降级为结构化匹配")
    private String message;
}
