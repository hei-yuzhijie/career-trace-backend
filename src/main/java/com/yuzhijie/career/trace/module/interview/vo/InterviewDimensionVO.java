package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试维度分")
public class InterviewDimensionVO {
    @Schema(description = "标签 ID")
    private Long tagId;
    @Schema(description = "方向名")
    private String name;
    @Schema(description = "维度分 0-100")
    private Integer score;
    @Schema(description = "题目数")
    private Integer questionCount;
}
