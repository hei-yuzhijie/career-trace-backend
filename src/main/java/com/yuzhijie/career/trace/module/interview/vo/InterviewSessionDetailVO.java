package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "面试会话详情")
public class InterviewSessionDetailVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "状态：0生成中 1待作答 2评估中 3完成 4失败")
    private Integer status;
    @Schema(description = "状态文案")
    private String statusText;
    @Schema(description = "难度")
    private String difficulty;
    @Schema(description = "主问题数")
    private Integer questionCount;
    @Schema(description = "追问数")
    private Integer followUpCount;
    @Schema(description = "方向列表")
    private List<InterviewDirectionVO> directions;
    @Schema(description = "题目扁平列表；追问通过 parentIndex 关联")
    private List<InterviewQuestionVO> questions;
    @Schema(description = "已保存答案数")
    private Integer answeredCount;
    @Schema(description = "报告；FINISHED 时有值")
    private InterviewReportVO report;
    @Schema(description = "记忆候选数")
    private Integer memoryCandidateCount;
    @Schema(description = "失败原因")
    private String errorMessage;
}
