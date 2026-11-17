package com.yuzhijie.career.trace.module.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "保存单题答案请求")
public class SaveAnswerRequest {
    @Schema(description = "答案，最大 5000 字")
    private String answerText;
}
