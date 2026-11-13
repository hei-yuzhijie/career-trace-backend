package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户地点标签")
public class LocationTagVO {

    @Schema(description = "标签 ID", example = "101")
    private Long tagId;

    @Schema(description = "标签名", example = "杭州")
    private String name;

    @Schema(description = "标签唯一标识", example = "hangzhou")
    private String slug;

    @Schema(description = "支撑原文", example = "想留在杭州")
    private String evidence;

    @Schema(description = "来源记忆 ID", example = "1")
    private Long memoryId;
}
