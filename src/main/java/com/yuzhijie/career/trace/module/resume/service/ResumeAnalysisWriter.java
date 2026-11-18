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
 * 简历诊断结果落库。成功时先清旧建议和旧候选，再写新结果，最后把简历状态改为已完成；
 * 失败时只把状态改为失败并累加重试次数。
 */
@Service
@RequiredArgsConstructor
public class ResumeAnalysisWriter {

    private final ResumeMapper resumeMapper;
    private final ResumeAdviceMapper resumeAdviceMapper;
    private final ResumeMemoryCandidateMapper resumeMemoryCandidateMapper;

    /**
     * 写诊断成功的结果。事务内完成：删旧建议、删旧候选、插新建议、插新候选、更新简历状态。
     * 更新简历时带 attemptId 条件，只有当前任务仍是接管者时才生效。
     */
    @Transactional
    public void saveSuccess(Long resumeId, Integer overallScore, String analysisJson,
                            String attemptId, List<ResumeAdvice> advices,
                            List<ResumeMemoryCandidate> candidates) {
        // 先清掉该简历上一次诊断留下的建议，避免新旧混在一起
        resumeAdviceMapper.delete(Wrappers.<ResumeAdvice>lambdaQuery()
                .eq(ResumeAdvice::getResumeId, resumeId));
        // 同样清掉旧的记忆候选
        resumeMemoryCandidateMapper.delete(Wrappers.<ResumeMemoryCandidate>lambdaQuery()
                .eq(ResumeMemoryCandidate::getResumeId, resumeId));

        // 插入本次诊断生成的新建议
        for (ResumeAdvice advice : advices) {
            resumeAdviceMapper.insert(advice);
        }
        // 插入本次诊断生成的新记忆候选，status=0 表示待用户确认
        for (ResumeMemoryCandidate candidate : candidates) {
            resumeMemoryCandidateMapper.insert(candidate);
        }

        // 更新简历为已完成，写入总分和完整诊断 JSON
        Resume update = new Resume();
        update.setStatus(2);
        update.setOverallScore(overallScore);
        update.setAnalysisJson(analysisJson);
        update.setUpdatedAt(LocalDateTime.now());
        // 带 attemptId 条件：如果简历已被其他任务接管，这次更新不会生效
        resumeMapper.update(update, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .eq(attemptId != null, Resume::getAttemptId, attemptId));
    }

    /**
     * 标记诊断失败。把状态改为 3，写入重试次数，更新更新时间。
     */
    @Transactional
    public void markFailed(Long resumeId, int retryCount, String attemptId) {
        resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .set(Resume::getStatus, 3)
                .set(Resume::getRetryCount, retryCount)
                .set(Resume::getUpdatedAt, LocalDateTime.now()));
    }
}