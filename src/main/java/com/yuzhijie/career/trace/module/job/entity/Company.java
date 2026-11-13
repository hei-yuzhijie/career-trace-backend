package com.yuzhijie.career.trace.module.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("companies")
public class Company {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String externalId;
    private String name;
    private String industry;
    private String size;
    private String stage;
    private LocalDateTime createdAt;
}