package com.yuzhijie.career.trace.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_advices")
public class ResumeAdvice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private String category;
    private String priority;
    private String issue;
    private String recommendation;
    private String evidence;
    private LocalDateTime createdAt;
}
