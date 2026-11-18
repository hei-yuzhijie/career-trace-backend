package com.yuzhijie.career.trace.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    /** MD5(password)，仅演示用；生产环境应使用 BCrypt/Argon2。 */
    private String password;
    private LocalDateTime createdAt;
}