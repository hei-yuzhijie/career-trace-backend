package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试记忆导入结果")
public class InterviewImportResultVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "成功导入数量")
    private Integer imported;
    @Schema(description = "跳过数量")
    private Integer skipped;
    @Schema(description = "写入的记忆 ID")
    private List<Long> memoryIds;
}
