package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历转职业记忆候选")
public class ResumeMemoryCandidateVO {
    @Schema(description = "候选 ID", example = "31")
    private Long candidateId;
    @Schema(description = "拟写入记忆的内容")
    private String content;
    @Schema(description = "标签候选")
    private List<ResumeMemoryTagVO> tags;
    @Schema(description = "状态：0待确认 1已导入 2已忽略", example = "0")
    private Integer status;
}
