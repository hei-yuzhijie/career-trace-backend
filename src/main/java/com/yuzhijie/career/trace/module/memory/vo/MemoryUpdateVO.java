package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新记忆原文响应")
public class MemoryUpdateVO {

    @Schema(description = "记忆 ID", example = "1")
    private Long memoryId;

    @Schema(description = "更新时间", example = "2026-11-09 15:01:00")
    private String updatedAt;

    @Schema(description = "是否需要重新打标（原文有变化时为 true）", example = "true")
    private Boolean needRetag;
}
