package com.yuzhijie.career.trace.module.interview.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {
    private String source;
    private Long tagId;
    private String direction;
    private String topicSummary;
    private String title;
    private String referenceAnswer;
    private String points;
    private java.util.List<String> followUps;
}
