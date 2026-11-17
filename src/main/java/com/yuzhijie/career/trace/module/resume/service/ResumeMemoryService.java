package com.yuzhijie.career.trace.module.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.AfterCommit;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagInput;
import com.yuzhijie.career.trace.module.memory.service.ExternalMemoryImportService;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.entity.ResumeMemoryCandidate;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.resume.model.ResumeDiagnosisPayload;
import com.yuzhijie.career.trace.module.resume.vo.ResumeImportResultVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeMemoryCandidateVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeMemoryCandidatesVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeMemoryTagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMemoryService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResumeMapper resumeMapper;
    private final ResumeMemoryCandidateMapper candidateMapper;
    private final ExternalMemoryImportService memoryImportService;
    private final SkillProfileService skillProfileService;

    public ResumeMemoryCandidatesVO candidates(Long userId, Long resumeId) {
        Resume resume = requireDone(userId, resumeId);
        List<ResumeMemoryCandidateVO> candidates = candidateMapper.selectList(
                        Wrappers.<ResumeMemoryCandidate>lambdaQuery()
                                .eq(ResumeMemoryCandidate::getResumeId, resumeId)
                                .orderByAsc(ResumeMemoryCandidate::getId))
                .stream().map(this::toVO).toList();
        return new ResumeMemoryCandidatesVO(resume.getId(), candidates);
    }

    @Transactional
    public ResumeImportResultVO confirm(Long userId, Long resumeId, List<Long> candidateIds) {
        requireDone(userId, resumeId);
        List<Long> memoryIds = new ArrayList<>();
        int skipped = 0;
        if (candidateIds != null) {
            for (Long candidateId : candidateIds) {
                ResumeMemoryCandidate candidate = candidateMapper.selectById(candidateId);
                if (candidate == null || !resumeId.equals(candidate.getResumeId())
                        || !userId.equals(candidate.getUserId())) {
                    throw BizException.notFound(ErrorCodes.RESUME_CANDIDATE_NOT_FOUND, "记忆候选不存在");
                }
                if (candidate.getStatus() == null || candidate.getStatus() != 0) {
                    skipped++;
                    continue;
                }
                Long memoryId = memoryImportService.importMemory(userId, candidate.getContent(),
                        "resume", resumeId, parseTags(candidate.getTagsJson()));
                candidate.setStatus(1);
                candidate.setMemoryId(memoryId);
                candidateMapper.updateById(candidate);
                memoryIds.add(memoryId);
            }
        }
        if (!memoryIds.isEmpty()) {
            AfterCommit.run(() -> skillProfileService.recomputeAsync(userId));
        }
        return new ResumeImportResultVO(resumeId, memoryIds.size(), skipped, memoryIds);
    }

    private Resume requireDone(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BizException.notFound(ErrorCodes.RESUME_NOT_FOUND, "简历不存在或已删除");
        }
        if (resume.getStatus() == null || resume.getStatus() != 2) {
            throw BizException.conflict(ErrorCodes.RESUME_IMPORT_NOT_READY, "请等待诊断完成后再导入");
        }
        return resume;
    }

    private ResumeMemoryCandidateVO toVO(ResumeMemoryCandidate candidate) {
        return new ResumeMemoryCandidateVO(candidate.getId(), candidate.getContent(),
                parseTagVOs(candidate.getTagsJson()), candidate.getStatus());
    }

    private List<MemoryTagInput> parseTags(String json) {
        try {
            List<ResumeDiagnosisPayload.TagCandidate> tags = MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class,
                            ResumeDiagnosisPayload.TagCandidate.class));
            List<MemoryTagInput> result = new ArrayList<>();
            for (ResumeDiagnosisPayload.TagCandidate tag : tags) {
                result.add(new MemoryTagInput(tag.getName(), tag.getCategory(),
                        tag.getProficiency(), tag.getConfidence(), tag.getEvidence()));
            }
            return result;
        } catch (Exception e) {
            log.warn("解析简历候选标签失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResumeMemoryTagVO> parseTagVOs(String json) {
        try {
            List<ResumeDiagnosisPayload.TagCandidate> tags = MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class,
                            ResumeDiagnosisPayload.TagCandidate.class));
            List<ResumeMemoryTagVO> result = new ArrayList<>();
            for (ResumeDiagnosisPayload.TagCandidate tag : tags) {
                result.add(new ResumeMemoryTagVO(tag.getName(), tag.getCategory(),
                        tag.getProficiency(), tag.getConfidence() == null
                        ? BigDecimal.valueOf(0.8) : tag.getConfidence(), tag.getEvidence()));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
}
