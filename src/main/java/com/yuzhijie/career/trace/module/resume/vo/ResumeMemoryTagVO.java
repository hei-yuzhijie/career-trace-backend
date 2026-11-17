package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历记忆候选标签")
public class ResumeMemoryTagVO {
    @Schema(description = "标签名", example = "内容策划")
    private String tagName;
    @Schema(description = "标签类别", example = "skill")
    private String category;
    @Schema(description = "熟练度 0-4", example = "3")
    private Integer proficiency;
    @Schema(description = "置信度", example = "0.92")
    private BigDecimal confidence;
    @Schema(description = "原文证据")
    private String evidence;
}
