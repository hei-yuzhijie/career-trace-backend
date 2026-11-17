package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发起模拟面试响应")
public class InterviewStartVO {
    @Schema(description = "会话 ID", example = "21")
    private Long sessionId;
    @Schema(description = "状态：0生成中 1待作答 2评估中 3完成 4失败", example = "0")
    private Integer status;
    @Schema(description = "状态文案", example = "GENERATING")
    private String statusText;
}
