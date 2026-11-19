package com.yuzhijie.career.trace.module.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.AfterCommit;
import com.yuzhijie.career.trace.module.interview.entity.InterviewMemoryCandidate;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewSessionMapper;
import com.yuzhijie.career.trace.module.interview.vo.InterviewIgnoreResultVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewImportResultVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewMemoryCandidateVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewMemoryCandidatesVO;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagInput;
import com.yuzhijie.career.trace.module.memory.service.ExternalMemoryImportService;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewMemoryService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMemoryCandidateMapper candidateMapper;
    private final TagMapper tagMapper;
    private final ExternalMemoryImportService memoryImportService;
    private final SkillProfileService skillProfileService;

    public InterviewMemoryCandidatesVO candidates(Long userId, Long sessionId) {
        InterviewSession session = requireFinished(userId, sessionId);
        Map<Long, Tag> tags = loadTags(sessionId);
        List<InterviewMemoryCandidateVO> list = candidateMapper.selectList(
                        Wrappers.<InterviewMemoryCandidate>lambdaQuery()
                                .eq(InterviewMemoryCandidate::getSessionId, sessionId)
                                .orderByAsc(InterviewMemoryCandidate::getId))
                .stream().map(candidate -> toVO(candidate, tags.get(candidate.getTagId()))).toList();
        return new InterviewMemoryCandidatesVO(session.getId(), list);
    }

    @Transactional
    public InterviewImportResultVO confirm(Long userId, Long sessionId, List<Long> candidateIds) {
        requireFinished(userId, sessionId);
        List<Long> memoryIds = new ArrayList<>();
        int skipped = 0;
        if (candidateIds != null) {
            for (Long candidateId : candidateIds) {
                InterviewMemoryCandidate candidate = candidateMapper.selectById(candidateId);
                if (candidate == null || !sessionId.equals(candidate.getSessionId())
                        || !userId.equals(candidate.getUserId())) {
                    throw BizException.notFound(ErrorCodes.INTERVIEW_CANDIDATE_NOT_FOUND, "记忆候选不存在");
                }
                if (candidate.getStatus() == null || candidate.getStatus() != 0) {
                    skipped++;
                    continue;
                }
                MemoryTagInput tag = new MemoryTagInput();
                Tag tagEntity = tagMapper.selectById(candidate.getTagId());
                tag.setName(tagEntity == null ? "面试技能" : tagEntity.getName());
                tag.setCategory(tagEntity == null ? "skill" : tagEntity.getCategory());
                tag.setProficiency(candidate.getProficiency());
                tag.setConfidence(candidate.getConfidence());
                tag.setEvidence(candidate.getEvidence());
                Long memoryId = memoryImportService.importMemory(userId, candidate.getContent(),
                        "interview", sessionId, List.of(tag));
                candidate.setStatus(1);
                candidate.setMemoryId(memoryId);
                candidateMapper.updateById(candidate);
                memoryIds.add(memoryId);
            }
        }
        if (!memoryIds.isEmpty()) {
            AfterCommit.run(() -> skillProfileService.recomputeAsync(userId));
        }
        return new InterviewImportResultVO(sessionId, memoryIds.size(), skipped, memoryIds);
    }

    @Transactional
    public InterviewIgnoreResultVO ignore(Long userId, Long sessionId, List<Long> candidateIds) {
        requireFinished(userId, sessionId);
        int ignored = 0;
        int skipped = 0;
        if (candidateIds != null) {
            for (Long candidateId : candidateIds) {
                InterviewMemoryCandidate candidate = candidateMapper.selectById(candidateId);
                if (candidate == null || !sessionId.equals(candidate.getSessionId())
                        || !userId.equals(candidate.getUserId())) {
                    throw BizException.notFound(ErrorCodes.INTERVIEW_CANDIDATE_NOT_FOUND, "记忆候选不存在");
                }
                if (candidate.getStatus() == null || candidate.getStatus() != 0) {
                    skipped++;
                    continue;
                }
                candidate.setStatus(2);
                candidateMapper.updateById(candidate);
                ignored++;
            }
        }
        return new InterviewIgnoreResultVO(sessionId, ignored, skipped);
    }

    private InterviewSession requireFinished(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BizException.notFound(ErrorCodes.INTERVIEW_NOT_FOUND, "面试会话不存在");
        }
        if (session.getStatus() == null || session.getStatus() != 3) {
            throw BizException.conflict(ErrorCodes.INTERVIEW_NOT_READY, "请等待评估完成");
        }
        return session;
    }

    private Map<Long, Tag> loadTags(Long sessionId) {
        List<InterviewMemoryCandidate> candidates = candidateMapper.selectList(
                Wrappers.<InterviewMemoryCandidate>lambdaQuery()
                        .eq(InterviewMemoryCandidate::getSessionId, sessionId));
        List<Long> ids = candidates.stream().map(InterviewMemoryCandidate::getTagId).distinct().toList();
        Map<Long, Tag> tags = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Tag tag : tagMapper.selectBatchIds(ids)) {
                tags.put(tag.getId(), tag);
            }
        }
        return tags;
    }

    private InterviewMemoryCandidateVO toVO(InterviewMemoryCandidate candidate, Tag tag) {
        return new InterviewMemoryCandidateVO(candidate.getId(), candidate.getTagId(),
                tag == null ? "技能" : tag.getName(), candidate.getContent(),
                candidate.getProficiency(), candidate.getConfidence(), candidate.getEvidence(),
                candidate.getStatus());
    }
}
