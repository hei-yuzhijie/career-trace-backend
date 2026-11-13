package com.yuzhijie.career.trace.module.match.service;

import com.yuzhijie.career.trace.common.util.DateTimes;
import com.yuzhijie.career.trace.module.match.entity.UserSkillProfile;
import com.yuzhijie.career.trace.module.match.mapper.UserSkillProfileMapper;
import com.yuzhijie.career.trace.module.match.model.SkillEvidenceRow;
import com.yuzhijie.career.trace.module.match.model.SkillProfileRow;
import com.yuzhijie.career.trace.module.match.vo.SkillItemVO;
import com.yuzhijie.career.trace.module.match.vo.SkillProfileVO;
import com.yuzhijie.career.trace.module.memory.mapper.MemoryTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * user_skill_profile 聚合：只收 category=skill 的正向标签。
 *
 * <p>聚合口径：level 取该技能所有证据中的最高熟练度；confidence 与 bestMemoryId 取
 * “熟练度最高、置信度最高、memoryId 最大”的那条证据；memoryCount 为去重后的记忆数。</p>
 */
@Slf4j
@Service
public class SkillProfileService {

    private final MemoryTagMapper memoryTagMapper;
    private final UserSkillProfileMapper userSkillProfileMapper;
    private final TransactionTemplate transactionTemplate;

    public SkillProfileService(MemoryTagMapper memoryTagMapper,
                               UserSkillProfileMapper userSkillProfileMapper,
                               PlatformTransactionManager transactionManager) {
        this.memoryTagMapper = memoryTagMapper;
        this.userSkillProfileMapper = userSkillProfileMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async("taskExecutor")
    public void recomputeAsync(Long userId) {
        try {
            transactionTemplate.executeWithoutResult(status -> doRecompute(userId));
        } catch (Exception e) {
            log.error("skill 画像重算失败 userId={}", userId, e);
        }
    }

    private void doRecompute(Long userId) {
        List<SkillEvidenceRow> evidence = memoryTagMapper.selectSkillEvidence(userId);
        Map<Long, List<SkillEvidenceRow>> grouped = new LinkedHashMap<>();
        for (SkillEvidenceRow row : evidence) {
            grouped.computeIfAbsent(row.getTagId(), key -> new ArrayList<>()).add(row);
        }

        userSkillProfileMapper.deleteByUserId(userId);
        for (Map.Entry<Long, List<SkillEvidenceRow>> entry : grouped.entrySet()) {
            List<SkillEvidenceRow> rows = entry.getValue();
            SkillEvidenceRow best = rows.get(0); // SQL 已按 proficiency/confidence/memoryId 倒序
            long memoryCount = rows.stream()
                    .map(SkillEvidenceRow::getMemoryId)
                    .distinct()
                    .count();

            UserSkillProfile profile = new UserSkillProfile();
            profile.setUserId(userId);
            profile.setTagId(entry.getKey());
            profile.setLevel(best.getProficiency());
            profile.setConfidence(best.getConfidence());
            profile.setMemoryCount((int) memoryCount);
            profile.setBestMemoryId(best.getMemoryId());
            profile.setUpdatedAt(LocalDateTime.now());
            userSkillProfileMapper.insert(profile);
        }
        log.info("skill 画像重算完成 userId={}, skillCount={}", userId, grouped.size());
    }

    public SkillProfileVO getProfile(Long userId) {
        List<SkillProfileRow> rows = userSkillProfileMapper.selectProfileRows(userId);
        LocalDateTime updatedAt = rows.stream()
                .map(SkillProfileRow::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<SkillItemVO> skills = rows.stream()
                .map(row -> new SkillItemVO(
                        row.getTagId(),
                        row.getName(),
                        row.getSlug(),
                        row.getLevel(),
                        levelLabel(row.getLevel()),
                        row.getConfidence(),
                        row.getMemoryCount(),
                        row.getBestMemoryId()))
                .toList();
        return new SkillProfileVO(DateTimes.format(updatedAt), skills);
    }

    public static String levelLabel(Integer level) {
        if (level == null) {
            return "未评估";
        }
        return switch (level) {
            case 1 -> "了解";
            case 2 -> "会用";
            case 3 -> "熟练";
            case 4 -> "精通";
            default -> "未评估";
        };
    }
}
