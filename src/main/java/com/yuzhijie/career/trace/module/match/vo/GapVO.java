package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "技能缺口")
public class GapVO {

    @Schema(description = "标签 ID", example = "108")
    private Long tagId;

    @Schema(description = "标签名", example = "A/B 测试")
    private String tagName;

    @Schema(description = "岗位要求熟练度", example = "2")
    private Integer requiredLevel;

    @Schema(description = "用户熟练度", example = "0")
    private Integer userLevel;

    @Schema(description = "补齐建议", example = "补 1–2 个月可补齐，有现成课程与作品集路径")
    private String suggestion;
}
