package com.yuzhijie.career.trace.module.interview.model;

import lombok.Data;

import java.util.List;

@Data
public class InterviewGenerationPayload {
    private List<Question> questions;

    @Data
    public static class Question {
        private String type;
        private String topicSummary;
        private String title;
        private List<String> followUps;
    }
}
