package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "简历诊断详情")
public class ResumeDetailVO {
    @Schema(description = "简历 ID", example = "12")
    private Long resumeId;
    @Schema(description = "原始文件名", example = "陈默-内容运营-简历.pdf")
    private String originalFilename;
    @Schema(description = "目标岗位 ID")
    private Long jobId;
    @Schema(description = "状态：0待分析 1分析中 2完成 3失败", example = "2")
    private Integer status;
    @Schema(description = "总分 0-100", example = "82")
    private Integer overallScore;
    @Schema(description = "一句话总结")
    private String summary;
    @Schema(description = "优势")
    private List<String> strengths;
    @Schema(description = "五维得分")
    private List<ResumeDimensionVO> dimensions;
    @Schema(description = "改进建议")
    private List<ResumeAdviceVO> advices;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
