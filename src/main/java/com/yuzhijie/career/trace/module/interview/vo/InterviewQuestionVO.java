package com.yuzhijie.career.trace.module.interview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "面试题目")
public class InterviewQuestionVO {
    @Schema(description = "题目序号 0-based", example = "0")
    private Integer questionIndex;
    @Schema(description = "追问所属主问题序号；主问题填自身", example = "0")
    private Integer parentIndex;
    @Schema(description = "是否追问", example = "false")
    private Boolean isFollowUp;
    @Schema(description = "来源：RESUME/SKILL", example = "SKILL")
    private String source;
    @Schema(description = "方向标签 ID", example = "106")
    private Long tagId;
    @Schema(description = "方向名", example = "内容策划")
    private String direction;
    @Schema(description = "知识点摘要", example = "内容选题拆解")
    private String topicSummary;
    @Schema(description = "题干")
    private String title;
}
