package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "匹配归因项")
public class MatchReasonVO {

    @Schema(description = "标签 ID", example = "106")
    private Long tagId;

    @Schema(description = "标签名", example = "内容策划")
    private String tagName;

    @Schema(description = "类别", example = "skill")
    private String category;

    @Schema(description = "岗位侧权重：1.0 必备 / 0.5 加分", example = "1.0")
    private BigDecimal weight;

    @Schema(description = "岗位要求熟练度", example = "3")
    private Integer requiredLevel;

    @Schema(description = "用户熟练度", example = "3")
    private Integer userLevel;

    @Schema(description = "等级差", example = "0")
    private Integer levelGap;

    @Schema(description = "用户标签置信度", example = "0.92")
    private BigDecimal confidence;

    @Schema(description = "支撑该标签的记忆原文片段", example = "做了 3 篇小红书笔记")
    private String evidence;

    @Schema(description = "证据记忆 ID", example = "3")
    private Long memoryId;

    @Schema(description = "减分 / 中性项原因", example = "缺失该技能证据")
    private String reason;

    @Schema(description = "对总分的贡献分数（可为负）", example = "12.04")
    private BigDecimal contribution;
}
