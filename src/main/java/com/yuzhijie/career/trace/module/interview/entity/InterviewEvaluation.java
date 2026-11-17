package com.yuzhijie.career.trace.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_evaluations")
public class InterviewEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer questionIndex;
    private Integer score;
    private String feedback;
    private String referenceAnswer;
    private String points;
    private LocalDateTime evaluatedAt;
}
