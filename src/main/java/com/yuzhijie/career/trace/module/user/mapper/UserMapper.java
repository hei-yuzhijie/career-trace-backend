package com.yuzhijie.career.trace.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT * FROM users
            WHERE nickname = #{nickname}
            LIMIT 1
            """)
    User selectByNickname(@Param("nickname") String nickname);
}
