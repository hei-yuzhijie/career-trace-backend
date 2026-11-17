package com.yuzhijie.career.trace.module.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    @Select("""
            SELECT * FROM resumes
            WHERE user_id = #{userId} AND file_hash = #{fileHash}
            LIMIT 1
            """)
    Resume selectByUserAndHash(@Param("userId") Long userId, @Param("fileHash") String fileHash);
}
