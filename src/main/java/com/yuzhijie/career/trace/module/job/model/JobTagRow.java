package com.yuzhijie.career.trace.module.job.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobTagRow {
    private Long jobId;
    private Long tagId;
    private BigDecimal weight;
    private Integer requiredLevel;
    private String tagName;
    private String tagCategory;
}
