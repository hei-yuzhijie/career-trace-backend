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
 * 新版结构化匹配核心。
 *
 * <p>核心思想：按岗位全部标签的权重做归一化，避免“标签多的岗位天然高分”；
 * 找到匹配项按权重加分，未覆盖项按权重扣分，最终结构化分限制在 0~85（语义可用时）。</p>
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
        boolean semanticAvailable = semanticPoints != null && semanticWeight.signum() > 0;
        BigDecimal structuredMax = semanticAvailable ? structuredWeight : HUNDRED;
        result.setSemanticAvailable(semanticAvailable);
        result.setStructuredWeightUsed(structuredMax.setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (JobTagRow jobTag : jobTags) {
            BigDecimal weight = jobTag.getWeight() == null ? ONE : jobTag.getWeight();
            if (weight.signum() > 0) {
                totalWeight = totalWeight.add(weight);
            }
        }
        result.setTotalTagWeight(totalWeight.setScale(2, RoundingMode.HALF_UP));
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
            boolean skillWithLevel = "skill".equals(category) && requiredLevel > 0;

            if (excludedTagIds.contains(jobTag.getTagId())) {
                BigDecimal contribution = negativeContribution(structuredMax, weight, ONE, totalWeight);
                addNegative(result, jobTag, weight, contribution, requiredLevel, 0, requiredLevel,
                        "命中你的排除项，已从推荐列表过滤", withReasons);
                continue;
            }

            MemoryTagRow userTag = userTags.get(jobTag.getTagId());
            if (userTag == null) {
                BigDecimal penalty = weight.compareTo(ONE) >= 0 ? requiredMissPenalty : bonusMissPenalty;
                BigDecimal contribution = negativeContribution(structuredMax, weight, penalty, totalWeight);
                addNegative(result, jobTag, weight, contribution, requiredLevel, 0, requiredLevel,
                        skillWithLevel ? "缺失该技能证据" : "岗位要求该条件，但你的记忆中未提及",
                        withReasons);
                if (skillWithLevel && withReasons) {
                    result.getGaps().add(gap(jobTag, 0, requiredLevel));
                }
                continue;
            }

            BigDecimal confidence = userTag.getConfidence() == null ? BigDecimal.ZERO : userTag.getConfidence();
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

            if (skillWithLevel && levelGap > 0 && withReasons) {
                result.getGaps().add(gap(jobTag, userLevel, levelGap));
            }
        }

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

    private static BigDecimal negativeContribution(BigDecimal structuredMax, BigDecimal weight,
                                                   BigDecimal penaltyRate, BigDecimal totalWeight) {
        return structuredMax.multiply(weight).multiply(penaltyRate)
                .divide(totalWeight, 4, RoundingMode.HALF_UP)
                .negate();
    }

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
        if ("skill".equals(jobTag.getTagCategory()) && requiredLevel > 0) {
            vo.setRequiredLevel(requiredLevel);
            vo.setUserLevel(userLevel);
            vo.setLevelGap(levelGap);
        }
        result.getNegative().add(vo);
    }

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
