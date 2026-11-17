package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历诊断维度")
public class ResumeDimensionVO {
    @Schema(description = "维度名", example = "经历/项目深度")
    private String dimension;
    @Schema(description = "得分", example = "27")
    private Integer score;
    @Schema(description = "满分", example = "30")
    private Integer maxScore;
    @Schema(description = "评述")
    private String comment;
}
