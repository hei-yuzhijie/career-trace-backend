package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "职业经历诊断")
public class CareerDiagnosisVO {
    @Schema(description = "数据来源", example = "MEMORY")
    private String source;
    @Schema(description = "目标岗位 ID；未指定时为空")
    private Long targetJobId;
    @Schema(description = "总分 0-100", example = "76")
    private Integer overallScore;
    @Schema(description = "总结")
    private String summary;
    @Schema(description = "五维得分")
    private List<ResumeDimensionVO> dimensions;
    @Schema(description = "优势")
    private List<String> strengths;
    @Schema(description = "缺口")
    private List<String> gaps;
    @Schema(description = "建议")
    private List<String> suggestions;
}
