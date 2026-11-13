package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐岗位项")
public class RecommendationItemVO {

    @Schema(description = "岗位 ID", example = "3")
    private Long jobId;

    @Schema(description = "岗位名", example = "内容运营")
    private String title;

    @Schema(description = "公司名", example = "某杭州电商公司")
    private String companyName;

    @Schema(description = "城市", example = "杭州")
    private String city;

    @Schema(description = "薪资下限（K）", example = "12")
    private Integer salaryMin;

    @Schema(description = "薪资上限（K）", example = "18")
    private Integer salaryMax;

    @Schema(description = "匹配总分 0-100（结构化分 + 语义分）", example = "83.52")
    private BigDecimal score;

    @Schema(description = "结构化分（语义可用时满分 85，不可用时满分 100）", example = "74.31")
    private BigDecimal structuredScore;

    @Schema(description = "语义分（0-15）；语义不可用时为空", example = "9.21")
    private BigDecimal semanticScore;

    @Schema(description = "命中的正向标签数", example = "3")
    private Integer matchedTagCount;

    @Schema(description = "归因明细；explain=false 时不返回")
    private List<MatchReasonVO> reasons;
}
