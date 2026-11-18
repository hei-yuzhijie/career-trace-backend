package com.yuzhijie.career.trace.module.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.AfterCommit;
import com.yuzhijie.career.trace.common.util.Pages;
import com.yuzhijie.career.trace.module.interview.dto.InterviewStartRequest;
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
import com.yuzhijie.career.trace.module.interview.vo.InterviewAnswerVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewDirectionVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewQuestionVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewReportVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewSessionDetailVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewSessionListItemVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewStartVO;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.job.mapper.JobTagMapper;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.match.mapper.UserSkillProfileMapper;
import com.yuzhijie.career.trace.module.match.model.SkillProfileRow;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 面试会话门面。负责创建会话、分页查询、详情、保存答案、提交评估，
 * 出题和评估都是事务提交后异步触发，不在这里等待结果。
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final int MAX_ANSWER_LENGTH = 5000;

    @Value("${career-trace.interview.question-min:3}")
    private int minQuestions;

    @Value("${career-trace.interview.question-max:15}")
    private int maxQuestions;

    @Value("${career-trace.interview.follow-up-max:3}")
    private int maxFollowUps;

    @Value("${career-trace.interview.direction-max:6}")
    private int maxDirections;

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final InterviewEvaluationMapper evaluationMapper;
    private final InterviewMemoryCandidateMapper memoryCandidateMapper;
    private final TagMapper tagMapper;
    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final ResumeMapper resumeMapper;
    private final UserSkillProfileMapper userSkillProfileMapper;
    private final InterviewGenerationService generationService;
    private final InterviewEvaluationService evaluationService;
    private final InterviewScoringService scoringService;
    private final InterviewMemoryService memoryService;

    /**
     * 创建面试会话。校验参数和关联资源，解析方向，写会话（status=0），
     * 事务提交后异步出题，立即返回 sessionId。
     */
    @Transactional
    public InterviewStartVO start(Long userId, InterviewStartRequest request) {
        validateRequest(request);
        // 岗位或简历先确认存在
        if (request.getJobId() != null && jobMapper.selectJobRowById(request.getJobId()) == null) {
            throw BizException.notFound(ErrorCodes.JOB_NOT_FOUND, "该岗位已下线");
        }
        if (request.getResumeId() != null && resumeMapper.selectById(request.getResumeId()) == null) {
            throw BizException.notFound(ErrorCodes.RESUME_NOT_FOUND, "简历不存在或已删除");
        }

        // 方向解析有三级兜底：请求指定 → 岗位 skill 标签 → 用户熟练度最高的技能
        List<Long> directionIds = resolveDirections(userId, request);
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(request.getResumeId());
        session.setJobId(request.getJobId());
        session.setJdText(request.getJdText());
        session.setDirectionTagIds(joinIds(directionIds));
        session.setDifficulty(request.getDifficulty());
        session.setQuestionCount(request.getQuestionCount());
        session.setFollowUpCount(request.getFollowUpCount());
        session.setStatus(0);
        session.setPromptVersion("v0.1");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        // 事务提交后再触发异步出题，避免异步线程读不到未提交的会话
        AfterCommit.run(() -> generationService.generateAsync(session.getId()));
        return new InterviewStartVO(session.getId(), 0, statusText(0));
    }

    /**
     * 分页查询当前用户的面试会话记录。可按状态和方向标签过滤。
     */
    public PageResult<InterviewSessionListItemVO> page(Long userId, Integer page, Integer size,
                                                       Integer status, Long tagId) {
        int pageNo = Pages.page(page);
        int pageSize = Pages.size(size);
        // tagId 过滤是对 directionTagIds 字符串做 LIKE，只能保证包含，不能保证精确匹配
        LambdaQueryWrapper<InterviewSession> query = Wrappers.<InterviewSession>lambdaQuery()
                .eq(InterviewSession::getUserId, userId)
                .eq(status != null, InterviewSession::getStatus, status)
                .like(tagId != null, InterviewSession::getDirectionTagIds, tagId == null ? null : String.valueOf(tagId))
                .orderByDesc(InterviewSession::getCreatedAt)
                .orderByDesc(InterviewSession::getId);
        Page<InterviewSession> result = new Page<>(pageNo, pageSize);
        sessionMapper.selectPage(result, query);
        // 列表项带上方向名，前端不用再单独查一次
        List<InterviewSessionListItemVO> records = result.getRecords().stream()
                .map(session -> new InterviewSessionListItemVO(
                        session.getId(), session.getDifficulty(), session.getQuestionCount(),
                        session.getFollowUpCount(), session.getStatus(), session.getTotalScore(),
                        directionNames(parseIds(session.getDirectionTagIds())), session.getCreatedAt()))
                .toList();
        return new PageResult<>(result.getTotal(), pageNo, pageSize, records);
    }

    /**
     * 会话详情。出题中（0）和失败（4）不返回题目；评估完成（3）时带上完整报告。
     * 这个接口也是前端轮询出题和评估状态的入口。
     */
    public InterviewSessionDetailVO detail(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        // 出题中或失败时没有可展示的题目
        List<InterviewQuestion> questions = session.getStatus() != null
                && session.getStatus() != 0 && session.getStatus() != 4
                ? questionMapper.selectBySessionId(sessionId) : List.of();
        long answered = answerMapper.selectCount(Wrappers.<InterviewAnswer>lambdaQuery()
                .eq(InterviewAnswer::getSessionId, sessionId));

        InterviewSessionDetailVO detail = new InterviewSessionDetailVO();
        detail.setSessionId(session.getId());
        detail.setStatus(session.getStatus());
        detail.setStatusText(statusText(session.getStatus()));
        detail.setDifficulty(session.getDifficulty());
        detail.setQuestionCount(session.getQuestionCount());
        detail.setFollowUpCount(session.getFollowUpCount());
        detail.setDirections(directionVOs(parseIds(session.getDirectionTagIds())));
        detail.setQuestions(questions.stream().map(this::toQuestionVO).toList());
        detail.setAnsweredCount((int) answered);
        // 待确认的记忆候选数，前端用来显示「有 N 条记忆待确认」的提示
        detail.setMemoryCandidateCount(Math.toIntExact(memoryCandidateMapper.selectCount(
                Wrappers.<InterviewMemoryCandidate>lambdaQuery()
                        .eq(InterviewMemoryCandidate::getSessionId, sessionId)
                        .eq(InterviewMemoryCandidate::getStatus, 0))));
        if (session.getStatus() != null && session.getStatus() == 4) {
            detail.setErrorMessage("出题或评估失败，请重新发起");
        }
        // 评估完成时才组装报告，报告由 InterviewScoringService 算
        if (session.getStatus() != null && session.getStatus() == 3) {
            List<InterviewEvaluation> evaluations = evaluationMapper.selectBySessionId(sessionId);
            InterviewReportVO report = scoringService.report(session, questions, evaluations);
            detail.setReport(report);
        }
        return detail;
    }

    /**
     * 保存单题答案。只有 status=1 时可作答，用 upsert 支持重复作答同一题。
     */
    @Transactional
    public InterviewAnswerVO saveAnswer(Long userId, Long sessionId, Integer questionIndex, String answerText) {
        InterviewSession session = requireSession(userId, sessionId);
        if (session.getStatus() == null || session.getStatus() != 1) {
            throw BizException.conflict(ErrorCodes.INTERVIEW_STATE_INVALID, "当前状态不能作答");
        }
        if (answerText != null && answerText.length() > MAX_ANSWER_LENGTH) {
            throw BizException.badRequest(ErrorCodes.ANSWER_TOO_LONG, "单题答案不能超过 5000 字");
        }
        // 确认这道题属于本会话，防止传入别的会话的 questionIndex
        InterviewQuestion question = questionMapper.selectOne(Wrappers.<InterviewQuestion>lambdaQuery()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getQuestionIndex, questionIndex));
        if (question == null) {
            throw BizException.badRequest(ErrorCodes.ANSWER_INDEX_INVALID, "答案与题目不匹配");
        }
        answerMapper.upsert(sessionId, questionIndex, answerText == null ? "" : answerText);
        // 回查一次拿到 answeredAt，upsert 本身不返回这列
        InterviewAnswer answer = answerMapper.selectOne(Wrappers.<InterviewAnswer>lambdaQuery()
                .eq(InterviewAnswer::getSessionId, sessionId)
                .eq(InterviewAnswer::getQuestionIndex, questionIndex));
        return new InterviewAnswerVO(questionIndex, answer == null ? answerText : answer.getAnswerText(),
                answer == null ? null : answer.getAnsweredAt());
    }

    /**
     * 查询会话的全部答案。
     */
    public List<InterviewAnswerVO> answers(Long userId, Long sessionId) {
        requireSession(userId, sessionId);
        return answerMapper.selectBySessionId(sessionId).stream()
                .map(answer -> new InterviewAnswerVO(answer.getQuestionIndex(), answer.getAnswerText(),
                        answer.getAnsweredAt()))
                .toList();
    }

    /**
     * 提交评估。只有 status=1 且至少有一道非空答案时可提交，
     * 状态先改为 2（评估中），事务提交后异步评估。
     */
    @Transactional
    public InterviewStartVO evaluate(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        if (session.getStatus() == null || session.getStatus() != 1) {
            throw BizException.conflict(ErrorCodes.INTERVIEW_STATE_INVALID, "当前状态不能评估");
        }
        // 只统计非空答案，空字符串算未作答
        long answers = answerMapper.selectCount(Wrappers.<InterviewAnswer>lambdaQuery()
                .eq(InterviewAnswer::getSessionId, sessionId)
                .ne(InterviewAnswer::getAnswerText, ""));
        if (answers == 0) {
            throw BizException.badRequest(ErrorCodes.NO_ANSWERS, "请至少作答一道题");
        }
        sessionMapper.update(null, Wrappers.<InterviewSession>lambdaUpdate()
                .eq(InterviewSession::getId, sessionId)
                .set(InterviewSession::getStatus, 2)
                .set(InterviewSession::getUpdatedAt, LocalDateTime.now()));
        AfterCommit.run(() -> evaluationService.evaluateAsync(sessionId));
        return new InterviewStartVO(sessionId, 2, statusText(2));
    }

    /**
     * 校验会话存在且属于当前用户，不满足时抛 404。
     */
    public InterviewSession requireSession(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BizException.notFound(ErrorCodes.INTERVIEW_NOT_FOUND, "面试会话不存在");
        }
        return session;
    }

    /**
     * 校验创建会话的参数：题量、追问数、难度、方向数都在配置范围内。
     */
    private void validateRequest(InterviewStartRequest request) {
        if (request == null) {
            throw BizException.badRequest(ErrorCodes.INTERVIEW_PARAM_INVALID, "请求不能为空");
        }
        if (request.getQuestionCount() == null || request.getQuestionCount() < minQuestions
                || request.getQuestionCount() > maxQuestions) {
            throw BizException.badRequest(ErrorCodes.INTERVIEW_PARAM_INVALID,
                    "题量需在 " + minQuestions + "-" + maxQuestions + " 之间");
        }
        if (request.getFollowUpCount() == null || request.getFollowUpCount() < 0
                || request.getFollowUpCount() > maxFollowUps) {
            throw BizException.badRequest(ErrorCodes.INTERVIEW_PARAM_INVALID,
                    "追问数需在 0-" + maxFollowUps + " 之间");
        }
        String difficulty = request.getDifficulty();
        if (difficulty == null || !Set.of("basic", "intermediate", "advanced").contains(difficulty)) {
            throw BizException.badRequest(ErrorCodes.INTERVIEW_PARAM_INVALID, "难度取值非法");
        }
        if (request.getDirectionTagIds() != null && request.getDirectionTagIds().size() > maxDirections) {
            throw BizException.badRequest(ErrorCodes.INTERVIEW_PARAM_INVALID,
                    "最多选择 " + maxDirections + " 个方向");
        }
    }

    /**
     * 解析面试方向：
     * ① 请求显式指定 → 校验全是 skill 标签；
     * ② 有 jobId → 取岗位的 skill 标签；
     * ③ 都没有 → 取用户熟练度最高的 5 个 skill。
     */
    private List<Long> resolveDirections(Long userId, InterviewStartRequest request) {
        List<Long> requested = request.getDirectionTagIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.getDirectionTagIds()));
        if (!requested.isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(requested);
            Set<Long> valid = new LinkedHashSet<>();
            for (Tag tag : tags) {
                if ("skill".equals(tag.getCategory())) {
                    valid.add(tag.getId());
                }
            }
            // 只要有一个不是 skill 标签，就整体拒绝，不做部分接受
            if (valid.size() != requested.size()) {
                throw BizException.badRequest(ErrorCodes.DIRECTION_INVALID, "请选择有效的技能方向");
            }
            return new ArrayList<>(valid);
        }
        if (request.getJobId() != null) {
            List<Long> fromJob = jobTagMapper.selectByJobId(request.getJobId()).stream()
                    .filter(row -> "skill".equals(row.getTagCategory()))
                    .map(JobTagRow::getTagId)
                    .distinct()
                    .limit(maxDirections)
                    .toList();
            if (!fromJob.isEmpty()) {
                return fromJob;
            }
        }
        // 最后兜底：用户熟练度降序取前 5 个
        return userSkillProfileMapper.selectProfileRows(userId).stream()
                .sorted((a, b) -> Integer.compare(b.getLevel() == null ? 0 : b.getLevel(),
                        a.getLevel() == null ? 0 : a.getLevel()))
                .limit(5)
                .map(SkillProfileRow::getTagId)
                .toList();
    }

    /**
     * 把方向标签 ID 列表转成带名称的 VO，标签不存在时名称显示为「方向」。
     */
    private List<InterviewDirectionVO> directionVOs(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Tag> tags = new java.util.HashMap<>();
        for (Tag tag : tagMapper.selectBatchIds(ids)) {
            tags.put(tag.getId(), tag);
        }
        List<InterviewDirectionVO> result = new ArrayList<>();
        for (Long id : ids) {
            Tag tag = tags.get(id);
            result.add(new InterviewDirectionVO(id, tag == null ? "方向" : tag.getName()));
        }
        return result;
    }

    private List<String> directionNames(List<Long> ids) {
        return directionVOs(ids).stream().map(InterviewDirectionVO::getName).toList();
    }

    /**
     * 题目实体转 VO，isFollowUp 由 questionIndex 和 parentIndex 是否相等推导。
     */
    private InterviewQuestionVO toQuestionVO(InterviewQuestion question) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setQuestionIndex(question.getQuestionIndex());
        vo.setParentIndex(question.getParentIndex());
        vo.setIsFollowUp(!question.getQuestionIndex().equals(question.getParentIndex()));
        vo.setSource(question.getSource());
        vo.setTagId(question.getTagId());
        vo.setDirection(question.getDirectionName());
        vo.setTopicSummary(question.getTopicSummary());
        vo.setTitle(question.getTitle());
        return vo;
    }

    private static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    /**
     * 解析逗号分隔的 ID 字符串，跳过非法项。
     */
    private static List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.replace("[", "").replace("]", "").split(",")) {
            try {
                if (!part.isBlank()) {
                    ids.add(Long.parseLong(part.trim()));
                }
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return ids;
    }

    /**
     * 状态码转文字，给前端直接展示。
     */
    private static String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "GENERATING";
            case 1 -> "READY";
            case 2 -> "EVALUATING";
            case 3 -> "FINISHED";
            case 4 -> "FAILED";
            default -> "UNKNOWN";
        };
    }
}