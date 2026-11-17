package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历上传响应")
public class ResumeUploadVO {
    @Schema(description = "简历 ID", example = "12")
    private Long resumeId;
    @Schema(description = "状态：0待分析 1分析中 2完成 3失败", example = "0")
    private Integer status;
    @Schema(description = "状态文案", example = "PENDING")
    private String statusText;
}
