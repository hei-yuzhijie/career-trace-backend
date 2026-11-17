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
 * 语义分计算。用户画像和岗位文本分别向量化，算余弦相似度，归一化后映射到 0~semanticWeight 分。
 * 向量缓存在进程内，按内容 hash 判断是否需要重算；embedding 调用失败后进入退避期，期间直接降级。
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
    // 退避截止时间戳，当前时间小于它时直接返回空 map
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
     * 计算候选岗位的语义分。
     * 返回的 map 只包含成功拿到向量的岗位；任何岗位缺向量时整批返回空，调用方据此降级为纯结构化匹配。
     */
    public Map<Long, SemanticScore> scoreJobs(Long userId,
                                              List<JobRow> jobs,
                                              Map<Long, List<JobTagRow>> jobTagsByJob,
                                              Map<Long, MemoryTagRow> positiveTags) {
        if (!isAvailable() || jobs == null || jobs.isEmpty()) {
            return Map.of();
        }
        // 用户没有正向标签就拼不出画像文本，语义分没有意义
        String profileText = buildProfileText(positiveTags);
        if (profileText.isBlank()) {
            return Map.of();
        }

        long now = System.currentTimeMillis();
        if (now < disabledUntil) {
            return Map.of();
        }

        // 用户画像 hash 变了才需要重新向量化
        String profileHash = Hashes.sha256(profileText);
        CachedVector userVector = userCache.get(userId);
        boolean needUser = userVector == null || !profileHash.equals(userVector.hash());

        // 收集本次需要向量化的文本：用户画像（若需重算）+ 新增或变更的岗位文本
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
            // 调用失败后进入退避期，避免后续请求继续等待超时
            disabledUntil = System.currentTimeMillis() + failureBackoffSeconds * 1000;
            log.warn("Embedding 调用失败，{} 秒内降级为纯结构化匹配: {}",
                    failureBackoffSeconds, e.getMessage());
            return Map.of();
        }

        if (userVector == null) {
            return Map.of();
        }

        // 逐岗位算语义分：余弦相似度 → 归一化到 0~1 → 乘权重得到最终分
        Map<Long, SemanticScore> result = new LinkedHashMap<>();
        for (JobRow job : jobs) {
            CachedVector jobVector = jobCache.get(job.getJobId());
            // 这个岗位没有向量（理论上不会发生，前面已经保证要么全有要么全降级），跳过
            if (jobVector == null) {
                continue;
            }

            // 用户向量和岗位向量算余弦相似度，范围 [-1, 1]，值越大越相似
            double similarity = cosine(userVector.vector(), jobVector.vector());

            // 转成 BigDecimal 并保留 4 位小数，避免后续计算的浮点误差
            BigDecimal similarityValue = BigDecimal.valueOf(similarity)
                    .setScale(4, RoundingMode.HALF_UP);

            // 把相似度从 [semanticFloor, semanticCeiling] 线性映射到 [0, 1]
            // 低于 floor 记 0，高于 ceiling 记 1，落在区间内按比例换算
            BigDecimal normalized = normalize(similarityValue);

            // 归一化值乘权重（默认 15），得到该岗位的语义分，保留 2 位小数
            BigDecimal points = normalized.multiply(semanticWeight)
                    .setScale(2, RoundingMode.HALF_UP);

            // 三个值都保留：similarity 给前端展示，normalized 用于排查，points 用于总分
            result.put(job.getJobId(), new SemanticScore(similarityValue, normalized, points));
        }
        // 只要有一个岗位没向量，整批不返回，保证同一批岗位的打分口径一致
        if (result.size() != jobs.size()) {
            log.warn("部分岗位缺少语义向量，本次整体降级为纯结构化匹配");
            return Map.of();
        }
        return result;
    }

    /**
     * 写岗位向量缓存，超过上限时整体清空。
     */
    private void putJobCache(Long jobId, String hash, float[] vector) {
        if (jobCache.size() >= MAX_CACHE_SIZE) {
            jobCache.clear();
        }
        jobCache.put(jobId, new CachedVector(hash, vector));
    }

    /**
     * 把余弦相似度从 [floor, ceiling] 线性映射到 [0, 1]。
     * 低于 floor 记 0，高于 ceiling 记 1；floor 和 ceiling 用来压缩无效区间，提高区分度。
     */
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

    /**
     * 拼用户画像文本：按 category 排序后逐个拼接，skill 和 experience 额外带上熟练度。
     * 示例："求职者画像：地点：杭州；偏好：大小周；技能：内容策划（熟练），证据：…；"
     */
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

    /**
     * 拼岗位文本：标题、城市、公司、JD 原文，末尾附上岗位标签名。
     */
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

    /**
     * 余弦相似度。两个向量长度不一致或任一为零向量时返回 0。
     */
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