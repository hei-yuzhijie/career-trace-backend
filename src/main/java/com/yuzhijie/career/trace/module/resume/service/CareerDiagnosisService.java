package com.yuzhijie.career.trace.module.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.match.service.RecommendationService;
import com.yuzhijie.career.trace.module.match.vo.ExplainVO;
import com.yuzhijie.career.trace.module.match.vo.GapVO;
import com.yuzhijie.career.trace.module.memory.entity.MemoryItem;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryItemMapper;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import com.yuzhijie.career.trace.module.resume.vo.CareerDiagnosisVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDimensionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 职业经历诊断。基于用户已积累的职业记忆和标签，用规则算出五个维度的分数，再拼出总结、优势、缺口和建议。
 * 指定 jobId 时复用岗位匹配的分数和缺口，不指定时按通用画像评估。
 */
@Service
@RequiredArgsConstructor
public class CareerDiagnosisService {

    // 用来判断一条记忆里是否含量化信息，命中数字、万、千、百分比、QPS、k 都算
    private static final Pattern QUANTIFIED = Pattern.compile(".*(\\d|万|千|%|QPS|qps|k|K).*");

    private final MemoryItemMapper memoryItemMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final JobMapper jobMapper;
    private final RecommendationService recommendationService;

    public CareerDiagnosisVO diagnose(Long userId, Long jobId) {
        // 传了 jobId 就先确认岗位存在，不存在直接报错
        if (jobId != null && jobMapper.selectJobRowById(jobId) == null) {
            throw BizException.notFound(ErrorCodes.JOB_NOT_FOUND, "该岗位已下线");
        }

        // 用户所有正常状态的记忆
        List<MemoryItem> memories = memoryItemMapper.selectList(Wrappers.<MemoryItem>lambdaQuery()
                .eq(MemoryItem::getUserId, userId)
                .eq(MemoryItem::getStatus, 1));

        // 同一标签可能来自多条记忆，按 tagId 去重，避免技能覆盖被重复计算
        Map<Long, MemoryTagRow> uniqueTags = new java.util.LinkedHashMap<>();
        for (MemoryTagRow tag : memoryTagMapper.selectUserTagRows(userId, 1)) {
            uniqueTags.putIfAbsent(tag.getTagId(), tag);
        }
        List<MemoryTagRow> positiveTags = new ArrayList<>(uniqueTags.values());
        // 从正向标签里挑出 skill 类，用于算熟练度和技能覆盖
        List<MemoryTagRow> skillTags = positiveTags.stream()
                .filter(tag -> "skill".equals(tag.getTagCategory()))
                .toList();

        // 以下四个统计量是五维打分的输入
        int memoryCount = memories.size();
        int quantifiedCount = (int) memories.stream()
                .filter(item -> item.getContent() != null && QUANTIFIED.matcher(item.getContent()).matches())
                .count();
        double avgProficiency = skillTags.stream()
                .map(MemoryTagRow::getProficiency)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .average().orElse(0);
        double avgConfidence = positiveTags.stream()
                .map(MemoryTagRow::getConfidence)
                .filter(value -> value != null)
                .mapToDouble(value -> value.doubleValue())
                .average().orElse(0.5);
        long evidenceCount = positiveTags.stream()
                .filter(tag -> tag.getEvidence() != null && !tag.getEvidence().isBlank())
                .count();
        // 证据覆盖率 = 带证据原文的标签数 / 正向标签总数
        double evidenceCoverage = positiveTags.isEmpty() ? 0 : (double) evidenceCount / positiveTags.size();

        // 岗位匹配维度默认 10 分；传了 jobId 就复用岗位匹配接口的分和缺口
        double jobFit = 10;
        List<GapVO> gaps = List.of();
        if (jobId != null) {
            ExplainVO explain = recommendationService.explain(userId, jobId);
            jobFit = explain.getScore() == null ? 0 : explain.getScore().doubleValue() / 100 * 20;
            gaps = explain.getGap() == null ? List.of() : explain.getGap();
        }

        // min(20, 8 + 2 × min(记忆条数, 6))
        int experienceDepth = (int) Math.min(20, 8 + 2 * Math.min(memoryCount, 6));
        // min(20, 4 × 含量化信息的记忆条数)
        int achievement = (int) Math.min(20, 4 * quantifiedCount);
        // min(20, 技能标签数 × 2 + 平均熟练度 × 2)
        int skillCoverage = (int) Math.min(20, skillTags.size() * 2 + avgProficiency * 2);
        // explain.getScore() / 100 × 20
        int jobFitScore = (int) Math.round(Math.min(20, Math.max(0, jobFit)));
        // min(20, 平均置信度 × 12 + 证据覆盖率 × 8)
        int expression = (int) Math.round(Math.min(20, avgConfidence * 12 + evidenceCoverage * 8));
        int overall = Math.min(100, experienceDepth + achievement + skillCoverage + jobFitScore + expression);

        // 组装返回结果，五个维度
        CareerDiagnosisVO vo = new CareerDiagnosisVO();
        vo.setSource("MEMORY");
        vo.setTargetJobId(jobId);
        vo.setOverallScore(overall);
        vo.setDimensions(List.of(
                new ResumeDimensionVO("经历完整度", experienceDepth, 20, "共 " + memoryCount + " 条职业记忆"),
                new ResumeDimensionVO("成果量化", achievement, 20, quantifiedCount + " 条记忆含量化/规模信息"),
                new ResumeDimensionVO("技能覆盖", skillCoverage, 20, skillTags.size() + " 个技能标签"),
                new ResumeDimensionVO("岗位匹配", jobFitScore, 20, jobId == null ? "未指定目标岗位" : "复用岗位匹配分"),
                new ResumeDimensionVO("表达结构", expression, 20, "证据覆盖率 " + Math.round(evidenceCoverage * 100) + "%")
        ));
        vo.setStrengths(strengths(skillTags, quantifiedCount, jobId, jobFitScore));
        vo.setGaps(gaps.stream().map(GapVO::getTagName).toList());
        vo.setSuggestions(suggestions(gaps, memoryCount, skillTags.size()));
        vo.setSummary(summary(memoryCount, skillTags.size(), quantifiedCount, jobId, overall));
        return vo;
    }

