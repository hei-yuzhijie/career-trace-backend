package com.yuzhijie.career.trace.module.resume.controller;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.module.resume.service.CareerDiagnosisService;
import com.yuzhijie.career.trace.module.resume.vo.CareerDiagnosisVO;
import com.yuzhijie.career.trace.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "职业经历诊断", description = "基于职业记忆与可选目标岗位的诊断")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class CareerDiagnosisController {

    private final CareerDiagnosisService careerDiagnosisService;

    @Operation(summary = "职业经历诊断")
    @GetMapping("/career-diagnosis")
    public ApiResponse<CareerDiagnosisVO> diagnose(
            @Parameter(description = "可选目标岗位 ID") @RequestParam(required = false) Long jobId) {
        return ApiResponse.ok(careerDiagnosisService.diagnose(UserContext.require(), jobId));
    }
}
