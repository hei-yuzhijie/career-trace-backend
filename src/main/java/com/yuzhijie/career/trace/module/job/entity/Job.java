package com.yuzhijie.career.trace.module.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jobs")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String externalId;
    private String source;
    private Long companyId;
    private String title;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String jdText;
    private Integer status;
    private LocalDateTime createdAt;
}