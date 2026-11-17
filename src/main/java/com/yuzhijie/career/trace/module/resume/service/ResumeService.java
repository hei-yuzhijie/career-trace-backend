package com.yuzhijie.career.trace.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.AfterCommit;
import com.yuzhijie.career.trace.common.util.Hashes;
import com.yuzhijie.career.trace.common.util.Pages;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.entity.ResumeAdvice;
import com.yuzhijie.career.trace.module.resume.entity.ResumeMemoryCandidate;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeAdviceMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.resume.model.ResumeDiagnosisPayload;
import com.yuzhijie.career.trace.module.resume.vo.ResumeAdviceVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDetailVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDimensionVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeListItemVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResumeMapper resumeMapper;
    private final ResumeAdviceMapper resumeAdviceMapper;
    private final ResumeMemoryCandidateMapper resumeMemoryCandidateMapper;
    private final ResumeStorageService storageService;
    private final ResumeTextExtractor textExtractor;
    private final ResumeAnalysisService analysisService;
    private final ResumePdfService pdfService;

    @Transactional
    public ResumeUploadVO upload(Long userId, MultipartFile file, Long jobId) {
        storageService.validate(file);
        String hash = Hashes.sha256(readBytes(file));
        Resume existing = resumeMapper.selectByUserAndHash(userId, hash);
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                return new ResumeUploadVO(existing.getId(), existing.getStatus(), statusText(existing.getStatus()));
            }
            if (existing.getStatus() != null && (existing.getStatus() == 0 || existing.getStatus() == 1)) {
                throw BizException.conflict(ErrorCodes.RESUME_PROCESSING, "正在分析中，请稍候");
            }
            // FAILED：复用原记录，替换文件与文本后重试
            ResumeStorageService.StoredFile stored = storageService.store(file, userId);
            String text = textExtractor.extract(stored.path());
            resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                    .eq(Resume::getId, existing.getId())
                    .set(Resume::getJobId, jobId)
                    .set(Resume::getFileKey, stored.fileKey())
                    .set(Resume::getOriginalFilename, file.getOriginalFilename())
                    .set(Resume::getContentType, file.getContentType())
                    .set(Resume::getFileHash, hash)
                    .set(Resume::getTextContent, text)
                    .set(Resume::getStatus, 0)
                    .set(Resume::getOverallScore, null)
                    .set(Resume::getAnalysisJson, null)
                    .set(Resume::getRetryCount, 0)
                    .set(Resume::getUpdatedAt, LocalDateTime.now()));
            storageService.delete(existing.getFileKey());
            afterCommitAnalyze(existing.getId());
            return new ResumeUploadVO(existing.getId(), 0, statusText(0));
        }

        ResumeStorageService.StoredFile stored = storageService.store(file, userId);
        String text = textExtractor.extract(stored.path());

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setJobId(jobId);
        resume.setFileHash(hash);
        resume.setOriginalFilename(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setFileKey(stored.fileKey());
        resume.setTextContent(text);
        resume.setStatus(0);
        resume.setRetryCount(0);
        resume.setPromptVersion("v0.1");
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        afterCommitAnalyze(resume.getId());
        return new ResumeUploadVO(resume.getId(), 0, statusText(0));
    }

    public PageResult<ResumeListItemVO> page(Long userId, Integer page, Integer size,
                                             Integer status, String keyword) {
        int pageNo = Pages.page(page);
        int pageSize = Pages.size(size);
        LambdaQueryWrapper<Resume> query = Wrappers.<Resume>lambdaQuery()
                .eq(Resume::getUserId, userId)
                .eq(status != null, Resume::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Resume::getOriginalFilename, keyword)
                .orderByDesc(Resume::getCreatedAt)
                .orderByDesc(Resume::getId);
        Page<Resume> result = new Page<>(pageNo, pageSize);
        resumeMapper.selectPage(result, query);
        List<ResumeListItemVO> records = result.getRecords().stream()
                .map(item -> new ResumeListItemVO(item.getId(), item.getOriginalFilename(),
                        item.getStatus(), item.getOverallScore(), item.getRetryCount(), item.getCreatedAt()))
                .toList();
        return new PageResult<>(result.getTotal(), pageNo, pageSize, records);
    }

    public ResumeDetailVO detail(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setResumeId(resume.getId());
        detail.setOriginalFilename(resume.getOriginalFilename());
        detail.setJobId(resume.getJobId());
        detail.setStatus(resume.getStatus());
        detail.setOverallScore(resume.getOverallScore());
        detail.setCreatedAt(resume.getCreatedAt());

        ResumeDiagnosisPayload payload = parseAnalysis(resume.getAnalysisJson());
        if (payload != null) {
            detail.setSummary(payload.getSummary());
            detail.setStrengths(payload.getStrengths());
            detail.setDimensions(toDimensions(payload));
        }
        detail.setAdvices(resumeAdviceMapper.selectByResumeId(resumeId).stream()
                .map(this::toAdviceVO).toList());
        return detail;
    }

    @Transactional
    public ResumeUploadVO reanalyze(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        if (resume.getStatus() != null && (resume.getStatus() == 0 || resume.getStatus() == 1)) {
            throw BizException.conflict(ErrorCodes.RESUME_PROCESSING, "正在分析中，请稍候");
        }
        resumeAdviceMapper.delete(Wrappers.<ResumeAdvice>lambdaQuery()
                .eq(ResumeAdvice::getResumeId, resumeId));
        resumeMemoryCandidateMapper.delete(Wrappers.<ResumeMemoryCandidate>lambdaQuery()
                .eq(ResumeMemoryCandidate::getResumeId, resumeId));
        resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .set(Resume::getStatus, 0)
                .set(Resume::getOverallScore, null)
                .set(Resume::getAnalysisJson, null)
                .set(Resume::getRetryCount, 0)
                .set(Resume::getUpdatedAt, LocalDateTime.now()));
        afterCommitAnalyze(resumeId);
        return new ResumeUploadVO(resumeId, 0, statusText(0));
    }

    @Transactional
    public void delete(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        if (resume.getStatus() != null && (resume.getStatus() == 0 || resume.getStatus() == 1)) {
            throw BizException.conflict(ErrorCodes.RESUME_PROCESSING, "正在分析中，请稍候");
        }
        resumeAdviceMapper.delete(Wrappers.<ResumeAdvice>lambdaQuery()
                .eq(ResumeAdvice::getResumeId, resumeId));
        resumeMemoryCandidateMapper.delete(Wrappers.<ResumeMemoryCandidate>lambdaQuery()
                .eq(ResumeMemoryCandidate::getResumeId, resumeId));
        resumeMapper.deleteById(resumeId);
        storageService.delete(resume.getFileKey());
    }

    public byte[] report(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        if (resume.getStatus() == null || resume.getStatus() != 2) {
            throw BizException.conflict(ErrorCodes.RESUME_REPORT_NOT_READY, "请等待诊断完成后再导出");
        }
        ResumeDetailVO detail = detail(userId, resumeId);
        return pdfService.render(resume, detail);
    }

    public Resume requireResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BizException.notFound(ErrorCodes.RESUME_NOT_FOUND, "简历不存在或已删除");
        }
        return resume;
    }

    private void afterCommitAnalyze(Long resumeId) {
        AfterCommit.run(() -> analysisService.analyzeAsync(resumeId));
    }

    private ResumeDiagnosisPayload parseAnalysis(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ResumeDiagnosisPayload.class);
        } catch (Exception e) {
            log.warn("解析简历分析 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private List<ResumeDimensionVO> toDimensions(ResumeDiagnosisPayload payload) {
        ResumeDiagnosisPayload.ScoreDetail score = payload.getScoreDetail();
        if (score == null) {
            return List.of();
        }
        List<ResumeDimensionVO> dimensions = new ArrayList<>();
        dimensions.add(new ResumeDimensionVO("经历/项目深度", nvl(score.getExperienceDepth()), 30, null));
        dimensions.add(new ResumeDimensionVO("成果量化", nvl(score.getAchievement()), 20, null));
        dimensions.add(new ResumeDimensionVO("技能匹配", nvl(score.getSkillMatch()), 20, null));
        dimensions.add(new ResumeDimensionVO("内容完整性", nvl(score.getContent()), 15, null));
        dimensions.add(new ResumeDimensionVO("结构/表达", nvl(score.getExpression()), 15, null));
        return dimensions;
    }

    private ResumeAdviceVO toAdviceVO(ResumeAdvice advice) {
        return new ResumeAdviceVO(advice.getCategory(), advice.getPriority(), advice.getIssue(),
                advice.getRecommendation(), advice.getEvidence());
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID, "读取文件失败");
        }
    }

    private static String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "PROCESSING";
            case 2 -> "DONE";
            case 3 -> "FAILED";
            default -> "UNKNOWN";
        };
    }
}
