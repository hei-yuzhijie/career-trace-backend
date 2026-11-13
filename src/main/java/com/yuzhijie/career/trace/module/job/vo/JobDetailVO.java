package com.yuzhijie.career.trace.module.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "岗位详情")
public class JobDetailVO {

    @Schema(description = "岗位 ID", example = "1")
    private Long jobId;

    @Schema(description = "外部岗位 ID", example = "boss_j_2001")
    private String externalId;

    @Schema(description = "来源", example = "boss")
    private String source;

    @Schema(description = "岗位名", example = "内容运营（校招）")
    private String title;

    @Schema(description = "公司信息")
    private JobCompanyVO company;

    @Schema(description = "城市", example = "杭州")
    private String city;

    @Schema(description = "薪资下限（K）", example = "15")
    private Integer salaryMin;

    @Schema(description = "薪资上限（K）", example = "25")
    private Integer salaryMax;

    @Schema(description = "JD 原文")
    private String jdText;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "岗位标签")
    private List<JobTagVO> tags;
}
