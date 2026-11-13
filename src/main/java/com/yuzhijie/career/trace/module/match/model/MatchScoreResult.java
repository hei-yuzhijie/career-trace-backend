package com.yuzhijie.career.trace.module.match.model;

import com.yuzhijie.career.trace.module.match.vo.GapVO;
import com.yuzhijie.career.trace.module.match.vo.MatchReasonVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 单个岗位的匹配计算结果。
 *
 * <p>score = structuredScore + semanticScore，最终截断到 0-100。</p>
 */
@Data
public class MatchScoreResult {

    /** 最终得分，0-100 */
    private BigDecimal score = BigDecimal.ZERO;
    /** 结构化加分合计 */
    private BigDecimal positiveSum = BigDecimal.ZERO;
    /** 结构化扣分合计（负数） */
    private BigDecimal negativeSum = BigDecimal.ZERO;
    /** 中性项合计，通常为 0 */
    private BigDecimal neutralSum = BigDecimal.ZERO;
    /** 结构化分，截断到 0 ~ structuredWeightUsed */
    private BigDecimal structuredScore = BigDecimal.ZERO;
    /** 语义分，0 ~ semanticWeight */
    private BigDecimal semanticScore = BigDecimal.ZERO;
    /** 本次使用的结构化满分（语义可用 85，不可用 100） */
    private BigDecimal structuredWeightUsed = BigDecimal.ZERO;
    /** 语义分是否可用 */
    private boolean semanticAvailable;
    /** 岗位全部标签权重之和，便于解释归一化 */
    private BigDecimal totalTagWeight = BigDecimal.ZERO;

    private int matchedTagCount;
    private List<MatchReasonVO> positive = new ArrayList<>();
    private List<MatchReasonVO> negative = new ArrayList<>();
    private List<MatchReasonVO> neutral = new ArrayList<>();
    private List<GapVO> gaps = new ArrayList<>();
}
