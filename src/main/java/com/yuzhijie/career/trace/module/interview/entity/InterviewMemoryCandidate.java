package com.yuzhijie.career.trace.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("interview_memory_candidates")
public class InterviewMemoryCandidate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private Long tagId;
    private String content;
    private Integer proficiency;
    private BigDecimal confidence;
    private String evidence;
    /** 0 待确认 / 1 已确认 / 2 已忽略 */
    private Integer status;
    private Long memoryId;
    private LocalDateTime createdAt;
}
