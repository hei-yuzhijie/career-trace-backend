package com.yuzhijie.career.trace.module.match.service;

import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.match.model.MatchScoreResult;
import com.yuzhijie.career.trace.module.match.vo.GapVO;
import com.yuzhijie.career.trace.module.match.vo.MatchReasonVO;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结构化匹配。
 *
 * <p>按岗位全部标签的权重做归一化，避免标签多的岗位天然占优。
 * 匹配上的标签按 weight × confidence × levelFactor 加分，未覆盖的按 penalty 扣分，
 * 结构化分限制在 0~85；语义可用时总分 = 结构化分 + 语义分（0~15），语义不可用时结构化满分按 100 算。</p>
 *
 * <ul>
 *     <li>匹配项：contribution = structuredMax × weight × confidence × levelFactor / Σweight；</li>
 *     <li>未覆盖的必备项：contribution = -structuredMax × weight × requiredMissPenalty / Σweight；</li>
 *     <li>未覆盖的加分项：contribution = -structuredMax × weight × bonusMissPenalty / Σweight；</li>
 *     <li>levelFactor：等级差 0/1/2/≥3 分别为 1 / 0.5 / 0.2 / 0；</li>
 *     <li>语义分：由 SemanticScoringService 传入，0~15；不可用时结构化满分改为 100。</li>
 * </ul>
 */
@Component
public class MatchEngine {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal ZERO_POINT_TWO = new BigDecimal("0.2");

    private final BigDecimal structuredWeight;
    private final BigDecimal semanticWeight;
    private final BigDecimal requiredMissPenalty;
    private final BigDecimal bonusMissPenalty;

    public MatchEngine(@Value("${career-trace.match.structured-weight:85}") BigDecimal structuredWeight,
                       @Value("${career-trace.match.semantic-weight:15}") BigDecimal semanticWeight,
                       @Value("${career-trace.match.required-miss-penalty:0.50}") BigDecimal requiredMissPenalty,
                       @Value("${career-trace.match.bonus-miss-penalty:0.25}") BigDecimal bonusMissPenalty) {
        this.structuredWeight = structuredWeight == null ? new BigDecimal("85") : structuredWeight;
        this.semanticWeight = semanticWeight == null ? new BigDecimal("15") : semanticWeight;
        this.requiredMissPenalty = requiredMissPenalty == null ? new BigDecimal("0.50") : requiredMissPenalty;
        this.bonusMissPenalty = bonusMissPenalty == null ? new BigDecimal("0.25") : bonusMissPenalty;
    }

    public MatchScoreResult score(List<JobTagRow> jobTags,
                                  Map<Long, MemoryTagRow> userTags,
                                  Map<Long, Integer> skillLevels,
                                  Set<Long> excludedTagIds,
                                  boolean withReasons) {
        return score(jobTags, userTags, skillLevels, excludedTagIds, withReasons, null);
    }

