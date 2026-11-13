package com.yuzhijie.career.trace.module.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "岗位标签")
public class JobTagVO {

    @Schema(description = "标签 ID", example = "101")
    private Long tagId;

    @Schema(description = "标签名", example = "杭州")
    private String name;

    @Schema(description = "类别", example = "location")
    private String category;

    @Schema(description = "权重：1.0 必备 / 0.5 加分", example = "1.0")
    private BigDecimal weight;

    @Schema(description = "要求的熟练度（仅 skill 有效）", example = "0")
    private Integer requiredLevel;
}
