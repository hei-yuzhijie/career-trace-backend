package com.yuzhijie.career.trace.module.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.AfterCommit;
import com.yuzhijie.career.trace.common.util.DateTimes;
import com.yuzhijie.career.trace.common.util.Pages;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.memory.dto.CreateMemoryRequest;
import com.yuzhijie.career.trace.module.memory.dto.UpdateMemoryRequest;
import com.yuzhijie.career.trace.module.memory.dto.UpdateMemoryTagRequest;
import com.yuzhijie.career.trace.module.memory.entity.MemoryItem;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryItemMapper;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagCountRow;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import com.yuzhijie.career.trace.module.memory.support.TaggingStatusStore;
import com.yuzhijie.career.trace.module.memory.vo.DeleteMemoryTagVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryDeleteVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryDetailVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryListItemVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryTagVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryUpdateVO;
import com.yuzhijie.career.trace.module.memory.vo.TaggingResultVO;
import com.yuzhijie.career.trace.module.memory.vo.UpdateMemoryTagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 记忆库业务：CRUD + 打标调度 + 手动修正标签。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final Set<String> SOURCE_TYPES = Set.of("resume", "interview", "manual", "review");
    private static final Set<String> TAGGING_STATUSES =
            Set.of(TaggingStatusStore.PENDING, TaggingStatusStore.DONE, TaggingStatusStore.FAILED);
    private static final int MAX_CONTENT_CODE_POINTS = 2000;
    private static final String ASYNC = "ASYNC";

    private final MemoryItemMapper memoryItemMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final MemoryTaggingService memoryTaggingService;
    private final TaggingStatusStore taggingStatusStore;
    private final SkillProfileService skillProfileService;

    @Transactional
    public TaggingResultVO create(Long userId, CreateMemoryRequest request) {
        String content = validateContent(request.getContent());
        String sourceType = validateSourceType(request.getSourceType());

        if (memoryItemMapper.selectRecentDuplicate(userId, content) != null) {
            throw BizException.conflict(ErrorCodes.DUPLICATE_SUBMIT, "内容重复，请稍后再试");
        }

        LocalDateTime now = LocalDateTime.now();
        MemoryItem item = new MemoryItem();
        item.setUserId(userId);
        item.setContent(content);
        item.setSourceType(sourceType);
        item.setSourceId(request.getSourceId());
        item.setStatus(1);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        memoryItemMapper.insert(item);

        taggingStatusStore.tryStart(item.getId());
        AfterCommit.run(() -> memoryTaggingService.tagAsync(item.getId()));
        return new TaggingResultVO(item.getId(), TaggingStatusStore.PENDING, MemoryTaggingService.ESTIMATED_SECONDS);
    }

    public PageResult<MemoryListItemVO> page(Long userId, Integer page, Integer size,
                                             String sourceType, String taggingStatus, String keyword) {
        int pageNo = Pages.page(page);
        int pageSize = Pages.size(size);
        if (taggingStatus != null && !taggingStatus.isBlank()
                && !TAGGING_STATUSES.contains(taggingStatus.trim().toUpperCase(Locale.ROOT))) {
            throw BizException.badRequest("taggingStatus 取值非法");
        }
        String statusFilter = taggingStatus == null || taggingStatus.isBlank()
                ? null : taggingStatus.trim().toUpperCase(Locale.ROOT);

        LambdaQueryWrapper<MemoryItem> query = Wrappers.<MemoryItem>lambdaQuery()
                .eq(MemoryItem::getUserId, userId)
                .eq(MemoryItem::getStatus, 1)
                .eq(sourceType != null && !sourceType.isBlank(), MemoryItem::getSourceType, sourceType)
                .like(keyword != null && !keyword.isBlank(), MemoryItem::getContent, keyword)
                .orderByDesc(MemoryItem::getCreatedAt)
                .orderByDesc(MemoryItem::getId);

        List<MemoryItem> items;
        long total;
        if (statusFilter == null) {
            Page<MemoryItem> result = new Page<>(pageNo, pageSize);
            memoryItemMapper.selectPage(result, query);
            items = result.getRecords();
            total = result.getTotal();
        } else {
            // taggingStatus 由“标签是否存在 + 进程内任务状态”推导，无法下推 SQL；
            // 记忆量级较小，内存过滤后再分页。
            List<MemoryItem> all = memoryItemMapper.selectList(query);
            Map<Long, Integer> allCounts = countTags(all);
            List<MemoryItem> filtered = all.stream()
                    .filter(item -> statusFilter.equals(taggingStatusStore.resolve(
                            item.getId(), allCounts.getOrDefault(item.getId(), 0) > 0)))
                    .toList();
            total = filtered.size();
            items = subList(filtered, pageNo, pageSize);
        }

        Map<Long, Integer> counts = countTags(items);
        List<MemoryListItemVO> records = new ArrayList<>(items.size());
        for (MemoryItem item : items) {
            int tagCount = counts.getOrDefault(item.getId(), 0);
            records.add(new MemoryListItemVO(
                    item.getId(),
                    item.getContent(),
                    item.getSourceType(),
                    taggingStatusStore.resolve(item.getId(), tagCount > 0),
                    tagCount,
                    0,
                    DateTimes.format(item.getCreatedAt())));
        }
        return new PageResult<>(total, pageNo, pageSize, records);
    }

    public MemoryDetailVO detail(Long userId, Long memoryId) {
        MemoryItem item = requireMemory(userId, memoryId);
        List<MemoryTagRow> rows = memoryTagMapper.selectTagRowsByMemoryId(memoryId);
        return new MemoryDetailVO(
                item.getId(),
                item.getContent(),
                item.getSourceType(),
                item.getSourceId(),
                taggingStatusStore.resolve(item.getId(), !rows.isEmpty()),
                DateTimes.format(item.getCreatedAt()),
                DateTimes.format(item.getUpdatedAt()),
                rows.stream().map(MemoryService::toTagVO).toList());
    }

    @Transactional
    public MemoryUpdateVO updateContent(Long userId, Long memoryId, UpdateMemoryRequest request) {
        MemoryItem item = requireMemory(userId, memoryId);
        String content = validateContent(request.getContent());
        boolean changed = !content.equals(item.getContent());
        LocalDateTime updatedAt = item.getUpdatedAt();
        if (changed) {
            updatedAt = LocalDateTime.now();
            memoryItemMapper.update(null, Wrappers.<MemoryItem>lambdaUpdate()
                    .eq(MemoryItem::getId, memoryId)
                    .eq(MemoryItem::getUserId, userId)
                    .set(MemoryItem::getContent, content)
                    .set(MemoryItem::getUpdatedAt, updatedAt));
        }
        return new MemoryUpdateVO(memoryId, DateTimes.format(updatedAt), changed);
    }

    @Transactional
    public MemoryDeleteVO delete(Long userId, Long memoryId) {
        requireMemory(userId, memoryId);
        memoryItemMapper.update(null, Wrappers.<MemoryItem>lambdaUpdate()
                .eq(MemoryItem::getId, memoryId)
                .eq(MemoryItem::getUserId, userId)
                .set(MemoryItem::getStatus, 0)
                .set(MemoryItem::getUpdatedAt, LocalDateTime.now()));
        memoryTagMapper.deleteByMemoryId(memoryId);
        taggingStatusStore.remove(memoryId);
        AfterCommit.run(() -> skillProfileService.recomputeAsync(userId));
        return new MemoryDeleteVO(memoryId, true, ASYNC);
    }

    public TaggingResultVO retag(Long userId, Long memoryId) {
        requireMemory(userId, memoryId);
        if (!taggingStatusStore.tryStart(memoryId)) {
            throw BizException.conflict(ErrorCodes.TAGGING_IN_PROGRESS, "正在分析中，请稍候");
        }
        AfterCommit.run(() -> memoryTaggingService.tagAsync(memoryId));
        return new TaggingResultVO(memoryId, TaggingStatusStore.PENDING, MemoryTaggingService.ESTIMATED_SECONDS);
    }

    public List<MemoryTagVO> tags(Long userId, Long memoryId, String category) {
        requireMemory(userId, memoryId);
        List<MemoryTagRow> rows = memoryTagMapper.selectTagRowsByMemoryId(memoryId);
        return rows.stream()
                .filter(row -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(row.getTagCategory()))
                .map(MemoryService::toTagVO)
                .toList();
    }

    @Transactional
    public UpdateMemoryTagVO updateTag(Long userId, Long memoryId, Long tagId,
                                       UpdateMemoryTagRequest request) {
        requireMemory(userId, memoryId);
        MemoryTagRow row = memoryTagMapper.selectTagRow(memoryId, tagId);
        if (row == null) {
            throw BizException.notFound(ErrorCodes.MEMORY_TAG_NOT_FOUND, "该标签不属于此记忆");
        }
        if (request.getProficiency() == null && request.getPolarity() == null) {
            throw BizException.badRequest("proficiency 与 polarity 至少提供一个");
        }
        if (request.getProficiency() != null
                && (request.getProficiency() < 0 || request.getProficiency() > 4)) {
            throw BizException.badRequest(ErrorCodes.PROFICIENCY_OUT_OF_RANGE, "熟练度需在 0–4 之间");
        }
        if (request.getPolarity() != null && request.getPolarity() != 1 && request.getPolarity() != -1) {
            throw BizException.badRequest("polarity 只能为 1 或 -1");
        }

        memoryTagMapper.updateManual(memoryId, tagId, request.getProficiency(), request.getPolarity());
        AfterCommit.run(() -> skillProfileService.recomputeAsync(userId));

        int effectiveProficiency = request.getProficiency() != null
                ? request.getProficiency() : row.getProficiency();
        return new UpdateMemoryTagVO(memoryId, tagId, effectiveProficiency, "manual", ASYNC);
    }

    @Transactional
    public DeleteMemoryTagVO deleteTag(Long userId, Long memoryId, Long tagId) {
        requireMemory(userId, memoryId);
        int deleted = memoryTagMapper.deleteByMemoryIdAndTagId(memoryId, tagId);
        if (deleted == 0) {
            throw BizException.notFound(ErrorCodes.MEMORY_TAG_NOT_FOUND, "该标签不属于此记忆");
        }
        AfterCommit.run(() -> skillProfileService.recomputeAsync(userId));
        return new DeleteMemoryTagVO(true, ASYNC);
    }

    private MemoryItem requireMemory(Long userId, Long memoryId) {
        MemoryItem item = memoryItemMapper.selectOne(Wrappers.<MemoryItem>lambdaQuery()
                .eq(MemoryItem::getId, memoryId)
                .eq(MemoryItem::getUserId, userId)
                .eq(MemoryItem::getStatus, 1));
        if (item == null) {
            throw BizException.notFound(ErrorCodes.MEMORY_NOT_FOUND, "该记忆不存在或已删除");
        }
        return item;
    }

    private Map<Long, Integer> countTags(List<MemoryItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = items.stream().map(MemoryItem::getId).toList();
        Map<Long, Integer> counts = new HashMap<>();
        for (MemoryTagCountRow row : memoryTagMapper.countByMemoryIds(ids)) {
            counts.put(row.getMemoryId(), row.getTagCount());
        }
        return counts;
    }

    private static <T> List<T> subList(List<T> all, int page, int size) {
        int from = (page - 1) * size;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + size, all.size()));
    }

    private static MemoryTagVO toTagVO(MemoryTagRow row) {
        return new MemoryTagVO(
                row.getTagId(),
                row.getTagName(),
                row.getTagSlug(),
                row.getTagCategory(),
                row.getPolarity(),
                row.getProficiency(),
                row.getConfidence(),
                row.getEvidence(),
                row.getAssessedBy());
    }

    private static String validateContent(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw BizException.badRequest("记忆内容不能为空");
        }
        String content = raw.trim();
        if (content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw BizException.badRequest(ErrorCodes.MEMORY_CONTENT_TOO_LONG, "记忆不能超过 2000 字");
        }
        return content;
    }

    private static String validateSourceType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BizException.badRequest("sourceType 不能为空");
        }
        String sourceType = raw.trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw BizException.badRequest("sourceType 取值非法");
        }
        return sourceType;
    }
}
