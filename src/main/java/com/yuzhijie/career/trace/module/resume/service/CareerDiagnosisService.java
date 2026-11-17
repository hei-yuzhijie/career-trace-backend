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
 * 职业经历诊断：基于 memory 库 + 可选岗位匹配，规则计算五维分，模板生成总结。
 */
@Service
@RequiredArgsConstructor
public class CareerDiagnosisService {

    private static final Pattern QUANTIFIED = Pattern.compile(".*(\\d|万|千|%|QPS|qps|k|K).*");

    private final MemoryItemMapper memoryItemMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final JobMapper jobMapper;
    private final RecommendationService recommendationService;

    public CareerDiagnosisVO diagnose(Long userId, Long jobId) {
        if (jobId != null && jobMapper.selectJobRowById(jobId) == null) {
            throw BizException.notFound(ErrorCodes.JOB_NOT_FOUND, "该岗位已下线");
        }
        List<MemoryItem> memories = memoryItemMapper.selectList(Wrappers.<MemoryItem>lambdaQuery()
                .eq(MemoryItem::getUserId, userId)
                .eq(MemoryItem::getStatus, 1));
        // 同一标签可能来自多条记忆，诊断按标签去重，避免技能覆盖被重复放大
        Map<Long, MemoryTagRow> uniqueTags = new java.util.LinkedHashMap<>();
        for (MemoryTagRow tag : memoryTagMapper.selectUserTagRows(userId, 1)) {
            uniqueTags.putIfAbsent(tag.getTagId(), tag);
        }
        List<MemoryTagRow> positiveTags = new ArrayList<>(uniqueTags.values());
        List<MemoryTagRow> skillTags = positiveTags.stream()
                .filter(tag -> "skill".equals(tag.getTagCategory()))
                .toList();

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
        double evidenceCoverage = positiveTags.isEmpty() ? 0 : (double) evidenceCount / positiveTags.size();

        double jobFit = 10;
        List<GapVO> gaps = List.of();
        if (jobId != null) {
            ExplainVO explain = recommendationService.explain(userId, jobId);
            jobFit = explain.getScore() == null ? 0 : explain.getScore().doubleValue() / 100 * 20;
            gaps = explain.getGap() == null ? List.of() : explain.getGap();
        }

        int experienceDepth = (int) Math.min(20, 8 + 2 * Math.min(memoryCount, 6));
        int achievement = (int) Math.min(20, 4 * quantifiedCount);
        int skillCoverage = (int) Math.min(20, skillTags.size() * 2 + avgProficiency * 2);
        int jobFitScore = (int) Math.round(Math.min(20, Math.max(0, jobFit)));
        int expression = (int) Math.round(Math.min(20, avgConfidence * 12 + evidenceCoverage * 8));
        int overall = Math.min(100, experienceDepth + achievement + skillCoverage + jobFitScore + expression);

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

    private static String summary(int memoryCount, int skillCount, int quantifiedCount,
                                  Long jobId, int overall) {
        String target = jobId == null ? "通用职业画像" : "目标岗位";
        return "当前有 " + memoryCount + " 条职业记忆、" + skillCount + " 个技能标签、"
                + quantifiedCount + " 条量化成果；" + target + "诊断得分 " + overall
                + " 分。建议优先补充缺口技能和项目结果。";
    }
}
