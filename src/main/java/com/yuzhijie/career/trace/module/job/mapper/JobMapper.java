package com.yuzhijie.career.trace.module.job.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JobMapper {

    @Select("""
            <script>
            SELECT j.id AS job_id, j.external_id, j.source, j.company_id,
                   c.name AS company_name, c.industry AS company_industry,
                   c.size AS company_size, c.stage AS company_stage,
                   j.title, j.city, j.salary_min, j.salary_max, j.jd_text, j.status, j.created_at
            FROM jobs j
            LEFT JOIN companies c ON c.id = j.company_id
            WHERE j.status = 1
            <if test="city != null and city != ''"> AND j.city = #{city}</if>
            <if test="keyword != null and keyword != ''"> AND j.title LIKE CONCAT('%', #{keyword}, '%')</if>
            <if test="companyId != null"> AND j.company_id = #{companyId}</if>
            ORDER BY j.created_at DESC, j.id DESC
            </script>
            """)
    Page<JobRow> selectJobPage(Page<JobRow> page,
                               @Param("city") String city,
                               @Param("keyword") String keyword,
                               @Param("companyId") Long companyId);

    @Select("""
            SELECT j.id AS job_id, j.external_id, j.source, j.company_id,
                   c.name AS company_name, c.industry AS company_industry,
                   c.size AS company_size, c.stage AS company_stage,
                   j.title, j.city, j.salary_min, j.salary_max, j.jd_text, j.status, j.created_at
            FROM jobs j
            LEFT JOIN companies c ON c.id = j.company_id
            WHERE j.id = #{jobId} AND j.status = 1
            """)
    JobRow selectJobRowById(@Param("jobId") Long jobId);

    /**
     * 推荐候选岗位（在招）。city 为额外的收窄条件。
     */
    @Select("""
            <script>
            SELECT j.id AS job_id, j.external_id, j.source, j.company_id,
                   c.name AS company_name, c.industry AS company_industry,
                   c.size AS company_size, c.stage AS company_stage,
                   j.title, j.city, j.salary_min, j.salary_max, j.jd_text, j.status, j.created_at
            FROM jobs j
            LEFT JOIN companies c ON c.id = j.company_id
            WHERE j.status = 1
            <if test="city != null and city != ''"> AND j.city = #{city}</if>
            ORDER BY j.id
            </script>
            """)
    List<JobRow> selectActiveJobRows(@Param("city") String city);
}
