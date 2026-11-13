package com.yuzhijie.career.trace.module.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "岗位所属公司")
public class JobCompanyVO {

    @Schema(description = "公司 ID", example = "1")
    private Long companyId;

    @Schema(description = "公司名", example = "字节跳动")
    private String name;

    @Schema(description = "行业", example = "互联网")
    private String industry;

    @Schema(description = "规模", example = "10000人以上")
    private String size;

    @Schema(description = "融资阶段", example = "已上市")
    private String stage;
}
