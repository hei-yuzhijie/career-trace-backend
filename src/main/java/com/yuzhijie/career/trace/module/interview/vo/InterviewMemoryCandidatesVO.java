package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试记忆候选列表")
public class InterviewMemoryCandidatesVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "候选列表")
    private List<InterviewMemoryCandidateVO> candidates;
}
