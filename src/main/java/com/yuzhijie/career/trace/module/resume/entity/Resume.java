package com.yuzhijie.career.trace.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resumes")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private String fileHash;
    private String originalFilename;
    private String contentType;
    private String fileKey;
    private String textContent;
    /** 0 PENDING / 1 PROCESSING / 2 DONE / 3 FAILED */
    private Integer status;
    private Integer overallScore;
    private String analysisJson;
    private Integer retryCount;
    private String attemptId;
    private String promptVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
