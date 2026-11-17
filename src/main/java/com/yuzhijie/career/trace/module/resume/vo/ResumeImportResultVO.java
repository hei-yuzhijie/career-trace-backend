package com.yuzhijie.career.trace.module.resume.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简历记忆导入结果")
public class ResumeImportResultVO {
    @Schema(description = "简历 ID")
    private Long resumeId;
    @Schema(description = "成功导入数量")
    private Integer imported;
    @Schema(description = "跳过数量")
    private Integer skipped;
    @Schema(description = "写入的记忆 ID")
    private List<Long> memoryIds;
}
