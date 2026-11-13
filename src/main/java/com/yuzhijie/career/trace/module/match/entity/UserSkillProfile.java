package com.yuzhijie.career.trace.module.match.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复合主键 (user_id, tag_id)，仅 category=skill 的标签进本表。
 * 异步任务维护，推荐读此表。
 */
@Data
@TableName("user_skill_profile")
public class UserSkillProfile {
    private Long userId;
    private Long tagId;
    private Integer level;
    private BigDecimal confidence;
    private Integer memoryCount;
    private Long bestMemoryId;
    private LocalDateTime updatedAt;
}