package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历记忆候选忽略结果")
public class ResumeIgnoreResultVO {
    @Schema(description = "简历 ID")
    private Long resumeId;
    @Schema(description = "成功忽略数量")
    private Integer ignored;
    @Schema(description = "跳过数量")
    private Integer skipped;
}
