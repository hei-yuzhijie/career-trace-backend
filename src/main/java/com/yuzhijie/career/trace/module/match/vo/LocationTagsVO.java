package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户地点标签列表")
public class LocationTagsVO {

    @Schema(description = "地点标签")
    private List<LocationTagVO> tags;
}
