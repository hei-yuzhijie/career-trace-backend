package com.yuzhijie.career.trace.module.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.resume.entity.ResumeAdvice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResumeAdviceMapper extends BaseMapper<ResumeAdvice> {

    @Select("""
            SELECT * FROM resume_advices
            WHERE resume_id = #{resumeId}
            ORDER BY FIELD(priority, '高', '中', '低'), id
            """)
    List<ResumeAdvice> selectByResumeId(@Param("resumeId") Long resumeId);
}
