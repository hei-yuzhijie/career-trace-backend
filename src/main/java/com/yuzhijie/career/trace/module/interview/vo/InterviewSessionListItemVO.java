package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试会话列表项")
public class InterviewSessionListItemVO {
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "难度")
    private String difficulty;
    @Schema(description = "主问题数")
    private Integer questionCount;
    @Schema(description = "每主问题追问数")
    private Integer followUpCount;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "总分")
    private Integer totalScore;
    @Schema(description = "方向名")
    private List<String> directionNames;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
