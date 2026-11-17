package com.yuzhijie.career.trace.module.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.entity.ResumeAdvice;
import com.yuzhijie.career.trace.module.resume.entity.ResumeMemoryCandidate;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeAdviceMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMemoryCandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历分析结果落库（事务内）：清旧建议/候选，写新结果，更新 resume 终态。
 */
@Service
@RequiredArgsConstructor
public class ResumeAnalysisWriter {

    private final ResumeMapper resumeMapper;
    private final ResumeAdviceMapper resumeAdviceMapper;
    private final ResumeMemoryCandidateMapper resumeMemoryCandidateMapper;

    @Transactional
    public void saveSuccess(Long resumeId, Integer overallScore, String analysisJson,
                            String attemptId, List<ResumeAdvice> advices,
                            List<ResumeMemoryCandidate> candidates) {
        resumeAdviceMapper.delete(Wrappers.<ResumeAdvice>lambdaQuery()
                .eq(ResumeAdvice::getResumeId, resumeId));
        resumeMemoryCandidateMapper.delete(Wrappers.<ResumeMemoryCandidate>lambdaQuery()
                .eq(ResumeMemoryCandidate::getResumeId, resumeId));

        for (ResumeAdvice advice : advices) {
            resumeAdviceMapper.insert(advice);
        }
        for (ResumeMemoryCandidate candidate : candidates) {
            resumeMemoryCandidateMapper.insert(candidate);
        }

        Resume update = new Resume();
        update.setStatus(2);
        update.setOverallScore(overallScore);
        update.setAnalysisJson(analysisJson);
        update.setUpdatedAt(LocalDateTime.now());
        resumeMapper.update(update, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .eq(attemptId != null, Resume::getAttemptId, attemptId));
    }

    @Transactional
    public void markFailed(Long resumeId, int retryCount, String attemptId) {
        resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .set(Resume::getStatus, 3)
                .set(Resume::getRetryCount, retryCount)
                .set(Resume::getUpdatedAt, LocalDateTime.now()));
    }
}
