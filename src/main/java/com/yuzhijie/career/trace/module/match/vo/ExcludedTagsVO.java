package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户排除标签列表")
public class ExcludedTagsVO {

    @Schema(description = "排除标签")
    private List<ExcludedTagVO> tags;
}
