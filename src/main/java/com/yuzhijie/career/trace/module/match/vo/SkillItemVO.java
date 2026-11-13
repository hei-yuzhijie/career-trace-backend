package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "技能画像项")
public class SkillItemVO {

    @Schema(description = "标签 ID", example = "106")
    private Long tagId;

    @Schema(description = "技能名", example = "内容策划")
    private String name;

    @Schema(description = "技能唯一标识", example = "nei_rong_ce_hua")
    private String slug;

    @Schema(description = "熟练度 0-4", example = "3")
    private Integer level;

    @Schema(description = "熟练度文案", example = "熟练")
    private String levelLabel;

    @Schema(description = "置信度", example = "0.92")
    private BigDecimal confidence;

    @Schema(description = "支撑该技能的 memory 条数", example = "1")
    private Integer memoryCount;

    @Schema(description = "最强证据记忆 ID", example = "3")
    private Long bestMemoryId;
}
