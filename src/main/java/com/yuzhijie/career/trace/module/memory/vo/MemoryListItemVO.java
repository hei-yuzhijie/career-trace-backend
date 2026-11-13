package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "记忆列表项")
public class MemoryListItemVO {

    @Schema(description = "记忆 ID", example = "3")
    private Long memoryId;

    @Schema(description = "记忆原文", example = "9 月做了 3 篇小红书笔记，其中 1 篇阅读 4.2 万")
    private String content;

    @Schema(description = "来源类型", example = "resume")
    private String sourceType;

    @Schema(description = "打标状态", example = "DONE")
    private String taggingStatus;

    @Schema(description = "标签数量", example = "2")
    private Integer tagCount;

    @Schema(description = "在推荐中被引用的次数（第一版恒为 0）", example = "0")
    private Integer usedInMatchCount;

    @Schema(description = "创建时间", example = "2026-11-09 14:22:10")
    private String createdAt;
}
