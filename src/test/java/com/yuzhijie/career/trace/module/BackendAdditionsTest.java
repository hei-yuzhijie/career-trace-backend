package com.yuzhijie.career.trace.module;

import com.yuzhijie.career.trace.common.api.ApiResponse;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.module.interview.entity.InterviewMemoryCandidate;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewAnswerMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewEvaluationMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewQuestionMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewSessionMapper;
import com.yuzhijie.career.trace.module.interview.service.InterviewMemoryService;
import com.yuzhijie.career.trace.module.interview.service.InterviewService;
import com.yuzhijie.career.trace.module.interview.vo.InterviewIgnoreResultVO;
import com.yuzhijie.career.trace.module.match.service.SkillProfileService;
import com.yuzhijie.career.trace.module.memory.service.ExternalMemoryImportService;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.entity.ResumeMemoryCandidate;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.resume.service.ResumeMemoryService;
import com.yuzhijie.career.trace.module.resume.vo.ResumeIgnoreResultVO;
import com.yuzhijie.career.trace.module.tag.controller.TagController;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import com.yuzhijie.career.trace.module.tag.vo.TagVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackendAdditionsTest {

    @Mock
    private TagMapper tagMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeMemoryCandidateMapper resumeCandidateMapper;
    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private InterviewQuestionMapper interviewQuestionMapper;
    @Mock
    private InterviewAnswerMapper interviewAnswerMapper;
    @Mock
    private InterviewEvaluationMapper interviewEvaluationMapper;
    @Mock
    private InterviewMemoryCandidateMapper interviewCandidateMapper;
    @Mock
    private ExternalMemoryImportService memoryImportService;
    @Mock
    private SkillProfileService skillProfileService;

    @Test
    void tagControllerListsEnabledTags() {
        Tag skill = new Tag();
        skill.setId(106L);
        skill.setName("内容策划");
        skill.setSlug("nei_rong_ce_hua");
        skill.setCategory("skill");
        skill.setStatus(1);
        when(tagMapper.selectList(any())).thenReturn(List.of(skill));

        TagController controller = new TagController(tagMapper);
        ApiResponse<List<TagVO>> response = controller.list("skill");

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals(106L, response.getData().get(0).getTagId());
        assertEquals("内容策划", response.getData().get(0).getName());
    }

    @Test
    void resumeMemoryIgnoreMarksPendingAsIgnored() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1L);
        resume.setStatus(2);
        when(resumeMapper.selectById(1L)).thenReturn(resume);

        ResumeMemoryCandidate pending = new ResumeMemoryCandidate();
        pending.setId(11L);
        pending.setResumeId(1L);
        pending.setUserId(1L);
        pending.setStatus(0);

        ResumeMemoryCandidate alreadyImported = new ResumeMemoryCandidate();
        alreadyImported.setId(12L);
        alreadyImported.setResumeId(1L);
        alreadyImported.setUserId(1L);
        alreadyImported.setStatus(1);

        when(resumeCandidateMapper.selectById(11L)).thenReturn(pending);
        when(resumeCandidateMapper.selectById(12L)).thenReturn(alreadyImported);

        ResumeMemoryService service = new ResumeMemoryService(
                resumeMapper, resumeCandidateMapper, memoryImportService, skillProfileService);
        ResumeIgnoreResultVO result = service.ignore(1L, 1L, List.of(11L, 12L));

        assertEquals(1, result.getIgnored());
        assertEquals(1, result.getSkipped());
        assertEquals(2, pending.getStatus());
        verify(resumeCandidateMapper).updateById(pending);
    }

    @Test
    void interviewMemoryIgnoreMarksPendingAsIgnored() {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setStatus(3);
        when(interviewSessionMapper.selectById(1L)).thenReturn(session);

        InterviewMemoryCandidate pending = new InterviewMemoryCandidate();
        pending.setId(21L);
        pending.setSessionId(1L);
        pending.setUserId(1L);
        pending.setStatus(0);
        when(interviewCandidateMapper.selectById(21L)).thenReturn(pending);

        InterviewMemoryService service = new InterviewMemoryService(
                interviewSessionMapper, interviewCandidateMapper, tagMapper, memoryImportService, skillProfileService);
        InterviewIgnoreResultVO result = service.ignore(1L, 1L, List.of(21L));

        assertEquals(1, result.getIgnored());
        assertEquals(0, result.getSkipped());
        assertEquals(2, pending.getStatus());
        verify(interviewCandidateMapper).updateById(pending);
    }

    @Test
    void interviewDeleteRemovesFinishedSessionAndRejectsProcessingSession() {
        InterviewSession finished = new InterviewSession();
        finished.setId(5L);
        finished.setUserId(1L);
        finished.setStatus(3);
        when(interviewSessionMapper.selectById(5L)).thenReturn(finished);

        InterviewService service = new InterviewService(
                interviewSessionMapper,
                interviewQuestionMapper,
                interviewAnswerMapper,
                interviewEvaluationMapper,
                interviewCandidateMapper,
                tagMapper,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        service.delete(1L, 5L);
        verify(interviewCandidateMapper).delete(any());
        verify(interviewEvaluationMapper).delete(any());
        verify(interviewAnswerMapper).delete(any());
        verify(interviewQuestionMapper).delete(any());
        verify(interviewSessionMapper).deleteById(5L);

        InterviewSession evaluating = new InterviewSession();
        evaluating.setId(6L);
        evaluating.setUserId(1L);
        evaluating.setStatus(2);
        when(interviewSessionMapper.selectById(6L)).thenReturn(evaluating);

        BizException ex = assertThrows(BizException.class, () -> service.delete(1L, 6L));
        assertEquals(ErrorCodes.INTERVIEW_PROCESSING, ex.getCode());
        verify(interviewSessionMapper, never()).deleteById(6L);
    }
}
