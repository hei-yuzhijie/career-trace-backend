package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试记忆候选")
public class InterviewMemoryCandidateVO {
    @Schema(description = "候选 ID")
    private Long candidateId;
    @Schema(description = "标签 ID")
    private Long tagId;
    @Schema(description = "标签名")
    private String tagName;
    @Schema(description = "拟写入记忆内容")
    private String content;
    @Schema(description = "熟练度 0-4")
    private Integer proficiency;
    @Schema(description = "置信度")
    private BigDecimal confidence;
    @Schema(description = "证据")
    private String evidence;
    @Schema(description = "状态：0待确认 1已确认 2已忽略")
    private Integer status;
}
