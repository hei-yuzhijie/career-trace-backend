package com.yuzhijie.career.trace.module.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.memory.entity.MemoryItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemoryItemMapper extends BaseMapper<MemoryItem> {

    /**
     * 10 秒防抖：同一用户相同原文的最近一条记忆。
     */
    @Select("""
            SELECT * FROM memory_items
            WHERE user_id = #{userId}
              AND content = #{content}
              AND status = 1
              AND created_at >= DATE_SUB(NOW(), INTERVAL 10 SECOND)
            ORDER BY id DESC
            LIMIT 1
            """)
    MemoryItem selectRecentDuplicate(@Param("userId") Long userId, @Param("content") String content);
}
