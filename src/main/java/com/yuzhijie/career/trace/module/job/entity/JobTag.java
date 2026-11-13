package com.yuzhijie.career.trace.module.job.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复合主键 (job_id, tag_id)，不声明 @TableId。
 */
@Data
@TableName("job_tags")
public class JobTag {
    private Long jobId;
    private Long tagId;
    /** 1.0 必备 / 0.5 加分 */
    private BigDecimal weight;
    /** 仅 skill 类有意义，其他存 0 */
    private Integer requiredLevel;
    private LocalDateTime createdAt;
}