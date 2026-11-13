package com.yuzhijie.career.trace.module.match.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.module.match.service.RecommendationService;
import com.yuzhijie.career.trace.module.match.vo.ExplainVO;
import com.yuzhijie.career.trace.module.match.vo.RecommendationPageVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "岗位匹配", description = "两段式推荐与匹配归因")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取推荐岗位（核心）",
            description = "第一段做排除过滤 + location 硬过滤（city 额外收窄），第二段加权求和并返回归因；"
                    + "explain=false 时只返回分数与命中数，不组装 reasons")
    @GetMapping
    public ApiResponse<RecommendationPageVO> recommend(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大 50", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "额外城市筛选，在用户地点硬过滤之上再收窄", example = "杭州")
            @RequestParam(required = false) String city,
            @Parameter(description = "最低分过滤，默认 0", example = "0")
            @RequestParam(required = false) BigDecimal minScore,
            @Parameter(description = "是否返回归因明细，默认 true", example = "true")
            @RequestParam(defaultValue = "true") boolean explain) {
        return ApiResponse.ok(recommendationService.recommend(
                UserContext.require(), page, size, city, minScore, explain));
    }

    @Operation(summary = "匹配归因详情",
            description = "针对单个岗位实时计算加分项、减分项、缺口与中性项，对应“为什么是这个分”的弹层")
    @GetMapping("/{jobId}/explain")
    public ApiResponse<ExplainVO> explain(
            @Parameter(description = "岗位 ID", required = true, example = "1")
            @PathVariable Long jobId) {
        return ApiResponse.ok(recommendationService.explain(UserContext.require(), jobId));
    }
}
