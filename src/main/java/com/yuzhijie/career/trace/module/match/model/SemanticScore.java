package com.yuzhijie.career.trace.module.match.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单个岗位的语义匹配结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticScore {

    /** 余弦相似度，-1 ~ 1 */
    private BigDecimal similarity;

    /** 归一化到 0 ~ 1 的语义匹配度 */
    private BigDecimal normalized;

    /** 折算后的语义分，0 ~ semanticWeight */
    private BigDecimal points;
}
