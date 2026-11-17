package com.yuzhijie.career.trace.module.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.integration.llm.DeepSeekClient;
import com.yuzhijie.career.trace.integration.llm.LlmException;
import com.yuzhijie.career.trace.integration.llm.LlmJson;
import com.yuzhijie.career.trace.integration.llm.PromptLoader;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.job.mapper.JobTagMapper;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.entity.ResumeAdvice;
import com.yuzhijie.career.trace.module.resume.entity.ResumeMemoryCandidate;
import com.yuzhijie.career.trace.module.resume.model.ResumeDiagnosisPayload;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 简历异步诊断：Tika 文本 -> Prompt -> DeepSeek -> 结构化结果落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;
    private final ResumeAnalysisWriter analysisWriter;
    private final ResumeStorageService storageService;
    private final ResumeTextExtractor textExtractor;

    @Value("${career-trace.resume.diagnosis-prompt:classpath:prompts/resume/resume-diagnosis.md}")
    private String promptLocation;

    @Value("${career-trace.resume.max-retry:3}")
    private int maxAttempts;

    @Async("taskExecutor")
    public void analyzeAsync(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getStatus() == null || resume.getStatus() != 0) {
            return;
        }
        String attemptId = UUID.randomUUID().toString();
        resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                .eq(Resume::getId, resumeId)
                .eq(Resume::getStatus, 0)
                .set(Resume::getStatus, 1)
                .set(Resume::getAttemptId, attemptId)
                .set(Resume::getUpdatedAt, LocalDateTime.now()));

        try {
            String text = resume.getTextContent();
            if (text == null || text.isBlank()) {
                var path = storageService.resolve(resume.getFileKey());
                if (path == null) {
                    throw new BizException(50003, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                            "简历文件不存在");
                }
                text = textExtractor.extract(path);
                resumeMapper.update(null, Wrappers.<Resume>lambdaUpdate()
                        .eq(Resume::getId, resumeId)
                        .set(Resume::getTextContent, text));
            }

            String jobSection = buildJobSection(resume.getJobId());
            String prompt = promptLoader.load(promptLocation)
                    .replace("{resumeText}", text)
                    .replace("{targetJobSection}", jobSection)
                    .replace("{jobTagsSection}", jobSection);

            String content = callWithRetry(prompt);
            String json = LlmJson.extractObject(content);
            ResumeDiagnosisPayload payload = MAPPER.readValue(json, ResumeDiagnosisPayload.class);
            if (payload.getScoreDetail() == null) {
                throw new IllegalStateException("诊断结果缺少 scoreDetail");
            }
            clampScore(payload);
            int overall = overall(payload.getScoreDetail());

            List<ResumeAdvice> advices = toAdvices(resumeId, payload.getSuggestions());
            List<ResumeMemoryCandidate> candidates = toCandidates(resume, payload.getMemories());
            String normalizedJson = MAPPER.writeValueAsString(payload);

            analysisWriter.saveSuccess(resumeId, overall, normalizedJson, attemptId, advices, candidates);
            log.info("简历诊断完成 resumeId={}, score={}", resumeId, overall);
        } catch (Exception e) {
            log.warn("简历诊断失败 resumeId={}: {}", resumeId, e.getMessage(), e);
            analysisWriter.markFailed(resumeId, (resume.getRetryCount() == null ? 0 : resume.getRetryCount()) + 1,
                    attemptId);
        }
    }

    private String callWithRetry(String prompt) {
        LlmException last = null;
        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            try {
                return deepSeekClient.completeJson(prompt, "请严格按系统提示输出 JSON。");
            } catch (LlmException e) {
                last = e;
                if (!e.isRetryable() || attempt >= maxAttempts) {
                    break;
                }
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last == null ? new LlmException("简历诊断调用失败") : last;
    }

    private String buildJobSection(Long jobId) {
        if (jobId == null) {
            return "未指定目标岗位，请按通用职业画像评估。";
        }
        JobRow job = jobMapper.selectJobRowById(jobId);
        if (job == null) {
            return "目标岗位不存在，请按通用职业画像评估。";
        }
        List<JobTagRow> tags = jobTagMapper.selectByJobId(jobId);
        StringBuilder builder = new StringBuilder();
        builder.append("岗位：").append(nullSafe(job.getTitle()))
                .append("；公司：").append(nullSafe(job.getCompanyName()))
                .append("；城市：").append(nullSafe(job.getCity()))
                .append("；JD：").append(nullSafe(job.getJdText()));
        if (!tags.isEmpty()) {
            builder.append("；岗位要求：");
            for (JobTagRow tag : tags) {
                builder.append(tag.getTagName()).append("（").append(tag.getWeight()).append("）;");
            }
        }
        return builder.toString();
    }

    private static void clampScore(ResumeDiagnosisPayload payload) {
        ResumeDiagnosisPayload.ScoreDetail d = payload.getScoreDetail();
        d.setExperienceDepth(clamp(d.getExperienceDepth(), 0, 30));
        d.setAchievement(clamp(d.getAchievement(), 0, 20));
        d.setSkillMatch(clamp(d.getSkillMatch(), 0, 20));
        d.setContent(clamp(d.getContent(), 0, 15));
        d.setExpression(clamp(d.getExpression(), 0, 15));
    }

    private static int overall(ResumeDiagnosisPayload.ScoreDetail d) {
        int total = d.getExperienceDepth() + d.getAchievement() + d.getSkillMatch()
                + d.getContent() + d.getExpression();
        return Math.max(0, Math.min(100, total));
    }

    private static Integer clamp(Integer value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static List<ResumeAdvice> toAdvices(Long resumeId,
                                                List<ResumeDiagnosisPayload.Suggestion> suggestions) {
        List<ResumeAdvice> result = new ArrayList<>();
        if (suggestions == null) {
            return result;
        }
        for (ResumeDiagnosisPayload.Suggestion suggestion : suggestions) {
            ResumeAdvice advice = new ResumeAdvice();
            advice.setResumeId(resumeId);
            advice.setCategory(suggestion.getCategory());
            advice.setPriority(suggestion.getPriority());
            advice.setIssue(suggestion.getIssue());
            advice.setRecommendation(suggestion.getRecommendation());
            advice.setEvidence(suggestion.getEvidence());
            result.add(advice);
        }
        return result;
    }

    private static List<ResumeMemoryCandidate> toCandidates(Resume resume,
                                                            List<ResumeDiagnosisPayload.MemoryCandidate> memories)
            throws Exception {
        List<ResumeMemoryCandidate> result = new ArrayList<>();
        if (memories == null) {
            return result;
        }
        for (ResumeDiagnosisPayload.MemoryCandidate memory : memories) {
            if (memory.getContent() == null || memory.getContent().isBlank()
                    || memory.getTags() == null || memory.getTags().isEmpty()) {
                continue;
            }
            ResumeMemoryCandidate candidate = new ResumeMemoryCandidate();
            candidate.setResumeId(resume.getId());
            candidate.setUserId(resume.getUserId());
            candidate.setContent(memory.getContent());
            candidate.setTagsJson(MAPPER.writeValueAsString(memory.getTags()));
            candidate.setStatus(0);
            result.add(candidate);
        }
        return result;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
