package com.yuzhijie.career.trace.module.job.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRow {
    private Long jobId;
    private String externalId;
    private String source;
    private Long companyId;
    private String companyName;
    private String companyIndustry;
    private String companySize;
    private String companyStage;
    private String title;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String jdText;
    private Integer status;
    private LocalDateTime createdAt;
}
