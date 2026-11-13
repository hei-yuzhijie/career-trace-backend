package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "记忆详情")
public class MemoryDetailVO {

    @Schema(description = "记忆 ID", example = "1")
    private Long memoryId;

    @Schema(description = "记忆原文", example = "想留在杭州，能接受大小周，希望有带教")
    private String content;

    @Schema(description = "来源类型", example = "manual")
    private String sourceType;

    @Schema(description = "来源对象 ID", example = "1001")
    private Long sourceId;

    @Schema(description = "打标状态", example = "DONE")
    private String taggingStatus;

    @Schema(description = "创建时间", example = "2026-11-09 14:20:00")
    private String createdAt;

    @Schema(description = "更新时间", example = "2026-11-09 14:20:08")
    private String updatedAt;

    @Schema(description = "标签列表")
    private List<MemoryTagVO> tags;
}
