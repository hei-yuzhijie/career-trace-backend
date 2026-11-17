package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "面试报告")
public class InterviewReportVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "总分 0-100", example = "76")
    private Integer totalScore;
    @Schema(description = "总评")
    private String summary;
    @Schema(description = "优势")
    private List<String> strengths;
    @Schema(description = "改进建议")
    private List<String> improvements;
    @Schema(description = "维度分")
    private List<InterviewDimensionVO> dimensions;
    @Schema(description = "逐题评估")
    private List<InterviewEvaluationVO> evaluations;
}
