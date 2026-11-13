package com.yuzhijie.career.trace.module.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tags")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    /** skill/experience/preference/location/seniority/gap */
    private String category;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createdAt;
}
