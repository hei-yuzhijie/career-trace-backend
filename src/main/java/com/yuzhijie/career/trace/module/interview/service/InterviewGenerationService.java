package com.yuzhijie.career.trace.module.interview.service;

import com.yuzhijie.career.trace.integration.llm.DeepSeekClient;
import com.yuzhijie.career.trace.integration.llm.LlmException;
import com.yuzhijie.career.trace.integration.llm.LlmJson;
import com.yuzhijie.career.trace.integration.llm.PromptLoader;
import com.yuzhijie.career.trace.module.interview.entity.InterviewQuestion;
import com.yuzhijie.career.trace.module.interview.entity.InterviewSession;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewQuestionMapper;
import com.yuzhijie.career.trace.module.interview.mapper.InterviewSessionMapper;
import com.yuzhijie.career.trace.module.interview.model.GeneratedQuestion;
import com.yuzhijie.career.trace.module.interview.model.InterviewGenerationPayload;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.job.mapper.JobTagMapper;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.match.mapper.UserSkillProfileMapper;
import com.yuzhijie.career.trace.module.match.model.SkillProfileRow;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.mapper.ResumeMapper;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试出题：简历题 + 方向题；方向来自 tags.category=skill。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewGenerationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final TagMapper tagMapper;
    private final UserSkillProfileMapper userSkillProfileMapper;
    private final JobTagMapper jobTagMapper;
    private final JobMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;
    private final InterviewQuestionWriter questionWriter;
    private final InterviewReferenceService referenceService;

    @Value("${career-trace.interview.resume-prompt:classpath:prompts/interview/question-resume.md}")
    private String resumePromptLocation;

    @Value("${career-trace.interview.skill-prompt:classpath:prompts/interview/question-skill.md}")
    private String skillPromptLocation;

    @Value("${career-trace.interview.max-retry:3}")
    private int maxAttempts;

    @Async("taskExecutor")
    public void generateAsync(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() == null || session.getStatus() != 0) {
            return;
        }
        try {
            List<Long> directionIds = parseIds(session.getDirectionTagIds());
            List<Tag> directions = loadDirections(directionIds);
            Map<Long, Integer> userLevels = loadUserLevels(session.getUserId());
            Map<Long, Integer> jobRequirements = loadJobRequirements(session.getJobId());
            String resumeText = loadResumeText(session.getResumeId());
            String jobSection = buildJobSection(session);

            int total = session.getQuestionCount() == null ? 5 : session.getQuestionCount();
            int followUp = session.getFollowUpCount() == null ? 0 : session.getFollowUpCount();
            int resumeCount = resumeText == null || resumeText.isBlank()
                    ? 0 : Math.max(1, (int) Math.round(total * 0.6));
            int skillCount = total - resumeCount;

            List<GeneratedQuestion> resumeQuestions = resumeCount > 0
                    ? generateResumeQuestions(session, resumeCount, followUp, resumeText, jobSection)
                    : List.of();
            List<GeneratedQuestion> skillQuestions = skillCount > 0
                    ? generateSkillQuestions(session, directions, userLevels, jobRequirements,
                    skillCount, followUp, jobSection)
                    : List.of();

            List<GeneratedQuestion> merged = new ArrayList<>();
            merged.addAll(resumeQuestions);
            merged.addAll(skillQuestions);
            if (merged.isEmpty()) {
                merged = fallbackQuestions(total, followUp, directions.isEmpty() ? null : directions.get(0));
            }

            List<InterviewQuestion> rows = buildRows(sessionId, merged, followUp);
            questionWriter.saveReady(sessionId, rows);
            log.info("模拟面试出题完成 sessionId={}, rows={}", sessionId, rows.size());
        } catch (Exception e) {
            log.warn("模拟面试出题失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            questionWriter.markFailed(sessionId);
        }
    }

    private List<GeneratedQuestion> generateResumeQuestions(InterviewSession session, int count, int followUp,
                                                            String resumeText, String jobSection) {
        try {
            String prompt = promptLoader.load(resumePromptLocation)
                    .replace("{questionCount}", String.valueOf(count))
                    .replace("{followUpCount}", String.valueOf(followUp))
                    .replace("{difficultyDescription}", difficultyText(session.getDifficulty()))
                    .replace("{resumeText}", resumeText)
                    .replace("{jdSection}", jobSection)
                    .replace("{historicalSection}", historicalSection(session.getUserId()));
            String content = callWithRetry(prompt);
            InterviewGenerationPayload payload = MAPPER.readValue(
                    LlmJson.extractObject(content), InterviewGenerationPayload.class);
            List<GeneratedQuestion> result = toQuestions(payload, "RESUME", null, "简历题",
                    count, followUp);
            if (result.size() < count) {
                result.addAll(fallbackQuestions(count - result.size(), followUp, null, "RESUME", "简历题"));
            }
            return result.subList(0, count);
        } catch (Exception e) {
            log.warn("简历题生成失败，降级为方向题: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GeneratedQuestion> generateSkillQuestions(InterviewSession session, List<Tag> directions,
                                                           Map<Long, Integer> userLevels,
                                                           Map<Long, Integer> jobRequirements,
                                                           int count, int followUp, String jobSection) {
        try {
            List<Tag> selected = selectDirections(directions, userLevels, jobRequirements, count);
            Map<Long, Integer> allocation = allocate(selected, count);
            String allocationTable = buildAllocationTable(selected, allocation);
            String evidence = buildDirectionEvidence(session.getUserId(), selected);

            String prompt = promptLoader.load(skillPromptLocation)
                    .replace("{questionCount}", String.valueOf(count))
                    .replace("{followUpCount}", String.valueOf(followUp))
                    .replace("{difficultyDescription}", difficultyText(session.getDifficulty()))
                    .replace("{allocationTable}", allocationTable)
                    .replace("{directionEvidence}", evidence)
                    .replace("{referenceSection}", referenceService.load(selected))
                    .replace("{jdSection}", jobSection)
                    .replace("{historicalSection}", historicalSection(session.getUserId()));
            String content = callWithRetry(prompt);
            InterviewGenerationPayload payload = MAPPER.readValue(
                    LlmJson.extractObject(content), InterviewGenerationPayload.class);
            List<GeneratedQuestion> result = toSkillQuestions(payload, selected, count, followUp);
            if (result.size() < count) {
                Tag tag = selected.isEmpty() ? null : selected.get(0);
                result.addAll(fallbackQuestions(count - result.size(), followUp, tag,
                        "SKILL", tag == null ? "通用职业能力" : tag.getName()));
            }
            return result.subList(0, count);
        } catch (Exception e) {
            log.warn("方向题生成失败，降级为兜底题: {}", e.getMessage());
            Tag tag = directions.isEmpty() ? null : directions.get(0);
            return fallbackQuestions(count, followUp, tag);
        }
    }

    private List<GeneratedQuestion> toQuestions(InterviewGenerationPayload payload, String source,
                                                 Long tagId, String direction, int count, int followUp) {
        List<GeneratedQuestion> result = new ArrayList<>();
        if (payload == null || payload.getQuestions() == null) {
            return result;
        }
        for (InterviewGenerationPayload.Question question : payload.getQuestions()) {
            if (question.getTitle() == null || question.getTitle().isBlank()) {
                continue;
            }
            GeneratedQuestion generated = new GeneratedQuestion();
            generated.setSource(source);
            generated.setTagId(tagId);
            generated.setDirection(direction);
            generated.setTopicSummary(truncate(nullSafe(question.getTopicSummary()), 50));
            generated.setTitle(question.getTitle());
            generated.setFollowUps(normalizeFollowUps(question.getFollowUps(), followUp));
            result.add(generated);
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    private List<GeneratedQuestion> toSkillQuestions(InterviewGenerationPayload payload,
                                                      List<Tag> directions, int count, int followUp) {
        List<GeneratedQuestion> result = new ArrayList<>();
        if (payload == null || payload.getQuestions() == null) {
            return result;
        }
        for (InterviewGenerationPayload.Question question : payload.getQuestions()) {
            if (question.getTitle() == null || question.getTitle().isBlank()) {
                continue;
            }
            Tag tag = matchTag(question.getType(), directions);
            GeneratedQuestion generated = new GeneratedQuestion();
            generated.setSource("SKILL");
            generated.setTagId(tag == null ? null : tag.getId());
            generated.setDirection(tag == null ? "通用职业能力" : tag.getName());
            generated.setTopicSummary(truncate(nullSafe(question.getTopicSummary()), 50));
            generated.setTitle(question.getTitle());
            generated.setFollowUps(normalizeFollowUps(question.getFollowUps(), followUp));
            result.add(generated);
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    private List<InterviewQuestion> buildRows(Long sessionId, List<GeneratedQuestion> questions, int followUp) {
        List<InterviewQuestion> rows = new ArrayList<>();
        int index = 0;
        for (GeneratedQuestion generated : questions) {
            int mainIndex = index++;
            rows.add(row(sessionId, mainIndex, mainIndex, generated));
            List<String> followUps = generated.getFollowUps() == null
                    ? List.of() : generated.getFollowUps();
            for (int i = 0; i < Math.min(followUp, followUps.size()); i++) {
                GeneratedQuestion child = new GeneratedQuestion();
                child.setSource(generated.getSource());
                child.setTagId(generated.getTagId());
                child.setDirection(generated.getDirection());
                child.setTopicSummary(truncate("追问·" + nullSafe(generated.getTopicSummary()), 50));
                child.setTitle(followUps.get(i));
                child.setFollowUps(List.of());
                rows.add(row(sessionId, index++, mainIndex, child));
            }
        }
        return rows;
    }

    private InterviewQuestion row(Long sessionId, int index, int parentIndex, GeneratedQuestion generated) {
        InterviewQuestion question = new InterviewQuestion();
        question.setSessionId(sessionId);
        question.setQuestionIndex(index);
        question.setParentIndex(parentIndex);
        question.setSource(generated.getSource());
        question.setTagId(generated.getTagId());
        question.setDirectionName(generated.getDirection());
        question.setTopicSummary(generated.getTopicSummary());
        question.setTitle(generated.getTitle());
        question.setReferenceAnswer(generated.getReferenceAnswer());
        question.setPoints(generated.getPoints());
        question.setCreatedAt(LocalDateTime.now());
        return question;
    }

    private List<GeneratedQuestion> fallbackQuestions(int count, int followUp, Tag tag) {
        return fallbackQuestions(count, followUp, tag,
                tag == null ? "SKILL" : "SKILL",
                tag == null ? "通用职业能力" : tag.getName());
    }

    private List<GeneratedQuestion> fallbackQuestions(int count, int followUp, Tag tag,
                                                       String source, String direction) {
        String[] titles = {
                "请介绍一个你最有代表性的项目，并说明你的个人贡献。",
                "在这个项目中，你遇到的最大困难是什么？怎么解决的？",
                "如果让你重做一次，你会在哪些地方做得不同？",
                "你如何验证自己的方案有效？用了哪些指标？",
                "请说明一个你从 0 到 1 推进的事情。"
        };
        List<GeneratedQuestion> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GeneratedQuestion question = new GeneratedQuestion();
            question.setSource(source);
            question.setTagId(tag == null ? null : tag.getId());
            question.setDirection(direction);
            question.setTopicSummary("通用问题" + (i + 1));
            question.setTitle(titles[i % titles.length]);
            question.setFollowUps(padFollowUps(List.of(), followUp));
            result.add(question);
        }
        return result;
    }

    private static List<String> normalizeFollowUps(List<String> followUps, int count) {
        return padFollowUps(followUps == null ? List.of() : followUps, count);
    }

    private static List<String> padFollowUps(List<String> followUps, int count) {
        List<String> result = new ArrayList<>();
        for (String followUp : followUps) {
            if (followUp != null && !followUp.isBlank()) {
                result.add(followUp);
            }
            if (result.size() >= count) {
                return result;
            }
        }
        while (result.size() < count) {
            result.add("请再具体说明一下你的做法和结果。");
        }
        return result;
    }

    private List<Tag> loadDirections(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectBatchIds(ids).stream()
                .filter(tag -> "skill".equals(tag.getCategory()))
                .toList();
    }

    private List<Tag> selectDirections(List<Tag> directions, Map<Long, Integer> userLevels,
                                       Map<Long, Integer> jobRequirements, int count) {
        if (directions.isEmpty()) {
            return List.of();
        }
        List<Tag> sorted = new ArrayList<>(directions);
        sorted.sort(Comparator
                .comparingInt((Tag tag) -> gap(tag, userLevels, jobRequirements)).reversed()
                .thenComparing(Tag::getId));
        return sorted.size() <= count ? sorted : sorted.subList(0, count);
    }

    private static int gap(Tag tag, Map<Long, Integer> userLevels, Map<Long, Integer> jobRequirements) {
        int required = jobRequirements.getOrDefault(tag.getId(), 3);
        int level = userLevels.getOrDefault(tag.getId(), 0);
        return Math.max(0, required - level);
    }

    private static Map<Long, Integer> allocate(List<Tag> directions, int count) {
        Map<Long, Integer> allocation = new LinkedHashMap<>();
        if (directions.isEmpty()) {
            return allocation;
        }
        for (int i = 0; i < count; i++) {
            Tag tag = directions.get(i % directions.size());
            allocation.merge(tag.getId(), 1, Integer::sum);
        }
        return allocation;
    }

    private static String buildAllocationTable(List<Tag> directions, Map<Long, Integer> allocation) {
        if (directions.isEmpty()) {
            return "通用职业能力 | 全部题目 | 通用";
        }
        StringBuilder builder = new StringBuilder("方向 | 数量 | 说明\n");
        for (Tag tag : directions) {
            builder.append(tag.getName()).append(" | ")
                    .append(allocation.getOrDefault(tag.getId(), 0))
                    .append(" | ").append(tag.getSlug()).append("\n");
        }
        return builder.toString();
    }

    private String historicalSection(Long userId) {
        List<String> topics = questionMapper.selectRecentTopics(userId, 20);
        if (topics == null || topics.isEmpty()) {
            return "无";
        }
        return String.join("；", topics);
    }

    private String buildDirectionEvidence(Long userId, List<Tag> directions) {
        if (directions.isEmpty()) {
            return "无指定方向，请出通用职业能力题。";
        }
        List<MemoryTagRow> rows = memoryTagMapper.selectUserTagRows(userId, 1);
        Map<Long, List<MemoryTagRow>> byTag = new HashMap<>();
        for (MemoryTagRow row : rows) {
            byTag.computeIfAbsent(row.getTagId(), key -> new ArrayList<>()).add(row);
        }
        StringBuilder builder = new StringBuilder();
        for (Tag tag : directions) {
            builder.append(tag.getName()).append("：");
            List<MemoryTagRow> evidenceRows = byTag.getOrDefault(tag.getId(), List.of());
            if (evidenceRows.isEmpty()) {
                builder.append("暂无可引用记忆证据；");
            } else {
                evidenceRows.stream().limit(2).forEach(row ->
                        builder.append(nullSafe(row.getEvidence())).append("；"));
            }
        }
        return builder.toString();
    }

    private String buildJobSection(InterviewSession session) {
        if (session.getJdText() != null && !session.getJdText().isBlank()) {
            return session.getJdText();
        }
        if (session.getJobId() != null) {
            JobRow job = jobMapper.selectJobRowById(session.getJobId());
            if (job != null) {
                return "岗位：" + nullSafe(job.getTitle()) + "；JD：" + nullSafe(job.getJdText());
            }
        }
        return "未指定目标岗位。";
    }

    private String loadResumeText(Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        Resume resume = resumeMapper.selectById(resumeId);
        return resume == null ? null : resume.getTextContent();
    }

    private Map<Long, Integer> loadUserLevels(Long userId) {
        Map<Long, Integer> levels = new HashMap<>();
        for (SkillProfileRow row : userSkillProfileMapper.selectProfileRows(userId)) {
            if (row.getLevel() != null) {
                levels.put(row.getTagId(), row.getLevel());
            }
        }
        return levels;
    }

    private Map<Long, Integer> loadJobRequirements(Long jobId) {
        Map<Long, Integer> requirements = new HashMap<>();
        if (jobId == null) {
            return requirements;
        }
        for (JobTagRow row : jobTagMapper.selectByJobId(jobId)) {
            if ("skill".equals(row.getTagCategory())) {
                requirements.put(row.getTagId(), row.getRequiredLevel() == null ? 0 : row.getRequiredLevel());
            }
        }
        return requirements;
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
        throw last == null ? new LlmException("面试出题调用失败") : last;
    }

    private static Tag matchTag(String type, List<Tag> directions) {
        if (type == null) {
            return directions.isEmpty() ? null : directions.get(0);
        }
        for (Tag tag : directions) {
            if (type.equalsIgnoreCase(tag.getSlug()) || type.equalsIgnoreCase(tag.getName())) {
                return tag;
            }
        }
        return directions.isEmpty() ? null : directions.get(0);
    }

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
                // 跳过非法 ID
            }
        }
        return ids;
    }

    private static String difficultyText(String difficulty) {
        if (difficulty == null) {
            return "进阶";
        }
        return switch (difficulty) {
            case "basic" -> "基础";
            case "advanced" -> "专家";
            default -> "进阶";
        };
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
