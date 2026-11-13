package com.yuzhijie.career.trace.module.match.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkillProfileRow {
    private Long tagId;
    private String name;
    private String slug;
    private Integer level;
    private BigDecimal confidence;
    private Integer memoryCount;
    private Long bestMemoryId;
    private LocalDateTime updatedAt;
}
