package com.yuzhijie.career.trace.module.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "岗位列表项")
public class JobListItemVO {

    @Schema(description = "岗位 ID", example = "1")
    private Long jobId;

    @Schema(description = "岗位名", example = "内容运营（校招）")
    private String title;

    @Schema(description = "公司 ID", example = "1")
    private Long companyId;

    @Schema(description = "公司名", example = "字节跳动")
    private String companyName;

    @Schema(description = "城市", example = "杭州")
    private String city;

    @Schema(description = "薪资下限（K）", example = "15")
    private Integer salaryMin;

    @Schema(description = "薪资上限（K）", example = "25")
    private Integer salaryMax;

    @Schema(description = "状态：1 在招，0 下线", example = "1")
    private Integer status;
}
