package com.yuzhijie.career.trace.module.memory.service;

import com.yuzhijie.career.trace.module.memory.entity.MemoryTag;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * memory_tags 的原子替换：重新打标时先删旧标签，再写新标签，避免“打标失败留下半套标签”。
 */
@Service
@RequiredArgsConstructor
public class MemoryTagWriter {

    private final MemoryTagMapper memoryTagMapper;

    @Transactional
    public void replaceTags(Long memoryId, List<MemoryTag> tags) {
        memoryTagMapper.deleteByMemoryId(memoryId);
        for (MemoryTag tag : tags) {
            memoryTagMapper.upsert(tag);
        }
    }
}
