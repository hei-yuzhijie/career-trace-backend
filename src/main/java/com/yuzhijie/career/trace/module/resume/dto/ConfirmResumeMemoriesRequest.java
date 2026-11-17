package com.yuzhijie.career.trace.module.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "确认导入简历记忆候选")
public class ConfirmResumeMemoriesRequest {
    @Schema(description = "候选 ID 列表")
    private List<Long> candidateIds;
}
