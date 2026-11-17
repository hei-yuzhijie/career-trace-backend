package com.yuzhijie.career.trace.module.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发起模拟面试请求")
public class InterviewStartRequest {
    @Schema(description = "技能方向标签 ID，1-6 个", example = "[106,107]")
    private List<Long> directionTagIds;
    @Schema(description = "可选目标岗位 ID", example = "3")
    private Long jobId;
    @Schema(description = "可选简历 ID", example = "1")
    private Long resumeId;
    @Schema(description = "可选 JD 文本")
    private String jdText;
    @Schema(description = "难度：basic/intermediate/advanced", example = "intermediate")
    private String difficulty;
    @Schema(description = "主问题数量 3-15", example = "5")
    private Integer questionCount;
    @Schema(description = "每主问题追问数 0-3", example = "1")
    private Integer followUpCount;
}