    /**
     * 优势列表输出：。有量化成果、有技能证据、岗位匹配分高时各加一条，都没有时给一条兜底。
     */
    private static List<String> strengths(List<MemoryTagRow> skillTags, int quantifiedCount,
                                          Long jobId, int jobFitScore) {
        List<String> strengths = new ArrayList<>();
        if (quantifiedCount > 0) {
            strengths.add("有 " + quantifiedCount + " 条量化成果记忆");
        }
        skillTags.stream().limit(3).forEach(tag -> strengths.add("具备「" + tag.getTagName() + "」技能证据"));
        if (jobId != null && jobFitScore >= 12) {
            strengths.add("与目标岗位匹配度较高");
        }
        if (strengths.isEmpty()) {
            strengths.add("已开始积累职业记忆");
        }
        return strengths;
    }

    /**
     * 改进建议输出。缺口技能最多列 3 条，记忆太少或没有技能标签时额外加提示，都没有时给一条兜底。
     */
    private static List<String> suggestions(List<GapVO> gaps, int memoryCount, int skillCount) {
        List<String> suggestions = new ArrayList<>();
        gaps.stream().limit(3).forEach(gap -> suggestions.add("补充「" + gap.getTagName() + "」的项目或作品证据"));
        if (memoryCount < 3) {
            suggestions.add("至少补充 3 条职业记忆，覆盖项目、技能与偏好");
        }
        if (skillCount == 0) {
            suggestions.add("在记忆中使用具体技能词，便于抽取 skill 标签");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("继续保持记忆更新，补充可量化结果");
        }
        return suggestions;
    }

    /**
     * 输出：把记忆条数、技能数、量化条数和总分串起来。
     */
    private static String summary(int memoryCount, int skillCount, int quantifiedCount,
                                  Long jobId, int overall) {
        String target = jobId == null ? "通用职业画像" : "目标岗位";
        return "当前有 " + memoryCount + " 条职业记忆、" + skillCount + " 个技能标签、"
                + quantifiedCount + " 条量化成果；" + target + "诊断得分 " + overall
                + " 分。建议优先补充缺口技能和项目结果。";
    }
}