package com.yuzhijie.career.trace.module.match.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户技能聚合画像")
public class SkillProfileVO {

    @Schema(description = "聚合更新时间", example = "2026-11-09 14:21:00")
    private String updatedAt;

    @Schema(description = "技能列表")
    private List<SkillItemVO> skills;
}
