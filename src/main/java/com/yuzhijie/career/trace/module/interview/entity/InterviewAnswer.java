package com.yuzhijie.career.trace.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_answers")
public class InterviewAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer questionIndex;
    private String answerText;
    private LocalDateTime answeredAt;
}
