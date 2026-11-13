package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "得分拆分")
public class ScoreBreakdownVO {

    @Schema(description = "加分项合计（0-85 或 0-100）", example = "78.23")
    private BigDecimal positive;

    @Schema(description = "减分项合计（负数）", example = "-12.40")
    private BigDecimal negative;

    @Schema(description = "中性项合计，通常为 0", example = "0.00")
    private BigDecimal neutral;

    @Schema(description = "结构化分（positive + negative，截断到 0 以上）", example = "65.83")
    private BigDecimal structured;

    @Schema(description = "语义分（0-15）", example = "9.21")
    private BigDecimal semantic;

    @Schema(description = "最终得分（0-100）", example = "75.04")
    private BigDecimal finalScore;
}
