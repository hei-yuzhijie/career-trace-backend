package com.yuzhijie.career.trace.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_memory_candidates")
public class ResumeMemoryCandidate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Long userId;
    private String content;
    private String tagsJson;
    /** 0 待确认 / 1 已导入 / 2 已忽略 */
    private Integer status;
    private Long memoryId;
    private LocalDateTime createdAt;
}
