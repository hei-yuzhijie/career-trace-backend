package com.yuzhijie.career.trace.module.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 外部模块（简历/面试）确认导入职业记忆时的标签输入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryTagInput {
    private String name;
    private String category;
    private Integer proficiency;
    private BigDecimal confidence;
    private String evidence;
}
