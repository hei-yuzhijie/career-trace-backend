package com.yuzhijie.career.trace.module.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.module.interview.entity.InterviewQuestion;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewQuestionMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewQuestionWriter {

    private final InterviewQuestionMapper questionMapper;
    private final InterviewSessionMapper sessionMapper;

    @Transactional
    public void saveReady(Long sessionId, List<InterviewQuestion> questions) {
        questionMapper.deleteBySessionId(sessionId);
        for (InterviewQuestion question : questions) {
            questionMapper.insert(question);
        }
        sessionMapper.update(null, Wrappers.<InterviewSession>lambdaUpdate()
                .eq(InterviewSession::getId, sessionId)
                .set(InterviewSession::getStatus, 1)
                .set(InterviewSession::getUpdatedAt, LocalDateTime.now()));
    }

    @Transactional
    public void markFailed(Long sessionId) {
        sessionMapper.update(null, Wrappers.<InterviewSession>lambdaUpdate()
                .eq(InterviewSession::getId, sessionId)
                .set(InterviewSession::getStatus, 4)
                .set(InterviewSession::getUpdatedAt, LocalDateTime.now()));
    }
}
