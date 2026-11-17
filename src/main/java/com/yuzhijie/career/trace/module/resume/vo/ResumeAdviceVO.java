package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历改进建议")
public class ResumeAdviceVO {
    @Schema(description = "类别：内容/格式/技能/项目", example = "项目")
    private String category;
    @Schema(description = "优先级：高/中/低", example = "高")
    private String priority;
    @Schema(description = "问题")
    private String issue;
    @Schema(description = "改进建议")
    private String recommendation;
    @Schema(description = "原文证据")
    private String evidence;
}
