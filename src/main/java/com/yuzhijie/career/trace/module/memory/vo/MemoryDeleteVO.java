package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "删除记忆响应")
public class MemoryDeleteVO {

    @Schema(description = "记忆 ID", example = "1")
    private Long memoryId;

    @Schema(description = "是否删除成功", example = "true")
    private Boolean deleted;

    @Schema(description = "skill 画像重算方式", example = "ASYNC")
    private String skillProfileRecalc;
}
