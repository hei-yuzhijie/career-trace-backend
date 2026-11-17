package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试方向")
public class InterviewDirectionVO {
    @Schema(description = "标签 ID", example = "106")
    private Long tagId;
    @Schema(description = "方向名", example = "内容策划")
    private String name;
}
