package com.yuzhijie.career.trace.module.memory.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.module.memory.dto.CreateMemoryRequest;
import com.yuzhijie.career.trace.module.memory.dto.UpdateMemoryRequest;
import com.yuzhijie.career.trace.module.memory.dto.UpdateMemoryTagRequest;
import com.yuzhijie.career.trace.module.memory.service.MemoryService;
import com.yuzhijie.career.trace.module.memory.vo.DeleteMemoryTagVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryDeleteVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryDetailVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryListItemVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryTagVO;
import com.yuzhijie.career.trace.module.memory.vo.MemoryUpdateVO;
import com.yuzhijie.career.trace.module.memory.vo.TaggingResultVO;
import com.yuzhijie.career.trace.module.memory.vo.UpdateMemoryTagVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "记忆库", description = "职业记忆 CRUD、异步大模型打标、标签手动修正")
@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @Operation(summary = "新增记忆", description = "写入 memory_items 后异步调用大模型抽取标签，接口立即返回 memoryId 与 PENDING")
    @PostMapping
    public ApiResponse<TaggingResultVO> create(@RequestBody CreateMemoryRequest request) {
        return ApiResponse.ok(memoryService.create(UserContext.require(), request));
    }

    @Operation(summary = "分页查询记忆列表")
    @GetMapping
    public ApiResponse<PageResult<MemoryListItemVO>> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大 50", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "来源类型：resume/interview/manual/review", example = "manual")
            @RequestParam(required = false) String sourceType,
            @Parameter(description = "打标状态：PENDING/DONE/FAILED", example = "DONE")
            @RequestParam(required = false) String taggingStatus,
            @Parameter(description = "原文模糊匹配关键字", example = "小红书")
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(memoryService.page(UserContext.require(), page, size, sourceType, taggingStatus, keyword));
    }

    @Operation(summary = "查询记忆详情")
    @GetMapping("/{memoryId}")
    public ApiResponse<MemoryDetailVO> detail(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId) {
        return ApiResponse.ok(memoryService.detail(UserContext.require(), memoryId));
    }

    @Operation(summary = "更新记忆原文", description = "只改原文、不自动重打标签；needRetag=true 时由前端询问用户是否重新分析")
    @PutMapping("/{memoryId}")
    public ApiResponse<MemoryUpdateVO> update(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId,
            @RequestBody UpdateMemoryRequest request) {
        return ApiResponse.ok(memoryService.updateContent(UserContext.require(), memoryId, request));
    }

    @Operation(summary = "删除记忆（软删除）", description = "置 status=0，删除关联 memory_tags，并异步重算 skill 画像")
    @DeleteMapping("/{memoryId}")
    public ApiResponse<MemoryDeleteVO> delete(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId) {
        return ApiResponse.ok(memoryService.delete(UserContext.require(), memoryId));
    }

    @Operation(summary = "重新打标", description = "删除旧标签并重新调用大模型抽取；打标进行中返回 40901")
    @PostMapping("/{memoryId}/retag")
    public ApiResponse<TaggingResultVO> retag(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId) {
        return ApiResponse.ok(memoryService.retag(UserContext.require(), memoryId));
    }

    @Operation(summary = "查询记忆的标签")
    @GetMapping("/{memoryId}/tags")
    public ApiResponse<List<MemoryTagVO>> tags(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId,
            @Parameter(description = "按类别过滤，如 skill", example = "skill")
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(memoryService.tags(UserContext.require(), memoryId, category));
    }

    @Operation(summary = "手动修正标签熟练度/极性", description = "覆盖 memory_tags 对应行，assessed_by 置为 manual，并异步重算 skill 画像")
    @PutMapping("/{memoryId}/tags/{tagId}")
    public ApiResponse<UpdateMemoryTagVO> updateTag(
            @Parameter(description = "记忆 ID", required = true, example = "3")
            @PathVariable Long memoryId,
            @Parameter(description = "标签 ID", required = true, example = "106")
            @PathVariable Long tagId,
            @RequestBody UpdateMemoryTagRequest request) {
        return ApiResponse.ok(memoryService.updateTag(UserContext.require(), memoryId, tagId, request));
    }

    @Operation(summary = "删除记忆上的某个标签", description = "用户认为 AI 误判时删除，并异步重算 skill 画像")
    @DeleteMapping("/{memoryId}/tags/{tagId}")
    public ApiResponse<DeleteMemoryTagVO> deleteTag(
            @Parameter(description = "记忆 ID", required = true, example = "1")
            @PathVariable Long memoryId,
            @Parameter(description = "标签 ID", required = true, example = "104")
            @PathVariable Long tagId) {
        return ApiResponse.ok(memoryService.deleteTag(UserContext.require(), memoryId, tagId));
    }
}
