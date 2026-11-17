package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历记忆候选列表")
public class ResumeMemoryCandidatesVO {
    @Schema(description = "简历 ID")
    private Long resumeId;
    @Schema(description = "候选列表")
    private List<ResumeMemoryCandidateVO> candidates;
}
