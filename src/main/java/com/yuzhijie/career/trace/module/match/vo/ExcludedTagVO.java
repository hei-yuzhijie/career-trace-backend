package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户排除标签")
public class ExcludedTagVO {

    @Schema(description = "标签 ID", example = "105")
    private Long tagId;

    @Schema(description = "标签名", example = "销售")
    private String name;

    @Schema(description = "标签唯一标识", example = "xiao_shou")
    private String slug;

    @Schema(description = "类别", example = "preference")
    private String category;

    @Schema(description = "支撑原文", example = "不考虑销售、地推类岗位")
    private String evidence;

    @Schema(description = "来源记忆 ID", example = "2")
    private Long memoryId;
}
