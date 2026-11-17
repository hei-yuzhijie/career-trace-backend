package com.yuzhijie.career.trace.module.interview.service;

import com.yuzhijie.career.trace.module.interview.entity.InterviewEvaluation;
import com.yuzhijie.career.trace.module.interview.entity.InterviewQuestion;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.vo.InterviewDimensionVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewEvaluationVO;
import com.yuzhijie.career.trace.module.interview.vo.InterviewReportVO;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 面试评分汇总：问答组加权、会话总分、维度分。
 */
@Service
@RequiredArgsConstructor
public class InterviewScoringService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TagMapper tagMapper;

    public int totalScore(List<InterviewQuestion> questions, List<InterviewEvaluation> evaluations) {
        List<GroupScore> groups = groupScores(questions, evaluations);
        if (groups.isEmpty()) {
            return 0;
        }
        double avg = groups.stream().mapToInt(GroupScore::score).average().orElse(0);
        return (int) Math.round(avg);
    }

    public List<InterviewDimensionVO> dimensions(List<InterviewQuestion> questions,
                                                 List<InterviewEvaluation> evaluations) {
        List<GroupScore> groups = groupScores(questions, evaluations);
        Map<Long, List<GroupScore>> byTag = new LinkedHashMap<>();
        for (GroupScore group : groups) {
            byTag.computeIfAbsent(group.tagId(), key -> new ArrayList<>()).add(group);
        }
        Map<Long, Tag> tags = loadTags(byTag.keySet());
        List<InterviewDimensionVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<GroupScore>> entry : byTag.entrySet()) {
            int avg = (int) Math.round(entry.getValue().stream()
                    .mapToInt(GroupScore::score).average().orElse(0));
            String name = entry.getKey() == null ? "简历题"
                    : (tags.containsKey(entry.getKey()) ? tags.get(entry.getKey()).getName() : "方向题");
            result.add(new InterviewDimensionVO(entry.getKey(), name, avg, entry.getValue().size()));
        }
        return result;
    }

    public List<GroupScore> groupScores(List<InterviewQuestion> questions,
                                        List<InterviewEvaluation> evaluations) {
        Map<Integer, InterviewQuestion> mainQuestions = new TreeMap<>();
        Map<Integer, List<InterviewQuestion>> followUps = new HashMap<>();
        for (InterviewQuestion question : questions) {
            if (question.getQuestionIndex().equals(question.getParentIndex())) {
                mainQuestions.put(question.getQuestionIndex(), question);
            } else {
                followUps.computeIfAbsent(question.getParentIndex(), key -> new ArrayList<>()).add(question);
            }
        }
        Map<Integer, Integer> scores = new HashMap<>();
        for (InterviewEvaluation evaluation : evaluations) {
            scores.put(evaluation.getQuestionIndex(), evaluation.getScore() == null ? 0 : evaluation.getScore());
        }

        List<GroupScore> result = new ArrayList<>();
        for (Map.Entry<Integer, InterviewQuestion> entry : mainQuestions.entrySet()) {
            Integer mainIndex = entry.getKey();
            InterviewQuestion main = entry.getValue();
            int mainScore = scores.getOrDefault(mainIndex, 0);
            List<InterviewQuestion> children = followUps.getOrDefault(mainIndex, List.of());
            int groupScore;
            if (children.isEmpty()) {
                groupScore = mainScore;
            } else {
                double followAvg = children.stream()
                        .mapToInt(child -> scores.getOrDefault(child.getQuestionIndex(), 0))
                        .average().orElse(0);
                groupScore = (int) Math.round(0.6 * mainScore + 0.4 * followAvg);
            }
            result.add(new GroupScore(mainIndex, main.getTagId(), groupScore));
        }
        return result;
    }

    public InterviewReportVO report(InterviewSession session,
                                    List<InterviewQuestion> questions,
                                    List<InterviewEvaluation> evaluations) {
        InterviewReportVO report = new InterviewReportVO();
        report.setSessionId(session.getId());
        report.setTotalScore(session.getTotalScore() == null
                ? totalScore(questions, evaluations) : session.getTotalScore());
        report.setSummary(session.getSummary());
        report.setStrengths(parseList(session.getStrengths()));
        report.setImprovements(parseList(session.getImprovements()));
        report.setDimensions(dimensions(questions, evaluations));
        report.setEvaluations(evaluations.stream()
                .map(item -> new InterviewEvaluationVO(item.getQuestionIndex(), item.getScore(),
                        item.getFeedback(), item.getReferenceAnswer(), item.getPoints()))
                .toList());
        return report;
    }

    private Map<Long, Tag> loadTags(java.util.Set<Long> tagIds) {
        Map<Long, Tag> result = new HashMap<>();
        List<Long> ids = tagIds.stream().filter(java.util.Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return result;
        }
        for (Tag tag : tagMapper.selectBatchIds(ids)) {
            result.put(tag.getId(), tag);
        }
        return result;
    }

    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    public record GroupScore(Integer parentIndex, Long tagId, Integer score) {
    }
}
