package com.yuzhijie.career.trace.module.match.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.match.entity.UserSkillProfile;
import com.yuzhijie.career.trace.module.match.model.SkillProfileRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSkillProfileMapper extends BaseMapper<UserSkillProfile> {

    @Select("""
            SELECT usp.tag_id, t.name, t.slug, usp.level, usp.confidence,
                   usp.memory_count, usp.best_memory_id, usp.updated_at
            FROM user_skill_profile usp
            JOIN tags t ON t.id = usp.tag_id
            WHERE usp.user_id = #{userId}
            ORDER BY usp.level DESC, usp.confidence DESC, usp.tag_id
            """)
    List<SkillProfileRow> selectProfileRows(@Param("userId") Long userId);

    @Delete("DELETE FROM user_skill_profile WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
