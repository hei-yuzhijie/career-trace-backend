package com.yuzhijie.career.trace.module.match.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.module.match.service.RecommendationService;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.match.vo.ExcludedTagsVO;
import com.yuzhijie.career.trace.module.match.vo.LocationTagsVO;
import com.yuzhijie.career.trace.module.match.vo.SkillProfileVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户画像", description = "用户 skill 聚合、排除标签与地点标签")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final SkillProfileService skillProfileService;
    private final RecommendationService recommendationService;

    @Operation(summary = "查询用户 skill 聚合画像", description = "读 user_skill_profile，仅含 category=skill 的标签")
    @GetMapping("/skill-profile")
    public ApiResponse<SkillProfileVO> skillProfile() {
        return ApiResponse.ok(skillProfileService.getProfile(UserContext.require()));
    }

    @Operation(summary = "查询用户的排除标签", description = "返回 memory_tags.polarity = -1 的标签")
    @GetMapping("/excluded-tags")
    public ApiResponse<ExcludedTagsVO> excludedTags() {
        return ApiResponse.ok(recommendationService.excludedTags(UserContext.require()));
    }

    @Operation(summary = "查询用户的 location 标签", description = "返回 category=location 且 polarity=1 的标签")
    @GetMapping("/location-tags")
    public ApiResponse<LocationTagsVO> locationTags() {
        return ApiResponse.ok(recommendationService.locationTags(UserContext.require()));
    }
}
