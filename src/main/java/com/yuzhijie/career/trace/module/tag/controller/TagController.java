package com.yuzhijie.career.trace.module.tag.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import com.yuzhijie.career.trace.module.tag.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "标签字典", description = "标准标签只读查询")
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagMapper tagMapper;

    @Operation(summary = "查询启用中的标准标签", description = "可选按 category 过滤（如 skill/location/preference）")
    @GetMapping
    public ApiResponse<List<TagVO>> list(
            @Parameter(description = "标签类别，例如 skill", example = "skill")
            @RequestParam(required = false) String category) {
        String trimmedCategory = category == null ? null : category.trim();
        List<TagVO> tags = tagMapper.selectList(
                        Wrappers.<Tag>lambdaQuery()
                                .eq(Tag::getStatus, 1)
                                .eq(trimmedCategory != null && !trimmedCategory.isEmpty(),
                                        Tag::getCategory, trimmedCategory)
                                .orderByAsc(Tag::getId))
                .stream()
                .map(tag -> new TagVO(tag.getId(), tag.getName(), tag.getSlug(), tag.getCategory()))
                .toList();
        return ApiResponse.ok(tags);
    }
}
