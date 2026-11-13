package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐过滤摘要")
public class FilterSummaryVO {

    @Schema(description = "被用户排除项过滤掉的岗位 ID", example = "[4]")
    private List<Long> excludedByPolarity;

    @Schema(description = "被 location 硬过滤（含 city 收窄）掉的岗位 ID", example = "[2]")
    private List<Long> excludedByLocation;

    @Schema(description = "进入第二段加权计算的岗位数", example = "3")
    private Integer candidateCount;
}
