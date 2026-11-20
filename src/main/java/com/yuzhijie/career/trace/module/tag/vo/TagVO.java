package com.yuzhijie.career.trace.module.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "标准标签")
public class TagVO {

    @Schema(description = "标签 ID", example = "106")
    private Long tagId;

    @Schema(description = "标签名", example = "内容策划")
    private String name;

    @Schema(description = "标签唯一标识", example = "nei_rong_ce_hua")
    private String slug;

    @Schema(description = "类别：skill/experience/preference/location/seniority/gap", example = "skill")
    private String category;
}
