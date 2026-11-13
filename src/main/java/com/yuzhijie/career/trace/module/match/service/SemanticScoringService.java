package com.yuzhijie.career.trace.module.match.service;

import com.yuzhijie.career.trace.common.util.Hashes;
import com.yuzhijie.career.trace.integration.llm.EmbeddingClient;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.match.model.SemanticScore;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐算法的语义分：用户画像文本 / 岗位文本向量化 → 余弦相似度 → 归一化 → 0~semanticWeight。
 *
 * <p>缓存只放进程内，key 为内容 hash；不新增表。Embedding 调用失败时在
 * failure-backoff-seconds 内直接降级为结构化匹配，避免每个请求都等待超时。</p>
 */
@Slf4j
@Service
public class SemanticScoringService {

    private static final int MAX_CACHE_SIZE = 1000;

    private final EmbeddingClient embeddingClient;
    private final BigDecimal semanticWeight;
    private final BigDecimal semanticFloor;
    private final BigDecimal semanticCeiling;
    private final long failureBackoffSeconds;

    private final Map<Long, CachedVector> userCache = new ConcurrentHashMap<>();
    private final Map<Long, CachedVector> jobCache = new ConcurrentHashMap<>();
    private volatile long disabledUntil = 0L;

    public SemanticScoringService(EmbeddingClient embeddingClient,
                                  @Value("${career-trace.match.semantic-weight:15}") BigDecimal semanticWeight,
                                  @Value("${career-trace.match.semantic-floor:0.55}") BigDecimal semanticFloor,
                                  @Value("${career-trace.match.semantic-ceiling:0.85}") BigDecimal semanticCeiling,
                                  @Value("${career-trace.embedding.failure-backoff-seconds:60}")
                                  long failureBackoffSeconds) {
        this.embeddingClient = embeddingClient;
        this.semanticWeight = semanticWeight == null ? new BigDecimal("15") : semanticWeight;
        this.semanticFloor = semanticFloor == null ? new BigDecimal("0.55") : semanticFloor;
        this.semanticCeiling = semanticCeiling == null ? new BigDecimal("0.85") : semanticCeiling;
        this.failureBackoffSeconds = Math.max(1, failureBackoffSeconds);
    }

    public boolean isAvailable() {
        return embeddingClient.isAvailable() && System.currentTimeMillis() >= disabledUntil;
    }

    public String getModel() {
        return embeddingClient.getModel();
    }

