package com.yuzhijie.career.trace.module.memory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复合主键 (memory_id, tag_id)。
 * MyBatis-Plus 不支持复合主键的 @TableId，本类不声明主键，
 * Mapper 中通过 Wrapper 或自定义 SQL 做增删改查。
 * 用户修正熟练度时走 ON DUPLICATE KEY UPDATE 覆盖本行。
 */
@Data
@TableName("memory_tags")
public class MemoryTag {
    private Long memoryId;
    private Long tagId;
    /** 1 正向 / -1 排除项 */
    private Integer polarity;
    /** 0-4，仅 skill 和部分 experience 有效 */
    private Integer proficiency;
    private BigDecimal confidence;
    /** 支撑该标签的原文片段，用于溯源 */
    private String evidence;
    /** llm / manual / review */
    private String assessedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}