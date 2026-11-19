package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试记忆候选忽略结果")
public class InterviewIgnoreResultVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "成功忽略数量")
    private Integer ignored;
    @Schema(description = "跳过数量")
    private Integer skipped;
}
