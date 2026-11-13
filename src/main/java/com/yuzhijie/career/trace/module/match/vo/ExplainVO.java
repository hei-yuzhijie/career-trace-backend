package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "岗位匹配归因详情")
public class ExplainVO {

    @Schema(description = "岗位 ID", example = "1")
    private Long jobId;

    @Schema(description = "岗位名", example = "内容运营（校招）")
    private String title;

    @Schema(description = "公司名", example = "字节跳动")
    private String companyName;

    @Schema(description = "城市", example = "杭州")
    private String city;

    @Schema(description = "匹配总分 0-100", example = "75.04")
    private BigDecimal score;

    @Schema(description = "得分拆分")
    private ScoreBreakdownVO scoreBreakdown;

    @Schema(description = "语义匹配信息；语义不可用时 available=false")
    private SemanticMatchVO semantic;

    @Schema(description = "加分项")
    private List<MatchReasonVO> positive;

    @Schema(description = "减分项")
    private List<MatchReasonVO> negative;

    @Schema(description = "技能 / 条件缺口")
    private List<GapVO> gap;

    @Schema(description = "中性项")
    private List<MatchReasonVO> neutral;
}
