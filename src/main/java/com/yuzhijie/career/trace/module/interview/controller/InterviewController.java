package com.yuzhijie.career.trace.module.interview.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.module.interview.dto.ConfirmInterviewMemoriesRequest;
import com.yuzhijie.career.trace.module.interview.dto.InterviewStartRequest;
import com.yuzhijie.career.trace.module.interview.dto.SaveAnswerRequest;
import com.yuzhijie.career.trace.module.interview.service.InterviewMemoryService;
import com.yuzhijie.career.trace.module.interview.service.InterviewService;
import com.yuzhijie.career.trace.module.interview.vo.InterviewAnswerVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewIgnoreResultVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewImportResultVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewMemoryCandidatesVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewSessionDetailVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewSessionListItemVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewStartVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "模拟面试", description = "模拟卷：异步出题、逐题保存答案、异步评估、记忆候选")
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewMemoryService memoryService;

    @Operation(summary = "发起模拟面试", description = "异步出题，返回 202 + sessionId；前端轮询详情")
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewStartVO>> start(
            @RequestBody InterviewStartRequest request, HttpServletResponse response) {
        InterviewStartVO result = interviewService.start(UserContext.require(), request);
        response.setHeader("Retry-After", "2");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(result));
    }

    @Operation(summary = "分页查询会话")
    @GetMapping
    public ApiResponse<PageResult<InterviewSessionListItemVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long tagId) {
        return ApiResponse.ok(interviewService.page(UserContext.require(), page, size, status, tagId));
    }

    @Operation(summary = "会话详情/轮询")
    @GetMapping("/{sessionId}")
    public ApiResponse<InterviewSessionDetailVO> detail(
            @PathVariable Long sessionId, HttpServletResponse response) {
        InterviewSessionDetailVO detail = interviewService.detail(UserContext.require(), sessionId);
        if (detail.getStatus() != null && (detail.getStatus() == 0 || detail.getStatus() == 2)) {
            response.setHeader("Retry-After", "2");
        }
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "删除模拟面试会话", description = "出题中/评估中不可删除；删除后题目、作答、评估报告与记忆候选一并清除")
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(@PathVariable Long sessionId) {
        interviewService.delete(UserContext.require(), sessionId);
        return ApiResponse.ok();
    }

    @Operation(summary = "保存单题答案", description = "可反复保存/修改；仅 READY 状态可写")
    @PutMapping("/{sessionId}/answers/{questionIndex}")
    public ApiResponse<InterviewAnswerVO> saveAnswer(
            @PathVariable Long sessionId,
            @PathVariable Integer questionIndex,
            @RequestBody SaveAnswerRequest request) {
        return ApiResponse.ok(interviewService.saveAnswer(UserContext.require(), sessionId,
                questionIndex, request == null ? null : request.getAnswerText()));
    }

    @Operation(summary = "查询已保存答案")
    @GetMapping("/{sessionId}/answers")
    public ApiResponse<List<InterviewAnswerVO>> answers(@PathVariable Long sessionId) {
        return ApiResponse.ok(interviewService.answers(UserContext.require(), sessionId));
    }

    @Operation(summary = "提交评估", description = "使用已保存答案，异步评估，返回 202")
    @PostMapping("/{sessionId}/evaluate")
    public ResponseEntity<ApiResponse<InterviewStartVO>> evaluate(
            @PathVariable Long sessionId, HttpServletResponse response) {
        InterviewStartVO result = interviewService.evaluate(UserContext.require(), sessionId);
        response.setHeader("Retry-After", "2");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(result));
    }

    @Operation(summary = "查询面试记忆候选", description = "仅 FINISHED 可查")
    @GetMapping("/{sessionId}/memory-candidates")
    public ApiResponse<InterviewMemoryCandidatesVO> memoryCandidates(@PathVariable Long sessionId) {
        return ApiResponse.ok(memoryService.candidates(UserContext.require(), sessionId));
    }

    @Operation(summary = "确认写入职业记忆", description = "仅用户确认的候选才写 memory_items")
    @PostMapping("/{sessionId}/confirm-memories")
    public ApiResponse<InterviewImportResultVO> confirmMemories(
            @PathVariable Long sessionId,
            @RequestBody ConfirmInterviewMemoriesRequest request) {
        return ApiResponse.ok(memoryService.confirm(UserContext.require(), sessionId,
                request == null ? null : request.getCandidateIds()));
    }

    @Operation(summary = "忽略面试记忆候选", description = "将待确认候选状态置为 2（已忽略），详情页待确认候选数随之更新")
    @PostMapping("/{sessionId}/ignore-memories")
    public ApiResponse<InterviewIgnoreResultVO> ignoreMemories(
            @PathVariable Long sessionId,
            @RequestBody ConfirmInterviewMemoriesRequest request) {
        return ApiResponse.ok(memoryService.ignore(UserContext.require(), sessionId,
                request == null ? null : request.getCandidateIds()));
    }
}
