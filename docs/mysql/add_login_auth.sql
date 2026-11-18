-- =====================================================
-- 登录认证字段（对已有库执行一次）
-- 演示密码：123456
-- =====================================================
USE careertrace;

ALTER TABLE users
    ADD COLUMN password VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'MD5(password)，仅演示用'
    AFTER nickname;

UPDATE users
SET password = 'e10adc3949ba59abbe56e057f20f883e'
WHERE id IN (1, 2);
