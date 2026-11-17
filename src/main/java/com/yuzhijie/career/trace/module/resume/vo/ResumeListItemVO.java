package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历列表项")
public class ResumeListItemVO {
    @Schema(description = "简历 ID", example = "12")
    private Long resumeId;
    @Schema(description = "原始文件名", example = "陈默-内容运营-简历.pdf")
    private String originalFilename;
    @Schema(description = "状态", example = "2")
    private Integer status;
    @Schema(description = "总分", example = "82")
    private Integer overallScore;
    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
