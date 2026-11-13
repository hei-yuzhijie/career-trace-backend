package com.yuzhijie.career.trace.module.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_aliases")
public class TagAlias {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tagId;
    private String alias;
    private String aliasSlug;
    private LocalDateTime createdAt;
}