package com.yuzhijie.career.trace.module.interview.model;

import lombok.Data;

import java.util.List;

@Data
public class InterviewEvaluationPayload {
    private List<Evaluation> questionEvaluations;

    @Data
    public static class Evaluation {
        private Integer questionIndex;
        private Integer score;
        private String feedback;
        private String referenceAnswer;
        private String points;
    }
}