    /**
     * 批量计算候选岗位的语义分；返回的 map 不包含语义不可用的岗位。
     */
    public Map<Long, SemanticScore> scoreJobs(Long userId,
                                              List<JobRow> jobs,
                                              Map<Long, List<JobTagRow>> jobTagsByJob,
                                              Map<Long, MemoryTagRow> positiveTags) {
        if (!isAvailable() || jobs == null || jobs.isEmpty()) {
            return Map.of();
        }
        String profileText = buildProfileText(positiveTags);
        if (profileText.isBlank()) {
            return Map.of();
        }

        long now = System.currentTimeMillis();
        if (now < disabledUntil) {
            return Map.of();
        }

        String profileHash = Hashes.sha256(profileText);
        CachedVector userVector = userCache.get(userId);
        boolean needUser = userVector == null || !profileHash.equals(userVector.hash());

        List<String> inputs = new ArrayList<>();
        int userInputIndex = -1;
        if (needUser) {
            userInputIndex = inputs.size();
            inputs.add(profileText);
        }

        Map<Long, String> jobHashes = new HashMap<>();
        Map<Long, Integer> jobInputIndexes = new HashMap<>();
        for (JobRow job : jobs) {
            String jobText = buildJobText(job, jobTagsByJob.getOrDefault(job.getJobId(), List.of()));
            String hash = Hashes.sha256(jobText);
            CachedVector cached = jobCache.get(job.getJobId());
            if (cached == null || !hash.equals(cached.hash())) {
                jobHashes.put(job.getJobId(), hash);
                jobInputIndexes.put(job.getJobId(), inputs.size());
                inputs.add(jobText);
            }
        }

        try {
            List<float[]> vectors = inputs.isEmpty() ? List.of() : embeddingClient.embed(inputs);
            if (needUser) {
                userVector = new CachedVector(profileHash, vectors.get(userInputIndex));
                userCache.put(userId, userVector);
            }
            for (JobRow job : jobs) {
                Integer inputIndex = jobInputIndexes.get(job.getJobId());
                if (inputIndex != null) {
                    putJobCache(job.getJobId(), jobHashes.get(job.getJobId()), vectors.get(inputIndex));
                }
            }
        } catch (Exception e) {
            disabledUntil = System.currentTimeMillis() + failureBackoffSeconds * 1000;
            log.warn("Embedding 调用失败，{} 秒内降级为纯结构化匹配: {}",
                    failureBackoffSeconds, e.getMessage());
            return Map.of();
        }

        if (userVector == null) {
            return Map.of();
        }

        Map<Long, SemanticScore> result = new LinkedHashMap<>();
        for (JobRow job : jobs) {
            CachedVector jobVector = jobCache.get(job.getJobId());
            if (jobVector == null) {
                continue;
            }
            double similarity = cosine(userVector.vector(), jobVector.vector());
            BigDecimal similarityValue = BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);
            BigDecimal normalized = normalize(similarityValue);
            BigDecimal points = normalized.multiply(semanticWeight).setScale(2, RoundingMode.HALF_UP);
            result.put(job.getJobId(), new SemanticScore(similarityValue, normalized, points));
        }
        if (result.size() != jobs.size()) {
            log.warn("部分岗位缺少语义向量，本次整体降级为纯结构化匹配");
            return Map.of();
        }
        return result;
    }

    private void putJobCache(Long jobId, String hash, float[] vector) {
        if (jobCache.size() >= MAX_CACHE_SIZE) {
            jobCache.clear();
        }
        jobCache.put(jobId, new CachedVector(hash, vector));
    }

    private BigDecimal normalize(BigDecimal similarity) {
        BigDecimal range = semanticCeiling.subtract(semanticFloor);
        if (range.signum() <= 0) {
            return similarity.compareTo(semanticFloor) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        BigDecimal value = similarity.subtract(semanticFloor)
                .divide(range, 6, RoundingMode.HALF_UP);
        if (value.signum() < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String buildProfileText(Map<Long, MemoryTagRow> positiveTags) {
        if (positiveTags == null || positiveTags.isEmpty()) {
            return "";
        }
        List<MemoryTagRow> tags = new ArrayList<>(positiveTags.values());
        tags.sort(Comparator.comparing(MemoryTagRow::getTagCategory)
                .thenComparing(MemoryTagRow::getTagId));
        StringBuilder text = new StringBuilder("求职者画像：");
        for (MemoryTagRow tag : tags) {
            text.append(categoryLabel(tag.getTagCategory())).append("：")
                    .append(tag.getTagName());
            if ("skill".equals(tag.getTagCategory()) || "experience".equals(tag.getTagCategory())) {
                text.append("（").append(levelLabel(tag.getProficiency())).append("）");
            }
            if (tag.getEvidence() != null && !tag.getEvidence().isBlank()) {
                text.append("，证据：").append(tag.getEvidence());
            }
            text.append("；");
        }
        return text.toString();
    }

    private static String buildJobText(JobRow job, List<JobTagRow> jobTags) {
        StringBuilder text = new StringBuilder();
        text.append("岗位：").append(nullSafe(job.getTitle()))
                .append("；城市：").append(nullSafe(job.getCity()))
                .append("；公司：").append(nullSafe(job.getCompanyName()))
                .append("；JD：").append(nullSafe(job.getJdText()));
        if (!jobTags.isEmpty()) {
            text.append("；岗位标签：");
            for (JobTagRow tag : jobTags) {
                text.append(tag.getTagName()).append("、");
            }
            text.setLength(text.length() - 1);
        }
        return text.toString();
    }

    private static String categoryLabel(String category) {
        if (category == null) {
            return "其他";
        }
        return switch (category) {
            case "skill" -> "技能";
            case "experience" -> "经历";
            case "preference" -> "偏好";
            case "location" -> "地点";
            case "seniority" -> "学历年限";
            default -> category;
        };
    }

    private static String levelLabel(Integer level) {
        if (level == null) {
            return "未评估";
        }
        return switch (level) {
            case 1 -> "了解";
            case 2 -> "会用";
            case 3 -> "熟练";
            case 4 -> "精通";
            default -> "未评估";
        };
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length) {
            return 0.0;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record CachedVector(String hash, float[] vector) {
    }
}