    /**
     * @param semanticPoints 语义分；null 表示语义服务不可用，此时结构化满分按 100 计算
     */
    public MatchScoreResult score(List<JobTagRow> jobTags,
                                  Map<Long, MemoryTagRow> userTags,
                                  Map<Long, Integer> skillLevels,
                                  Set<Long> excludedTagIds,
                                  boolean withReasons,
                                  BigDecimal semanticPoints) {
        MatchScoreResult result = new MatchScoreResult();
        // 语义可用时结构化满分是 85，不可用时改成 100，保证总分上限始终是 100
        boolean semanticAvailable = semanticPoints != null && semanticWeight.signum() > 0;
        BigDecimal structuredMax = semanticAvailable ? structuredWeight : HUNDRED;
        result.setSemanticAvailable(semanticAvailable);
        result.setStructuredWeightUsed(structuredMax.setScale(2, RoundingMode.HALF_UP));

        // 先算岗位所有标签的权重之和，作为归一化分母
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (JobTagRow jobTag : jobTags) {
            BigDecimal weight = jobTag.getWeight() == null ? ONE : jobTag.getWeight();
            if (weight.signum() > 0) {
                totalWeight = totalWeight.add(weight);
            }
        }
        result.setTotalTagWeight(totalWeight.setScale(2, RoundingMode.HALF_UP));
        // 岗位没有任何有效标签时，结构化分为 0，直接返回语义分
        if (totalWeight.signum() == 0) {
            applySemantic(result, semanticPoints, semanticAvailable);
            return result;
        }

        for (JobTagRow jobTag : jobTags) {
            BigDecimal weight = jobTag.getWeight() == null ? ONE : jobTag.getWeight();
            if (weight.signum() == 0) {
                continue;
            }
            String category = jobTag.getTagCategory();
            int requiredLevel = jobTag.getRequiredLevel() == null ? 0 : jobTag.getRequiredLevel();
            // 只有 skill 类且岗位写了 requiredLevel 的标签，才按等级差打折
            boolean skillWithLevel = "skill".equals(category) && requiredLevel > 0;

            // 命中用户排除项：正常流程里这类岗位已被第一段过滤掉，这里兜底扣满分
            if (excludedTagIds.contains(jobTag.getTagId())) {
                BigDecimal contribution = negativeContribution(structuredMax, weight, ONE, totalWeight);
                addNegative(result, jobTag, weight, contribution, requiredLevel, 0, requiredLevel,
                        "命中你的排除项，已从推荐列表过滤", withReasons);
                continue;
            }

            // 用户没有这个标签：按必备/加分走不同扣分比例
            MemoryTagRow userTag = userTags.get(jobTag.getTagId());
            if (userTag == null) {
                BigDecimal penalty = weight.compareTo(ONE) >= 0 ? requiredMissPenalty : bonusMissPenalty;
                BigDecimal contribution = negativeContribution(structuredMax, weight, penalty, totalWeight);
                addNegative(result, jobTag, weight, contribution, requiredLevel, 0, requiredLevel,
                        skillWithLevel ? "缺失该技能证据" : "岗位要求该条件，但你的记忆中未提及",
                        withReasons);
                // 缺的是带等级的 skill，额外记一条 gap 供前端展示学习建议
                if (skillWithLevel && withReasons) {
                    result.getGaps().add(gap(jobTag, 0, requiredLevel));
                }
                continue;
            }

            // 用户有这个标签：算置信度、等级差、levelFactor，再算贡献分
            BigDecimal confidence = userTag.getConfidence() == null ? BigDecimal.ZERO : userTag.getConfidence();
            // skill 类优先用聚合表里的 level，没有则回退到这条记忆上的 proficiency
            int userLevel = skillWithLevel
                    ? skillLevels.getOrDefault(jobTag.getTagId(),
                    userTag.getProficiency() == null ? 0 : userTag.getProficiency())
                    : 0;
            int levelGap = skillWithLevel ? Math.max(0, requiredLevel - userLevel) : 0;
            BigDecimal factor = skillWithLevel ? levelFactor(levelGap) : ONE;
            BigDecimal quality = confidence.multiply(factor);

            BigDecimal contribution = structuredMax.multiply(weight)
                    .multiply(quality)
                    .divide(totalWeight, 4, RoundingMode.HALF_UP);

            // 贡献分为正记入加分项，为 0 记入中性项
            if (contribution.signum() > 0) {
                result.setPositiveSum(result.getPositiveSum().add(contribution));
                result.setMatchedTagCount(result.getMatchedTagCount() + 1);
                if (withReasons) {
                    result.getPositive().add(positiveReason(jobTag, userTag, weight, confidence,
                            skillWithLevel ? requiredLevel : null,
                            skillWithLevel ? userLevel : null,
                            skillWithLevel ? levelGap : null,
                            contribution));
                }
            } else {
                result.setNeutralSum(result.getNeutralSum().add(contribution));
                if (withReasons) {
                    result.getNeutral().add(neutralReason(jobTag, weight, confidence,
                            skillWithLevel ? "技能等级差 " + levelGap + " 级，按规则不计分"
                                    : "用户该标签置信度为 0"));
                }
            }

            // 等级差大于 0 的 skill，记一条 gap
            if (skillWithLevel && levelGap > 0 && withReasons) {
                result.getGaps().add(gap(jobTag, userLevel, levelGap));
            }
        }

        // 结构化分 = 加分 + 扣分，限制在 [0, structuredMax]
        BigDecimal structured = result.getPositiveSum().add(result.getNegativeSum());
        if (structured.signum() < 0) {
            structured = BigDecimal.ZERO;
        }
        if (structured.compareTo(structuredMax) > 0) {
            structured = structuredMax;
        }
        result.setStructuredScore(structured.setScale(2, RoundingMode.HALF_UP));
        result.setPositiveSum(result.getPositiveSum().setScale(2, RoundingMode.HALF_UP));
        result.setNegativeSum(result.getNegativeSum().setScale(2, RoundingMode.HALF_UP));
        result.setNeutralSum(result.getNeutralSum().setScale(2, RoundingMode.HALF_UP));
        applySemantic(result, semanticPoints, semanticAvailable);
        return result;
    }

