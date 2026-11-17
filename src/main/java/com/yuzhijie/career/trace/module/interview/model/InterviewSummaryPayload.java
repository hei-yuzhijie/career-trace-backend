package com.yuzhijie.career.trace.module.interview.model;

import lombok.Data;

import java.util.List;

@Data
public class InterviewSummaryPayload {
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
}
