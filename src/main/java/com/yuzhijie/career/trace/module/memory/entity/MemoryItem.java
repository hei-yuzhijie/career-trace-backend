package com.yuzhijie.career.trace.module.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("memory_items")
public class MemoryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    /** resume / interview / manual / review */
    private String sourceType;
    private Long sourceId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}