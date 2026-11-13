package com.yuzhijie.career.trace.module.memory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "手动修正标签响应")
public class UpdateMemoryTagVO {

    @Schema(description = "记忆 ID", example = "3")
    private Long memoryId;

    @Schema(description = "标签 ID", example = "106")
    private Long tagId;

    @Schema(description = "修正后的熟练度", example = "4")
    private Integer proficiency;

    @Schema(description = "评估来源", example = "manual")
    private String assessedBy;

    @Schema(description = "skill 画像重算方式", example = "ASYNC")
    private String skillProfileRecalc;
}
