package com.yuzhijie.career.trace.module.match.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户 skill 证据行（memory_tags 联查 memory_items，按 tag 分组前的原始行）。
 */
@Data
public class SkillEvidenceRow {
    private Long tagId;
    private Integer proficiency;
    private BigDecimal confidence;
    private Long memoryId;
}
