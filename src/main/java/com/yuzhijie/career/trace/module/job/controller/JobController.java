package com.yuzhijie.career.trace.module.job.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.module.job.service.JobService;
import com.yuzhijie.career.trace.module.job.vo.JobDetailVO;
import com.yuzhijie.career.trace.module.job.vo.JobListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "岗位", description = "岗位列表与详情（浏览用，非推荐）")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "岗位列表")
    @GetMapping
    public ApiResponse<PageResult<JobListItemVO>> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大 50", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "城市筛选", example = "杭州")
            @RequestParam(required = false) String city,
            @Parameter(description = "岗位名模糊匹配", example = "内容")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "公司筛选", example = "1")
            @RequestParam(required = false) Long companyId) {
        return ApiResponse.ok(jobService.page(page, size, city, keyword, companyId));
    }

    @Operation(summary = "岗位详情", description = "岗位不存在或已下线时返回 40402")
    @GetMapping("/{jobId}")
    public ApiResponse<JobDetailVO> detail(
            @Parameter(description = "岗位 ID", required = true, example = "1")
            @PathVariable Long jobId) {
        return ApiResponse.ok(jobService.detail(jobId));
    }
}
