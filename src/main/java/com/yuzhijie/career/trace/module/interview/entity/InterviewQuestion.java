package com.yuzhijie.career.trace.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_questions")
public class InterviewQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer questionIndex;
    private Integer parentIndex;
    private String source;
    private Long tagId;
    private String directionName;
    private String topicSummary;
    private String title;
    private String referenceAnswer;
    private String points;
    private LocalDateTime createdAt;
}
