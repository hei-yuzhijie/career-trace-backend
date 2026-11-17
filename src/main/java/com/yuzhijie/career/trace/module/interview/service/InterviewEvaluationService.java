package com.yuzhijie.career.trace.module.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.integration.llm.DeepSeekClient;
import com.yuzhijie.career.trace.integration.llm.LlmException;
import com.yuzhijie.career.trace.integration.llm.LlmJson;
import com.yuzhijie.career.trace.integration.llm.PromptLoader;
import com.yuzhijie.career.trace.module.interview.entity.InterviewAnswer;
import com.yuzhijie.career.trace.module.interview.entity.InterviewEvaluation;
import com.yuzhijie.career.trace.module.interview.entity.InterviewMemoryCandidate;
import com.yuzhijie.career.trace.module.interview.entity.InterviewQuestion;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewAnswerMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewEvaluationMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewMemoryCandidateMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewQuestionMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewSessionMapper;
import com.yuzhijie.career.trace.module.interview.model.InterviewEvaluationPayload;
import com.yuzhijie.career.trace.module.interview.model.InterviewSummaryPayload;
import com.yuzhijie.career.trace.module.interview.vo.InterviewDimensionVO;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试批量评估：问答组分组、分批 LLM、代码算总分/维度分、记忆候选。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewEvaluationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final InterviewEvaluationMapper evaluationMapper;
    private final InterviewMemoryCandidateMapper memoryCandidateMapper;
    private final InterviewScoringService scoringService;
    private final TagMapper tagMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;

    @Value("${career-trace.interview.evaluation-prompt:classpath:prompts/interview/evaluation.md}")
    private String evaluationPromptLocation;

    @Value("${career-trace.interview.summary-prompt:classpath:prompts/interview/evaluation-summary.md}")
    private String summaryPromptLocation;

    @Value("${career-trace.interview.batch-size:8}")
    private int batchSize;

    @Value("${career-trace.interview.max-retry:3}")
    private int maxAttempts;

    @Async("taskExecutor")
    public void evaluateAsync(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() == null || session.getStatus() != 2) {
            return;
        }
        try {
            List<InterviewQuestion> questions = questionMapper.selectBySessionId(sessionId);
            List<InterviewAnswer> answers = answerMapper.selectBySessionId(sessionId);
            if (questions.isEmpty()) {
                throw new IllegalStateException("没有可评估的题目");
            }
            Map<Integer, String> answerMap = new HashMap<>();
            for (InterviewAnswer answer : answers) {
                answerMap.put(answer.getQuestionIndex(), answer.getAnswerText());
            }

            List<List<InterviewQuestion>> groups = groupQuestions(questions);
            int size = Math.max(1, batchSize);
            for (int start = 0; start < groups.size(); start += size) {
                List<InterviewQuestion> batch = groups.subList(start, Math.min(start + size, groups.size()))
                        .stream().flatMap(List::stream).toList();
                evaluateBatch(session, batch, answerMap);
            }

            List<InterviewEvaluation> evaluations = evaluationMapper.selectBySessionId(sessionId);
            int total = scoringService.totalScore(questions, evaluations);
            List<InterviewDimensionVO> dimensions = scoringService.dimensions(questions, evaluations);
            InterviewSummaryPayload summary = summarize(session, dimensions, evaluations, total);

            sessionMapper.update(null, Wrappers.<InterviewSession>lambdaUpdate()
                    .eq(InterviewSession::getId, sessionId)
                    .set(InterviewSession::getStatus, 3)
                    .set(InterviewSession::getTotalScore, total)
                    .set(InterviewSession::getSummary, summary.getSummary())
                    .set(InterviewSession::getStrengths, writeJson(summary.getStrengths()))
                    .set(InterviewSession::getImprovements, writeJson(summary.getImprovements()))
                    .set(InterviewSession::getUpdatedAt, LocalDateTime.now()));

            try {
                generateCandidates(session, dimensions);
            } catch (Exception candidateError) {
                // 报告已经生成，候选失败不应把会话改成 FAILED
                log.warn("生成面试记忆候选失败 sessionId={}: {}", sessionId, candidateError.getMessage());
            }
            log.info("模拟面试评估完成 sessionId={}, score={}", sessionId, total);
        } catch (Exception e) {
            log.warn("模拟面试评估失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            sessionMapper.update(null, Wrappers.<InterviewSession>lambdaUpdate()
                    .eq(InterviewSession::getId, sessionId)
                    .set(InterviewSession::getStatus, 4)
                    .set(InterviewSession::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private void evaluateBatch(InterviewSession session, List<InterviewQuestion> batch,
                               Map<Integer, String> answerMap) {
        String qaRecords = buildQaRecords(batch, answerMap);
        String prompt = promptLoader.load(evaluationPromptLocation)
                .replace("{resumeText}", "")
                .replace("{qaRecords}", qaRecords)
                .replace("{referenceContext}", "无");
        Map<Integer, InterviewEvaluationPayload.Evaluation> evaluated = new HashMap<>();
        try {
            String content = callWithRetry(prompt);
            InterviewEvaluationPayload payload = MAPPER.readValue(
                    LlmJson.extractObject(content), InterviewEvaluationPayload.class);
            if (payload.getQuestionEvaluations() != null) {
                for (InterviewEvaluationPayload.Evaluation evaluation : payload.getQuestionEvaluations()) {
                    if (evaluation.getQuestionIndex() != null) {
                        evaluated.put(evaluation.getQuestionIndex(), evaluation);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("面试评估批次失败，按 0 分兜底: {}", e.getMessage());
        }
        for (InterviewQuestion question : batch) {
            InterviewEvaluationPayload.Evaluation evaluation = evaluated.get(question.getQuestionIndex());
            int score = evaluation == null || evaluation.getScore() == null
                    ? 0 : Math.max(0, Math.min(100, evaluation.getScore()));
            String feedback = evaluation == null || evaluation.getFeedback() == null
                    ? "评估缺失，按 0 分处理" : evaluation.getFeedback();
            evaluationMapper.upsert(session.getId(), question.getQuestionIndex(), score, feedback,
                    evaluation == null ? null : evaluation.getReferenceAnswer(),
                    evaluation == null ? null : evaluation.getPoints());
        }
    }

    private InterviewSummaryPayload summarize(InterviewSession session,
                                              List<InterviewDimensionVO> dimensions,
                                              List<InterviewEvaluation> evaluations,
                                              int total) {
        InterviewSummaryPayload fallback = new InterviewSummaryPayload();
        fallback.setSummary("本次模拟面试总分 " + total + "，最高维度："
                + (dimensions.isEmpty() ? "-" : dimensions.get(0).getName()) + "。");
        fallback.setStrengths(dimensions.stream().limit(3)
                .map(item -> item.getName() + "：" + item.getScore() + " 分").toList());
        fallback.setImprovements(dimensions.stream()
                .sorted((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .limit(3)
                .map(item -> "提升 " + item.getName() + "（当前 " + item.getScore() + " 分）").toList());

        try {
            String categorySummary = dimensions.stream()
                    .map(item -> item.getName() + "=" + item.getScore())
                    .reduce((a, b) -> a + "；" + b).orElse("无");
            String highlights = evaluations.stream().limit(5)
                    .map(item -> "第 " + item.getQuestionIndex() + " 题：" + item.getFeedback())
                    .reduce((a, b) -> a + "；" + b).orElse("无");
            String prompt = promptLoader.load(summaryPromptLocation)
                    .replace("{resumeText}", "")
                    .replace("{referenceContext}", "无")
                    .replace("{categorySummary}", categorySummary)
                    .replace("{questionHighlights}", highlights)
                    .replace("{fallbackOverallFeedback}", fallback.getSummary())
                    .replace("{fallbackStrengths}", String.join("；", fallback.getStrengths()))
                    .replace("{fallbackImprovements}", String.join("；", fallback.getImprovements()));
            String content = callWithRetry(prompt);
            InterviewSummaryPayload payload = MAPPER.readValue(
                    LlmJson.extractObject(content), InterviewSummaryPayload.class);
            if (payload.getSummary() == null || payload.getSummary().isBlank()) {
                return fallback;
            }
            if (payload.getStrengths() == null || payload.getStrengths().isEmpty()) {
                payload.setStrengths(fallback.getStrengths());
            }
            if (payload.getImprovements() == null || payload.getImprovements().isEmpty()) {
                payload.setImprovements(fallback.getImprovements());
            }
            return payload;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void generateCandidates(InterviewSession session, List<InterviewDimensionVO> dimensions) {
        memoryCandidateMapper.delete(Wrappers.<InterviewMemoryCandidate>lambdaQuery()
                .eq(InterviewMemoryCandidate::getSessionId, session.getId()));
        for (InterviewDimensionVO dimension : dimensions) {
            if (dimension.getTagId() == null || dimension.getScore() == null
                    || dimension.getScore() < 60) {
                continue;
            }
            Tag tag = tagMapper.selectById(dimension.getTagId());
            if (tag == null) {
                continue;
            }
            InterviewMemoryCandidate candidate = new InterviewMemoryCandidate();
            candidate.setSessionId(session.getId());
            candidate.setUserId(session.getUserId());
            candidate.setTagId(tag.getId());
            candidate.setContent("模拟面试（" + LocalDate.now() + "）：在「" + tag.getName()
                    + "」方向得分 " + dimension.getScore() + "。");
            candidate.setProficiency(proficiency(dimension.getScore()));
            candidate.setConfidence(BigDecimal.valueOf(dimension.getScore() / 100.0)
                    .max(new BigDecimal("0.5")).min(BigDecimal.ONE)
                    .setScale(2, RoundingMode.HALF_UP));
            candidate.setEvidence("维度得分 " + dimension.getScore());
            candidate.setStatus(0);
            candidate.setCreatedAt(LocalDateTime.now());
            memoryCandidateMapper.insert(candidate);
        }
    }

    private static int proficiency(int score) {
        if (score >= 90) {
            return 4;
        }
        if (score >= 80) {
            return 3;
        }
        if (score >= 70) {
            return 2;
        }
        return 1;
    }

    private static List<List<InterviewQuestion>> groupQuestions(List<InterviewQuestion> questions) {
        Map<Integer, List<InterviewQuestion>> grouped = new LinkedHashMap<>();
        for (InterviewQuestion question : questions) {
            grouped.computeIfAbsent(question.getParentIndex(), key -> new ArrayList<>()).add(question);
        }
        return new ArrayList<>(grouped.values());
    }

    private static String buildQaRecords(List<InterviewQuestion> batch, Map<Integer, String> answerMap) {
        StringBuilder builder = new StringBuilder();
        for (InterviewQuestion question : batch) {
            String answer = answerMap.get(question.getQuestionIndex());
            boolean followUp = !question.getQuestionIndex().equals(question.getParentIndex());
            builder.append("questionIndex=").append(question.getQuestionIndex())
                    .append("；parentIndex=").append(question.getParentIndex())
                    .append("；isFollowUp=").append(followUp)
                    .append("；题目：").append(question.getTitle())
                    .append("；回答：").append(answer == null || answer.isBlank() ? "未作答" : answer)
                    .append("\n");
        }
        return builder.toString();
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
        throw last == null ? new LlmException("面试评估调用失败") : last;
    }

    private static String writeJson(List<String> values) {
        try {
            return MAPPER.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }
}
