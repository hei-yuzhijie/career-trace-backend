package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "推荐岗位分页结果")
public class RecommendationPageVO {

    @Schema(description = "总条数", example = "3")
    private Long total;

    @Schema(description = "页码", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "10")
    private Integer size;

    @Schema(description = "过滤摘要")
    private FilterSummaryVO filterSummary;

    @Schema(description = "提示信息（如用户还没有任何正向标签）", example = "请先补充职业记忆")
    private String hint;

    @Schema(description = "推荐岗位列表")
    private List<RecommendationItemVO> records;
}
