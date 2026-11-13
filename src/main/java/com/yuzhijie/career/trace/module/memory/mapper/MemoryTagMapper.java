package com.yuzhijie.career.trace.module.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.match.model.SkillEvidenceRow;
import com.yuzhijie.career.trace.module.memory.entity.MemoryTag;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagCountRow;
import com.yuzhijie.career.trace.module.memory.model.MemoryTagRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MemoryTagMapper extends BaseMapper<MemoryTag> {

    @Select("""
            SELECT mt.memory_id, mt.tag_id, mt.polarity, mt.proficiency, mt.confidence,
                   mt.evidence, mt.assessed_by,
                   t.name AS tag_name, t.slug AS tag_slug, t.category AS tag_category
            FROM memory_tags mt
            JOIN tags t ON t.id = mt.tag_id
            WHERE mt.memory_id = #{memoryId}
            ORDER BY t.category, mt.confidence DESC, mt.tag_id
            """)
    List<MemoryTagRow> selectTagRowsByMemoryId(@Param("memoryId") Long memoryId);

    @Select("""
            SELECT mt.memory_id, mt.tag_id, mt.polarity, mt.proficiency, mt.confidence,
                   mt.evidence, mt.assessed_by,
                   t.name AS tag_name, t.slug AS tag_slug, t.category AS tag_category
            FROM memory_tags mt
            JOIN tags t ON t.id = mt.tag_id
            JOIN memory_items mi ON mi.id = mt.memory_id
            WHERE mi.user_id = #{userId} AND mi.status = 1
              AND mt.polarity = #{polarity}
            ORDER BY mt.tag_id, mt.proficiency DESC, mt.confidence DESC, mt.memory_id DESC
            """)
    List<MemoryTagRow> selectUserTagRows(@Param("userId") Long userId, @Param("polarity") Integer polarity);

    @Select("""
            SELECT mt.memory_id, mt.tag_id, mt.polarity, mt.proficiency, mt.confidence,
                   mt.evidence, mt.assessed_by,
                   t.name AS tag_name, t.slug AS tag_slug, t.category AS tag_category
            FROM memory_tags mt
            JOIN tags t ON t.id = mt.tag_id
            WHERE mt.memory_id = #{memoryId} AND mt.tag_id = #{tagId}
            """)
    MemoryTagRow selectTagRow(@Param("memoryId") Long memoryId, @Param("tagId") Long tagId);

    @Select("""
            <script>
            SELECT memory_id, COUNT(*) AS tag_count
            FROM memory_tags
            WHERE memory_id IN
            <foreach collection="memoryIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY memory_id
            </script>
            """)
    List<MemoryTagCountRow> countByMemoryIds(@Param("memoryIds") Collection<Long> memoryIds);

    @Select("""
            SELECT mt.tag_id, mt.proficiency, mt.confidence, mt.memory_id
            FROM memory_tags mt
            JOIN tags t ON t.id = mt.tag_id
            JOIN memory_items mi ON mi.id = mt.memory_id
            WHERE mi.user_id = #{userId} AND mi.status = 1
              AND mt.polarity = 1 AND t.category = 'skill'
            ORDER BY mt.tag_id, mt.proficiency DESC, mt.confidence DESC, mt.memory_id DESC
            """)
    List<SkillEvidenceRow> selectSkillEvidence(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO memory_tags (memory_id, tag_id, polarity, proficiency, confidence, evidence, assessed_by)
            VALUES (#{memoryId}, #{tagId}, #{polarity}, #{proficiency}, #{confidence}, #{evidence}, #{assessedBy}) AS new_row
            ON DUPLICATE KEY UPDATE
                polarity = new_row.polarity,
                proficiency = new_row.proficiency,
                confidence = new_row.confidence,
                evidence = new_row.evidence,
                assessed_by = new_row.assessed_by,
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(MemoryTag memoryTag);

    @Delete("DELETE FROM memory_tags WHERE memory_id = #{memoryId}")
    int deleteByMemoryId(@Param("memoryId") Long memoryId);

    @Delete("DELETE FROM memory_tags WHERE memory_id = #{memoryId} AND tag_id = #{tagId}")
    int deleteByMemoryIdAndTagId(@Param("memoryId") Long memoryId, @Param("tagId") Long tagId);

    @Update("""
            <script>
            UPDATE memory_tags
            SET assessed_by = 'manual'
            <if test="proficiency != null">, proficiency = #{proficiency}</if>
            <if test="polarity != null">, polarity = #{polarity}</if>
            , updated_at = CURRENT_TIMESTAMP
            WHERE memory_id = #{memoryId} AND tag_id = #{tagId}
            </script>
            """)
    int updateManual(@Param("memoryId") Long memoryId,
                     @Param("tagId") Long tagId,
                     @Param("proficiency") Integer proficiency,
                     @Param("polarity") Integer polarity);
}
