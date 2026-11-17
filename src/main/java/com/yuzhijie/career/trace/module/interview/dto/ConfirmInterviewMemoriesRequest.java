package com.yuzhijie.career.trace.module.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "确认面试记忆候选")
public class ConfirmInterviewMemoriesRequest {
    @Schema(description = "候选 ID 列表")
    private List<Long> candidateIds;
}