    /**
     * 把语义分加到结构化分上，总分限制在 [0, 100]。
     * 语义分会被截断到 [0, semanticWeight]，防止外部传入越界值。
     */
    private void applySemantic(MatchScoreResult result, BigDecimal semanticPoints, boolean semanticAvailable) {
        BigDecimal semantic = semanticAvailable
                ? semanticPoints.max(BigDecimal.ZERO).min(semanticWeight)
                : BigDecimal.ZERO;
        result.setSemanticScore(semantic.setScale(2, RoundingMode.HALF_UP));

        BigDecimal score = result.getStructuredScore().add(semantic);
        if (score.signum() < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(HUNDRED) > 0) {
            score = HUNDRED;
        }
        result.setScore(score.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 扣分公式：-(structuredMax × weight × penaltyRate / totalWeight)。
     */
    private static BigDecimal negativeContribution(BigDecimal structuredMax, BigDecimal weight,
                                                   BigDecimal penaltyRate, BigDecimal totalWeight) {
        return structuredMax.multiply(weight).multiply(penaltyRate)
                .divide(totalWeight, 4, RoundingMode.HALF_UP)
                .negate();
    }

    /**
     * 等级差对应的折扣系数：0 级差 1.0，差 1 级 0.5，差 2 级 0.2，差 3 级以上 0。
     */
    public static BigDecimal levelFactor(int levelGap) {
        if (levelGap <= 0) {
            return ONE;
        }
        return switch (levelGap) {
            case 1 -> HALF;
            case 2 -> ZERO_POINT_TWO;
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * 记一条扣分项。withReasons 为 false 时只累加分数，不组装 VO。
     */
    private void addNegative(MatchScoreResult result, JobTagRow jobTag, BigDecimal weight,
                             BigDecimal contribution, int requiredLevel, int userLevel, int levelGap,
                             String reason, boolean withReasons) {
        result.setNegativeSum(result.getNegativeSum().add(contribution));
        if (!withReasons) {
            return;
        }
        MatchReasonVO vo = new MatchReasonVO();
        vo.setTagId(jobTag.getTagId());
        vo.setTagName(jobTag.getTagName());
        vo.setCategory(jobTag.getTagCategory());
        vo.setWeight(weight);
        vo.setReason(reason);
        vo.setContribution(contribution);
        // 只有带等级的 skill 才填 requiredLevel / userLevel / levelGap
        if ("skill".equals(jobTag.getTagCategory()) && requiredLevel > 0) {
            vo.setRequiredLevel(requiredLevel);
            vo.setUserLevel(userLevel);
            vo.setLevelGap(levelGap);
        }
        result.getNegative().add(vo);
    }

    /**
     * 组装加分项的 VO，包含标签信息、等级差、置信度、证据原文和记忆 ID，前端可溯源。
     */
    private MatchReasonVO positiveReason(JobTagRow jobTag, MemoryTagRow userTag, BigDecimal weight,
                                         BigDecimal confidence, Integer requiredLevel, Integer userLevel,
                                         Integer levelGap, BigDecimal contribution) {
        MatchReasonVO vo = new MatchReasonVO();
        vo.setTagId(jobTag.getTagId());
        vo.setTagName(jobTag.getTagName());
        vo.setCategory(jobTag.getTagCategory());
        vo.setWeight(weight);
        vo.setRequiredLevel(requiredLevel);
        vo.setUserLevel(userLevel);
        vo.setLevelGap(levelGap);
        vo.setConfidence(confidence);
        vo.setEvidence(userTag.getEvidence());
        vo.setMemoryId(userTag.getMemoryId());
        vo.setContribution(contribution);
        return vo;
    }

    /**
     * 组装中性项 VO，contribution 固定为 0。
     */
    private MatchReasonVO neutralReason(JobTagRow jobTag, BigDecimal weight, BigDecimal confidence, String reason) {
        MatchReasonVO vo = new MatchReasonVO();
        vo.setTagId(jobTag.getTagId());
        vo.setTagName(jobTag.getTagName());
        vo.setCategory(jobTag.getTagCategory());
        vo.setWeight(weight);
        vo.setConfidence(confidence);
        vo.setReason(reason);
        vo.setContribution(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        return vo;
    }

    /**
     * 组装缺口项 VO，根据用户等级和等级差生成不同的补齐建议。
     */
    private GapVO gap(JobTagRow jobTag, int userLevel, int levelGap) {
        int requiredLevel = jobTag.getRequiredLevel() == null ? 0 : jobTag.getRequiredLevel();
        String suggestion;
        if (userLevel == 0) {
            suggestion = "建议补充「" + jobTag.getTagName() + "」的项目或作品证据，预计 1–2 个月可补齐";
        } else if (levelGap >= 2) {
            suggestion = "「" + jobTag.getTagName() + "」还差 " + levelGap
                    + " 级，建议用 1–2 个真实项目补齐后再投递";
        } else {
            suggestion = "「" + jobTag.getTagName() + "」基本满足要求，可在简历中补充更具体的成果数据";
        }
        return new GapVO(jobTag.getTagId(), jobTag.getTagName(), requiredLevel, userLevel, suggestion);
    }
}