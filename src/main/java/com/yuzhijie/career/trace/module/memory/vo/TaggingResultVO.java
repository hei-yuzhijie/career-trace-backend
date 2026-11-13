package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "新增记忆 / 重新打标响应")
public class TaggingResultVO {

    @Schema(description = "记忆 ID", example = "101")
    private Long memoryId;

    @Schema(description = "打标状态：PENDING/DONE/FAILED", example = "PENDING")
    private String taggingStatus;

    @Schema(description = "预计耗时（秒）", example = "8")
    private Integer estimatedSeconds;
}
