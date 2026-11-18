package com.yuzhijie.career.trace.module.memory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.integration.llm.DeepSeekClient;
import com.yuzhijie.career.trace.integration.llm.LlmException;
import com.yuzhijie.career.trace.integration.llm.model.LlmTag;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.memory.entity.MemoryItem;
import com.yuzhijie.career.trace.module.memory.entity.MemoryTag;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryItemMapper;
import com.yuzhijie.career.trace.module.memory.support.TaggingStatusStore;
import com.yuzhijie.career.trace.module.tag.service.TagNormalizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步打标：调用大模型 → 归一化 → 覆盖 memory_tags → 触发 skill 画像重算。
 * 失败只记录 FAILED 状态，不抛出到请求线程。
 */
@Slf4j
@Service
public class MemoryTaggingService {

    public static final int ESTIMATED_SECONDS = 8;
    private static final int MAX_EVIDENCE_LENGTH = 500;
    private static final int MAX_NAME_LENGTH = 100;

    private final MemoryItemMapper memoryItemMapper;
    private final DeepSeekClient deepSeekClient;
    private final TagNormalizationService tagNormalizationService;
    private final MemoryTagWriter memoryTagWriter;
    private final SkillProfileService skillProfileService;
    private final TaggingStatusStore taggingStatusStore;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public MemoryTaggingService(MemoryItemMapper memoryItemMapper,
                                DeepSeekClient deepSeekClient,
                                TagNormalizationService tagNormalizationService,
                                MemoryTagWriter memoryTagWriter,
                                SkillProfileService skillProfileService,
                                TaggingStatusStore taggingStatusStore,
                                @Value("${career-trace.llm.tag-max-attempts:3}") int maxAttempts,
                                @Value("${career-trace.llm.tag-retry-backoff-ms:800}") long retryBackoffMs) {
        this.memoryItemMapper = memoryItemMapper;
        this.deepSeekClient = deepSeekClient;
        this.tagNormalizationService = tagNormalizationService;
        this.memoryTagWriter = memoryTagWriter;
        this.skillProfileService = skillProfileService;
        this.taggingStatusStore = taggingStatusStore;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    @Async("taskExecutor")
    public void tagAsync(Long memoryId) {
        try {
            MemoryItem memory = memoryItemMapper.selectById(memoryId);
            if (memory == null || !Integer.valueOf(1).equals(memory.getStatus())) {
                taggingStatusStore.markFailed(memoryId, "记忆不存在或已删除");
                return;
            }

            List<LlmTag> rawTags = extractTagsWithRetry(memoryId, memory.getContent());
            if (rawTags == null) {
                return;
            }
            // 加载标签索引，把大模型输出的标签名归一化到已有的 tagId
            TagNormalizationService.TagIndex index = tagNormalizationService.loadIndex();
            List<MemoryTag> rows = resolveTags(index, memoryId, rawTags);

            memoryTagWriter.replaceTags(memoryId, rows);
            // updatedAt 在打标完成后变化，这里同步记录“最近一次分析时间”
            memoryItemMapper.update(null, Wrappers.<MemoryItem>lambdaUpdate()
                    .eq(MemoryItem::getId, memoryId)
                    .set(MemoryItem::getUpdatedAt, LocalDateTime.now()));
            taggingStatusStore.markDone(memoryId);
            log.info("打标完成 memoryId={}, tagCount={}", memoryId, rows.size());

            // skill 标签可能新增/删除，统一触发异步重算（幂等）
            skillProfileService.recomputeAsync(memory.getUserId());
        } catch (Exception e) {
            taggingStatusStore.markFailed(memoryId, e.getMessage());
            log.warn("打标失败 memoryId={}, error={}", memoryId, e.getMessage(), e);
        }
    }

    /**
     * 大模型调用最多尝试 maxAttempts 次（默认 3 次，含首次）；仅对可重试错误退避重试。
     *
     * @return 成功时返回标签列表；重试耗尽或不可重试时返回 null（状态已标记 FAILED）
     */
    private List<LlmTag> extractTagsWithRetry(Long memoryId, String content) {
        LlmException lastError = null;
        int attempts = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            attempts = attempt;
            try {
                return deepSeekClient.extractTags(content);
            } catch (LlmException e) {
                lastError = e;
                if (!e.isRetryable() || attempt >= maxAttempts) {
                    break;
                }
                long sleepMs = retryBackoffMs * attempt;
                log.warn("打标第 {}/{} 次失败，{}ms 后重试 memoryId={}: {}",
                        attempt, maxAttempts, sleepMs, memoryId, e.getMessage());
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    taggingStatusStore.markFailed(memoryId, "打标重试被中断");
                    return null;
                }
            }
        }
        String message = lastError == null ? "大模型调用失败" : lastError.getMessage();
        String detail = lastError != null && !lastError.isRetryable()
                ? "打标失败（不可重试）：" + message
                : "打标失败（" + attempts + " 次尝试）：" + message;
        taggingStatusStore.markFailed(memoryId, detail);
        log.warn("打标失败 memoryId={}, attempts={}, error={}", memoryId, attempts, message);
        return null;
    }

    /**
     * 清洗并归一化大模型返回的标签，按 tagId 合并成待落库的 MemoryTag 列表。
     * 同一标签重复出现时：熟练度取高、置信度取高（同时保留该条的 evidence）、极性取排除。
     */
    private List<MemoryTag> resolveTags(TagNormalizationService.TagIndex index, Long memoryId,
                                        List<LlmTag> rawTags) {
        Map<Long, MemoryTag> merged = new LinkedHashMap<>();
        for (LlmTag raw : rawTags) {
            LlmTag tag = sanitize(raw);
            if (tag == null) {
                continue;
            }
            // 归一化：命中别名表就用已有 tagId，没命中则按规则新建
            Long tagId = tagNormalizationService.resolveOrCreate(
                    index, tag.getName(), tag.getCategory(), tag.getPolarity());
            if (tagId == null) {
                continue;
            }

            MemoryTag row = merged.get(tagId);
            if (row == null) {
                row = new MemoryTag();
                row.setMemoryId(memoryId);
                row.setTagId(tagId);
                row.setPolarity(tag.getPolarity());
                row.setProficiency(tag.getProficiency());
                row.setConfidence(tag.getConfidence());
                row.setEvidence(tag.getEvidence());
                row.setAssessedBy("llm");
                merged.put(tagId, row);
            } else {
                // 同一标签重复出现：熟练度取高、置信度取高、极性取排除
                if (tag.getProficiency() > row.getProficiency()) {
                    row.setProficiency(tag.getProficiency());
                }
                if (tag.getConfidence().compareTo(row.getConfidence()) > 0) {
                    row.setConfidence(tag.getConfidence());
                    row.setEvidence(tag.getEvidence());
                }
                if (tag.getPolarity() == -1) {
                    row.setPolarity(-1);
                }
            }
        }
        return new ArrayList<>(merged.values());
    }


    /**
     * 单条标签清洗：过滤空名、过滤 gap 类、截断超长字段
     * 返回 null 表示这条标签不可用，调用方直接跳过。
     */
    private LlmTag sanitize(LlmTag raw) {
        if (raw == null || raw.getName() == null || raw.getName().isBlank()) {
            return null;
        }
        String category = TagNormalizationService.normalizeCategory(raw.getCategory());
        // gap 是推荐侧的缺口概念，不作为用户记忆标签落库
        if (category == null || "gap".equals(category)) {
            return null;
        }
        String name = raw.getName().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }

        LlmTag tag = new LlmTag();
        tag.setName(name);
        tag.setCategory(category);
        // 极性：默认正向
        tag.setPolarity(raw.getPolarity() != null && raw.getPolarity() == -1 ? -1 : 1);

        int proficiency = raw.getProficiency() == null ? 0 : clamp(raw.getProficiency(), 0, 4);
        if (!"skill".equals(category) && !"experience".equals(category)) {
            proficiency = 0;
        }
        tag.setProficiency(proficiency);

        BigDecimal confidence = raw.getConfidence() == null
                ? BigDecimal.valueOf(0.8)
                : raw.getConfidence().max(BigDecimal.ZERO).min(BigDecimal.ONE);
        tag.setConfidence(confidence.setScale(2, RoundingMode.HALF_UP));

        String evidence = raw.getEvidence() == null || raw.getEvidence().isBlank()
                ? name : raw.getEvidence().trim();
        if (evidence.length() > MAX_EVIDENCE_LENGTH) {
            evidence = evidence.substring(0, MAX_EVIDENCE_LENGTH);
        }
        tag.setEvidence(evidence);
        return tag;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
