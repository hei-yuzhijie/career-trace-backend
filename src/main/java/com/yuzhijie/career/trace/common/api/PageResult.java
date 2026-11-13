package com.yuzhijie.career.trace.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应体")
public class PageResult<T> {

    @Schema(description = "总条数", example = "42")
    private long total;

    @Schema(description = "页码，从 1 开始", example = "1")
    private int page;

    @Schema(description = "每页条数", example = "10")
    private int size;

    @Schema(description = "当前页记录")
    private List<T> records;
}
