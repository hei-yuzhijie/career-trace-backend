package com.yuzhijie.career.trace.module.memory.service;

import com.yuzhijie.career.trace.module.memory.entity.MemoryItem;
import com.yuzhijie.career.trace.module.memory.entity.MemoryTag;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryItemMapper;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagInput;
import com.yuzhijie.career.trace.module.tag.service.TagNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 外部模块确认后写入职业记忆：memory_items + memory_tags。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalMemoryImportService {

    private final MemoryItemMapper memoryItemMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final TagNormalizationService tagNormalizationService;

    @Transactional
    public Long importMemory(Long userId, String content, String sourceType, Long sourceId,
                             List<MemoryTagInput> tags) {
        MemoryItem item = new MemoryItem();
        item.setUserId(userId);
        item.setContent(content);
        item.setSourceType(sourceType);
        item.setSourceId(sourceId);
        item.setStatus(1);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        memoryItemMapper.insert(item);

        if (tags != null && !tags.isEmpty()) {
            TagNormalizationService.TagIndex index = tagNormalizationService.loadIndex();
            for (MemoryTagInput input : tags) {
                if (input == null || input.getName() == null || input.getName().isBlank()) {
                    continue;
                }
                Long tagId = tagNormalizationService.resolveOrCreate(
                        index, input.getName(), input.getCategory(), 1);
                if (tagId == null) {
                    continue;
                }
                MemoryTag tag = new MemoryTag();
                tag.setMemoryId(item.getId());
                tag.setTagId(tagId);
                tag.setPolarity(1);
                tag.setProficiency(clamp(input.getProficiency()));
                tag.setConfidence(clamp(input.getConfidence()));
                tag.setEvidence(input.getEvidence());
                tag.setAssessedBy("manual");
                memoryTagMapper.upsert(tag);
            }
        }
        return item.getId();
    }

    private static int clamp(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(4, value));
    }

    private static BigDecimal clamp(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.8);
        }
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
