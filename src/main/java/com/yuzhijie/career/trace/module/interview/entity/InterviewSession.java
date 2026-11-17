package com.yuzhijie.career.trace.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_sessions")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Long jobId;
    private String jdText;
    private String directionTagIds;
    private String difficulty;
    private Integer questionCount;
    private Integer followUpCount;
    /** 0 GENERATING / 1 READY / 2 EVALUATING / 3 FINISHED / 4 FAILED */
    private Integer status;
    private Integer totalScore;
    private String summary;
    private String strengths;
    private String improvements;
    private String promptVersion;
    private String attemptId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
