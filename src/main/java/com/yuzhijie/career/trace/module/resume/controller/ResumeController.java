package com.yuzhijie.career.trace.module.resume.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.module.resume.dto.ConfirmResumeMemoriesRequest;
import com.yuzhijie.career.trace.module.resume.service.ResumeMemoryService;
import com.yuzhijie.career.trace.module.resume.service.ResumeService;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDetailVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeIgnoreResultVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeImportResultVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeListItemVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeMemoryCandidatesVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeUploadVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "简历诊断", description = "简历上传、异步诊断、PDF 报告、记忆候选导入")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeMemoryService resumeMemoryService;

    @Operation(summary = "上传简历", description = "支持 PDF/DOCX/TXT/MD，文件落本地 storage/resumes/，异步分析")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeUploadVO>> upload(
            @Parameter(description = "简历文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "可选目标岗位 ID") @RequestParam(value = "jobId", required = false) Long jobId) {
        ResumeUploadVO result = resumeService.upload(UserContext.require(), file, jobId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(result));
    }

    @Operation(summary = "分页查询简历")
    @GetMapping
    public ApiResponse<PageResult<ResumeListItemVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(resumeService.page(UserContext.require(), page, size, status, keyword));
    }

    @Operation(summary = "查询简历详情/轮询状态")
    @GetMapping("/{resumeId}")
    public ApiResponse<ResumeDetailVO> detail(@PathVariable Long resumeId) {
        return ApiResponse.ok(resumeService.detail(UserContext.require(), resumeId));
    }

    @Operation(summary = "重新分析")
    @PostMapping("/{resumeId}/reanalyze")
    public ResponseEntity<ApiResponse<ResumeUploadVO>> reanalyze(@PathVariable Long resumeId) {
        ResumeUploadVO result = resumeService.reanalyze(UserContext.require(), resumeId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(result));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> delete(@PathVariable Long resumeId) {
        resumeService.delete(UserContext.require(), resumeId);
        return ApiResponse.ok();
    }

    @Operation(summary = "导出 PDF 诊断报告")
    @GetMapping(value = "/{resumeId}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@PathVariable Long resumeId) {
        byte[] pdf = resumeService.report(UserContext.require(), resumeId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resume-report-" + resumeId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "查询简历转记忆候选")
    @GetMapping("/{resumeId}/memory-candidates")
    public ApiResponse<ResumeMemoryCandidatesVO> memoryCandidates(@PathVariable Long resumeId) {
        return ApiResponse.ok(resumeMemoryService.candidates(UserContext.require(), resumeId));
    }

    @Operation(summary = "确认导入简历记忆")
    @PostMapping("/{resumeId}/confirm-memories")
    public ApiResponse<ResumeImportResultVO> confirmMemories(
            @PathVariable Long resumeId,
            @RequestBody ConfirmResumeMemoriesRequest request) {
        return ApiResponse.ok(resumeMemoryService.confirm(UserContext.require(), resumeId,
                request == null ? null : request.getCandidateIds()));
    }

    @Operation(summary = "忽略简历记忆候选", description = "将待确认候选状态置为 2（已忽略）")
    @PostMapping("/{resumeId}/ignore-memories")
    public ApiResponse<ResumeIgnoreResultVO> ignoreMemories(
            @PathVariable Long resumeId,
            @RequestBody ConfirmResumeMemoriesRequest request) {
        return ApiResponse.ok(resumeMemoryService.ignore(UserContext.require(), resumeId,
                request == null ? null : request.getCandidateIds()));
    }
}
