package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "逐题评估")
public class InterviewEvaluationVO {
    @Schema(description = "题目序号", example = "0")
    private Integer questionIndex;
    @Schema(description = "得分 0-100", example = "82")
    private Integer score;
    @Schema(description = "反馈")
    private String feedback;
    @Schema(description = "参考答案")
    private String referenceAnswer;
    @Schema(description = "核心要点")
    private String points;
}
