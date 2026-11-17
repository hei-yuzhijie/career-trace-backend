package com.yuzhijie.career.trace.module.match.service;

import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.Pages;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.job.mapper.JobTagMapper;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.match.mapper.UserSkillProfileMapper;
import com.yuzhijie.career.trace.module.match.model.MatchScoreResult;
import com.yuzhijie.career.trace.module.match.model.SemanticScore;
import com.yuzhijie.career.trace.module.match.model.SkillProfileRow;
import com.yuzhijie.career.trace.module.match.vo.ExcludedTagVO;
import com.yuzhijie.career.trace.module.match.vo.ExcludedTagsVO;
import com.yuzhijie.career.trace.module.match.vo.ExplainVO;
import com.yuzhijie.career.trace.module.match.vo.FilterSummaryVO;
import com.yuzhijie.career.trace.module.match.vo.LocationTagVO;
import com.yuzhijie.career.trace.module.match.vo.LocationTagsVO;
import com.yuzhijie.career.trace.module.match.vo.RecommendationItemVO;
import com.yuzhijie.career.trace.module.match.vo.RecommendationPageVO;
import com.yuzhijie.career.trace.module.match.vo.ScoreBreakdownVO;
import com.yuzhijie.career.trace.module.match.vo.SemanticMatchVO;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 岗位推荐。先按排除标签和地点过滤岗位，再对候选岗位打分（结构化 + 语义），最后排序分页。
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String SEMANTIC_DEGRADED_MESSAGE = "语义服务不可用，已降级为结构化匹配";

    private final MemoryTagMapper memoryTagMapper;
    private final UserSkillProfileMapper userSkillProfileMapper;
    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final MatchEngine matchEngine;
    private final SemanticScoringService semanticScoringService;

    public RecommendationPageVO recommend(Long userId, Integer page, Integer size, String city,
                                          BigDecimal minScore, boolean explain) {
        int pageNo = Pages.page(page);
        int pageSize = Pages.size(size);
        BigDecimal threshold = minScore == null || minScore.signum() < 0 ? BigDecimal.ZERO : minScore;

        // 用户没有正向标签时直接返回空，后面的过滤和打分都没意义
        Map<Long, MemoryTagRow> positiveTags = loadPositiveTags(userId);
        if (positiveTags.isEmpty()) {
            RecommendationPageVO empty = new RecommendationPageVO();
            empty.setTotal(0L);
            empty.setPage(pageNo);
            empty.setSize(pageSize);
            empty.setFilterSummary(new FilterSummaryVO(List.of(), List.of(), 0));
            empty.setHint("请先补充职业记忆");
            empty.setRecords(List.of());
            return empty;
        }

        // 用户标记为排除的标签，岗位只要命中其中一个就剔除
        Set<Long> excludedTagIds = loadExcludedTagIds(userId);

        // 从正向标签里挑出地点，用户有地点标签时作为硬过滤条件
        Set<Long> locationTagIds = new HashSet<>();
        Set<String> locationNames = new LinkedHashSet<>();
        for (MemoryTagRow tag : positiveTags.values()) {
            if ("location".equals(tag.getTagCategory())) {
                locationTagIds.add(tag.getTagId());
                if (tag.getTagName() != null) {
                    locationNames.add(tag.getTagName());
                }
            }
        }

        // skill 熟练度，用来算用户 level 和岗位 requiredLevel 的差距
        Map<Long, Integer> skillLevels = loadSkillLevels(userId);

        List<JobRow> activeJobs = jobMapper.selectActiveJobRows(trimToNull(city));
        Map<Long, List<JobTagRow>> jobTagsByJob = loadJobTags(activeJobs);

        // 排除标签过滤：岗位带任一排除标签就剔除，剔除的 jobId 记下来用于响应
        List<Long> excludedByPolarity = new ArrayList<>();
        List<JobRow> afterPolarity = new ArrayList<>();
        for (JobRow job : activeJobs) {
            List<JobTagRow> tags = jobTagsByJob.getOrDefault(job.getJobId(), List.of());
            boolean excluded = tags.stream().anyMatch(tag -> excludedTagIds.contains(tag.getTagId()));
            if (excluded) {
                excludedByPolarity.add(job.getJobId());
            } else {
                afterPolarity.add(job);
            }
        }

        // 地点过滤：用户有地点标签时，岗位必须命中其中一个，否则剔除
        List<Long> excludedByLocation = new ArrayList<>();
        List<JobRow> candidates;
        if (locationTagIds.isEmpty()) {
            candidates = afterPolarity;
        } else {
            candidates = new ArrayList<>();
            for (JobRow job : afterPolarity) {
                List<JobTagRow> tags = jobTagsByJob.getOrDefault(job.getJobId(), List.of());
                if (locationHit(job, tags, locationTagIds, locationNames)) {
                    candidates.add(job);
                } else {
                    excludedByLocation.add(job.getJobId());
                }
            }
        }

        // 语义分，外部服务调用，失败时返回空 map，打分时降级为纯结构化
        Map<Long, SemanticScore> semanticScores = semanticScoringService.scoreJobs(
                userId, candidates, jobTagsByJob, positiveTags);

        // 对每个候选岗位打分，低于阈值的丢弃
        List<ScoredJob> scored = new ArrayList<>();
        for (JobRow job : candidates) {
            SemanticScore semantic = semanticScores.get(job.getJobId());
            MatchScoreResult result = matchEngine.score(
                    jobTagsByJob.getOrDefault(job.getJobId(), List.of()),
                    positiveTags, skillLevels, excludedTagIds, explain,
                    semantic == null ? null : semantic.getPoints());
            if (result.getScore().compareTo(threshold) >= 0) {
                scored.add(new ScoredJob(job, result));
            }
        }

        // 按总分降序，相同总分时依次比结构化分、匹配标签数、语义分，最后用 jobId 保证顺序稳定
        scored.sort((left, right) -> {
            int compared = right.result().getScore().compareTo(left.result().getScore());
            if (compared != 0) {
                return compared;
            }
            compared = right.result().getStructuredScore().compareTo(left.result().getStructuredScore());
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(right.result().getMatchedTagCount(),
                    left.result().getMatchedTagCount());
            if (compared != 0) {
                return compared;
            }
            compared = right.result().getSemanticScore().compareTo(left.result().getSemanticScore());
            if (compared != 0) {
                return compared;
            }
            return Long.compare(left.job().getJobId(), right.job().getJobId());
        });

        // 内存分页，取当前页数据组装返回
        List<RecommendationItemVO> records = new ArrayList<>();
        for (ScoredJob item : subList(scored, pageNo, pageSize)) {
            JobRow job = item.job();
            MatchScoreResult result = item.result();
            records.add(new RecommendationItemVO(
                    job.getJobId(),
                    job.getTitle(),
                    job.getCompanyName(),
                    job.getCity(),
                    job.getSalaryMin(),
                    job.getSalaryMax(),
                    result.getScore(),
                    result.getStructuredScore(),
                    result.isSemanticAvailable() ? result.getSemanticScore() : null,
                    result.getMatchedTagCount(),
                    explain ? result.getPositive() : null));
        }

        RecommendationPageVO response = new RecommendationPageVO();
        response.setTotal((long) scored.size());
        response.setPage(pageNo);
        response.setSize(pageSize);
        response.setFilterSummary(new FilterSummaryVO(
                excludedByPolarity, excludedByLocation, candidates.size()));
        response.setRecords(records);
        return response;
    }

    /**
     * 单个岗位的完整归因，返回加分、减分、缺口、中性项，供前端展示
     */
    public ExplainVO explain(Long userId, Long jobId) {
        JobRow job = jobMapper.selectJobRowById(jobId);
        if (job == null) {
            throw BizException.notFound(ErrorCodes.JOB_NOT_FOUND, "该岗位已下线");
        }
        Map<Long, MemoryTagRow> positiveTags = loadPositiveTags(userId);
        Set<Long> excludedTagIds = loadExcludedTagIds(userId);
        Map<Long, Integer> skillLevels = loadSkillLevels(userId);
        List<JobTagRow> jobTags = jobTagMapper.selectByJobId(jobId);

        Map<Long, SemanticScore> semanticScores = semanticScoringService.scoreJobs(
                userId, List.of(job), Map.of(jobId, jobTags), positiveTags);
        SemanticScore semantic = semanticScores.get(jobId);
        MatchScoreResult result = matchEngine.score(
                jobTags, positiveTags, skillLevels, excludedTagIds, true,
                semantic == null ? null : semantic.getPoints());

        ExplainVO response = new ExplainVO();
        response.setJobId(job.getJobId());
        response.setTitle(job.getTitle());
        response.setCompanyName(job.getCompanyName());
        response.setCity(job.getCity());
        response.setScore(result.getScore());
        response.setScoreBreakdown(new ScoreBreakdownVO(
                round2(result.getPositiveSum()),
                round2(result.getNegativeSum()),
                round2(result.getNeutralSum()),
                result.getStructuredScore(),
                result.getSemanticScore(),
                result.getScore()));
        response.setSemantic(semantic == null
                ? new SemanticMatchVO(false, null, null, BigDecimal.ZERO,
                semanticScoringService.getModel(), SEMANTIC_DEGRADED_MESSAGE)
                : new SemanticMatchVO(true, semantic.getSimilarity(), semantic.getNormalized(),
                semantic.getPoints(), semanticScoringService.getModel(), null));
        response.setPositive(result.getPositive());
        response.setNegative(result.getNegative());
        response.setGap(result.getGaps());
        response.setNeutral(result.getNeutral());
        return response;
    }

    /**
     * 用户排除的标签，按 tagId 去重，同一标签在多条记忆里只返回一条。
     */
    public ExcludedTagsVO excludedTags(Long userId) {
        Map<Long, MemoryTagRow> unique = new LinkedHashMap<>();
        for (MemoryTagRow row : memoryTagMapper.selectUserTagRows(userId, -1)) {
            unique.putIfAbsent(row.getTagId(), row);
        }
        List<ExcludedTagVO> tags = unique.values().stream()
                .map(row -> new ExcludedTagVO(row.getTagId(), row.getTagName(), row.getTagSlug(),
                        row.getTagCategory(), row.getEvidence(), row.getMemoryId()))
                .toList();
        return new ExcludedTagsVO(tags);
    }

    /**
     * 用户的地点标签，只取 category=location 且 polarity=1 的。
     */
    public LocationTagsVO locationTags(Long userId) {
        Map<Long, MemoryTagRow> unique = new LinkedHashMap<>();
        for (MemoryTagRow row : memoryTagMapper.selectUserTagRows(userId, 1)) {
            if ("location".equals(row.getTagCategory())) {
                unique.putIfAbsent(row.getTagId(), row);
            }
        }
        List<LocationTagVO> tags = unique.values().stream()
                .map(row -> new LocationTagVO(row.getTagId(), row.getTagName(), row.getTagSlug(),
                        row.getEvidence(), row.getMemoryId()))
                .toList();
        return new LocationTagsVO(tags);
    }

    /**
     * 判断岗位是否命中用户的地点标签。
     * 优先用 job_tags 的 location 标签比对；岗位没有 location 标签时，用 jobs.city 和标签名比对。
     */
    private static boolean locationHit(JobRow job, List<JobTagRow> jobTags,
                                       Set<Long> locationTagIds, Set<String> locationNames) {
        boolean hasLocationTag = jobTags.stream()
                .anyMatch(tag -> "location".equals(tag.getTagCategory()));
        if (hasLocationTag) {
            return jobTags.stream().anyMatch(tag -> locationTagIds.contains(tag.getTagId()));
        }
        return job.getCity() != null && locationNames.contains(job.getCity());
    }

    /**
     * 加载用户的正向标签，按 tagId 去重。
     * SQL 已按优先级排序（skill 按熟练度、其余按置信度），putIfAbsent 保证每个 tagId 只取第一条。
     */
    private Map<Long, MemoryTagRow> loadPositiveTags(Long userId) {
        Map<Long, MemoryTagRow> best = new LinkedHashMap<>();
        for (MemoryTagRow row : memoryTagMapper.selectUserTagRows(userId, 1)) {
            best.putIfAbsent(row.getTagId(), row);
        }
        return best;
    }

    private Set<Long> loadExcludedTagIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (MemoryTagRow row : memoryTagMapper.selectUserTagRows(userId, -1)) {
            ids.add(row.getTagId());
        }
        return ids;
    }

    /**
     * 用户 skill 类标签的聚合熟练度，level 为 null 的跳过。
     */
    private Map<Long, Integer> loadSkillLevels(Long userId) {
        Map<Long, Integer> levels = new HashMap<>();
        for (SkillProfileRow row : userSkillProfileMapper.selectProfileRows(userId)) {
            if (row.getLevel() != null) {
                levels.put(row.getTagId(), row.getLevel());
            }
        }
        return levels;
    }

    /**
     * 批量查岗位标签，按 jobId 分组。
     */
    private Map<Long, List<JobTagRow>> loadJobTags(List<JobRow> jobs) {
        if (jobs.isEmpty()) {
            return Map.of();
        }
        List<Long> jobIds = jobs.stream().map(JobRow::getJobId).toList();
        Map<Long, List<JobTagRow>> grouped = new HashMap<>();
        for (JobTagRow row : jobTagMapper.selectByJobIds(jobIds)) {
            grouped.computeIfAbsent(row.getJobId(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    /**
     * 内存分页。page 从 1 开始，越界返回空列表。
     */
    private static <T> List<T> subList(List<T> all, int page, int size) {
        int from = (page - 1) * size;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + size, all.size()));
    }

    private static BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ScoredJob(JobRow job, MatchScoreResult result) {
    }
}