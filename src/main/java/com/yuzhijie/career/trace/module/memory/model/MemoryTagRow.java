package com.yuzhijie.career.trace.module.memory.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * memory_tags 联查 tags 的扁平行，供详情、标签列表、推荐共用。
 */
@Data
public class MemoryTagRow {
    private Long memoryId;
    private Long tagId;
    private Integer polarity;
    private Integer proficiency;
    private BigDecimal confidence;
    private String evidence;
    private String assessedBy;
    private String tagName;
    private String tagSlug;
    private String tagCategory;
}
