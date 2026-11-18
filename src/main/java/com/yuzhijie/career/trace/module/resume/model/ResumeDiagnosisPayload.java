package com.yuzhijie.career.trace.module.resume.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简历诊断大模型输出结构。
 */
@Data
public class ResumeDiagnosisPayload {
    //五维打分
    private ScoreDetail scoreDetail;
    //一句话总评
    private String summary;
    //简历的优势点，暂时没有处理，存的是大模型返回对应的json
    private List<String> strengths;
    private List<Suggestion> suggestions;
    private List<MemoryCandidate> memories;

    @Data
    public static class ScoreDetail {
        private Integer experienceDepth;
        private Integer achievement;
        private Integer skillMatch;
        private Integer content;
        private Integer expression;
    }

    @Data
    public static class Suggestion {
        private String category;
        private String priority;
        private String issue;
        private String recommendation;
        private String evidence;
    }

    @Data
    public static class MemoryCandidate {
        private String content;
        private List<TagCandidate> tags;
    }

    @Data
    public static class TagCandidate {
        private String name;
        private String category;
        private Integer proficiency;
        private BigDecimal confidence;
        private String evidence;
    }
}
