package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "已保存答案")
public class InterviewAnswerVO {
    @Schema(description = "题目序号")
    private Integer questionIndex;
    @Schema(description = "答案")
    private String answerText;
    @Schema(description = "保存时间")
    private LocalDateTime answeredAt;
}
