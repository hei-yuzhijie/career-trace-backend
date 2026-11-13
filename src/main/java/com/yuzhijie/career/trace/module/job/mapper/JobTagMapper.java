package com.yuzhijie.career.trace.module.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.job.entity.JobTag;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface JobTagMapper extends BaseMapper<JobTag> {

    @Select("""
            SELECT jt.job_id, jt.tag_id, jt.weight, jt.required_level,
                   t.name AS tag_name, t.category AS tag_category
            FROM job_tags jt
            JOIN tags t ON t.id = jt.tag_id
            WHERE jt.job_id = #{jobId}
            ORDER BY t.category, jt.weight DESC, jt.tag_id
            """)
    List<JobTagRow> selectByJobId(@Param("jobId") Long jobId);

    @Select("""
            <script>
            SELECT jt.job_id, jt.tag_id, jt.weight, jt.required_level,
                   t.name AS tag_name, t.category AS tag_category
            FROM job_tags jt
            JOIN tags t ON t.id = jt.tag_id
            WHERE jt.job_id IN
            <foreach collection="jobIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY jt.job_id, t.category, jt.weight DESC, jt.tag_id
            </script>
            """)
    List<JobTagRow> selectByJobIds(@Param("jobIds") Collection<Long> jobIds);
}
