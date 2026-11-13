package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "删除记忆标签响应")
public class DeleteMemoryTagVO {

    @Schema(description = "是否删除成功", example = "true")
    private Boolean deleted;

    @Schema(description = "skill 画像重算方式", example = "ASYNC")
    private String skillProfileRecalc;
}
