-- =====================================================
-- 职迹 CareerTrace 数据库初始化
-- MySQL 8.0+，InnoDB，utf8mb4
-- =====================================================
DROP DATABASE IF EXISTS careertrace;
CREATE DATABASE careertrace
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE careertrace;

-- 说明：所有主键用 BIGINT（有符号），避免 BIGINT UNSIGNED 在 Java Long 映射时的边界问题；
-- 外键按需保留，若团队约定不用物理外键，删掉 CONSTRAINT 段即可，索引保留。

-- -----------------------------------------------------
-- 1. users
-- -----------------------------------------------------
CREATE TABLE users (
                       id         BIGINT       NOT NULL AUTO_INCREMENT,
                       nickname   VARCHAR(50)  NOT NULL DEFAULT '',
                       created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户占位表';

-- -----------------------------------------------------
-- 2. companies
-- -----------------------------------------------------
CREATE TABLE companies (
                           id          BIGINT       NOT NULL AUTO_INCREMENT,
                           external_id VARCHAR(100) NULL COMMENT 'BOSS 侧公司 ID，用于去重',
                           name        VARCHAR(100) NOT NULL,
                           industry    VARCHAR(50)  NULL,
                           size        VARCHAR(50)  NULL COMMENT '如 10000人以上',
                           stage       VARCHAR(50)  NULL COMMENT '融资阶段',
                           created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           UNIQUE KEY uq_ext (external_id),
                           KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司主数据';

-- -----------------------------------------------------
-- 3. tags
-- -----------------------------------------------------
CREATE TABLE tags (
                      id         BIGINT       NOT NULL AUTO_INCREMENT,
                      name       VARCHAR(100) NOT NULL COMMENT '标签主名',
                      slug       VARCHAR(100) NOT NULL COMMENT '唯一标识，程序匹配用',
                      category   VARCHAR(50)  NOT NULL COMMENT 'skill/experience/preference/location/seniority/gap',
                      status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
                      created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      UNIQUE KEY uq_slug (slug),
                      KEY idx_category (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签字典';

-- -----------------------------------------------------
-- 4. tag_aliases
-- -----------------------------------------------------
CREATE TABLE tag_aliases (
                             id         BIGINT       NOT NULL AUTO_INCREMENT,
                             tag_id     BIGINT       NOT NULL,
                             alias      VARCHAR(100) NOT NULL COMMENT '别名原文',
                             alias_slug VARCHAR(100) NOT NULL COMMENT '别名归一化，精确匹配入口',
                             created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (id),
                             UNIQUE KEY uq_alias_slug (alias_slug),
                             KEY idx_tag (tag_id),
                             CONSTRAINT fk_alias_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签别名，大模型输出归一化入口';

-- -----------------------------------------------------
-- 5. jobs
-- -----------------------------------------------------
CREATE TABLE jobs (
                      id          BIGINT       NOT NULL AUTO_INCREMENT,
                      external_id VARCHAR(100) NOT NULL COMMENT 'BOSS 侧岗位 ID',
                      source      VARCHAR(30)  NOT NULL DEFAULT 'boss' COMMENT 'boss/manual',
                      company_id  BIGINT       NOT NULL,
                      title       VARCHAR(100) NOT NULL,
                      city        VARCHAR(50)  NOT NULL,
                      salary_min  INT          NULL COMMENT '单位 K',
                      salary_max  INT          NULL COMMENT '单位 K',
                      jd_text     TEXT         NOT NULL,
                      status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1在招 0下线',
                      created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      UNIQUE KEY uq_source_ext (source, external_id),
                      KEY idx_city_status (city, status),
                      KEY idx_company (company_id),
                      CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位主数据';

-- -----------------------------------------------------
-- 6. memory_items
-- -----------------------------------------------------
CREATE TABLE memory_items (
                              id          BIGINT      NOT NULL AUTO_INCREMENT,
                              user_id     BIGINT      NOT NULL,
                              content     TEXT        NOT NULL COMMENT '记忆原文',
                              source_type VARCHAR(30) NOT NULL COMMENT 'resume/interview/manual/review',
                              source_id   BIGINT      NULL COMMENT '来源对象 ID',
                              status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0删除',
                              created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (id),
                              KEY idx_user_status (user_id, status),
                              CONSTRAINT fk_mem_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职业记忆主表';

-- -----------------------------------------------------
-- 7. memory_tags（复合主键，MyBatis-Plus 需自定义 SQL）
-- -----------------------------------------------------
CREATE TABLE memory_tags (
                             memory_id   BIGINT       NOT NULL,
                             tag_id      BIGINT       NOT NULL,
                             polarity    TINYINT      NOT NULL DEFAULT 1 COMMENT '1正向 -1排除项',
                             proficiency TINYINT      NOT NULL DEFAULT 0 COMMENT '0-4，仅 skill/部分 experience 有效',
                             confidence  DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '大模型置信度 0-1',
                             evidence    VARCHAR(500) NULL COMMENT '支撑该标签的原文片段',
                             assessed_by VARCHAR(20)  NOT NULL DEFAULT 'llm' COMMENT 'llm/manual/review',
                             created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             PRIMARY KEY (memory_id, tag_id),
                             KEY idx_tag_memory (tag_id, memory_id),
                             KEY idx_memory_polarity (memory_id, polarity),
                             CONSTRAINT fk_mt_memory FOREIGN KEY (memory_id) REFERENCES memory_items(id) ON DELETE CASCADE,
                             CONSTRAINT fk_mt_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆↔标签';

-- -----------------------------------------------------
-- 8. job_tags（复合主键）
-- -----------------------------------------------------
CREATE TABLE job_tags (
                          job_id         BIGINT       NOT NULL,
                          tag_id         BIGINT       NOT NULL,
                          weight         DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '1.0必备 0.5加分',
                          required_level TINYINT      NOT NULL DEFAULT 0 COMMENT '仅 skill 类有意义',
                          created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (job_id, tag_id),
                          KEY idx_tag_job (tag_id, job_id),
                          CONSTRAINT fk_jt_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
                          CONSTRAINT fk_jt_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位↔标签';

-- -----------------------------------------------------
-- 9. user_skill_profile（复合主键，仅 skill 类）
-- -----------------------------------------------------
CREATE TABLE user_skill_profile (
                                    user_id        BIGINT       NOT NULL,
                                    tag_id         BIGINT       NOT NULL,
                                    level          TINYINT      NOT NULL DEFAULT 0,
                                    confidence     DECIMAL(3,2) NOT NULL DEFAULT 0.50,
                                    memory_count   INT          NOT NULL DEFAULT 0,
                                    best_memory_id BIGINT       NULL,
                                    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    PRIMARY KEY (user_id, tag_id),
                                    KEY idx_tag_level (tag_id, level),
                                    CONSTRAINT fk_usp_user FOREIGN KEY (user_id) REFERENCES users(id),
                                    CONSTRAINT fk_usp_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 skill 聚合，异步维护';

-- -----------------------------------------------------
-- 10. resumes（简历诊断）
-- -----------------------------------------------------
CREATE TABLE resumes (
                          id                BIGINT       NOT NULL AUTO_INCREMENT,
                          user_id           BIGINT       NOT NULL,
                          job_id            BIGINT       NULL COMMENT '可选目标岗位',
                          file_hash         VARCHAR(64)  NOT NULL COMMENT '文件内容 SHA-256',
                          original_filename VARCHAR(255) NOT NULL,
                          content_type      VARCHAR(100) NULL,
                          file_key          VARCHAR(500) NOT NULL COMMENT 'storage/resumes 下的相对路径',
                          text_content      MEDIUMTEXT   NULL COMMENT 'Tika 抽取的纯文本',
                          status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0待分析 1分析中 2完成 3失败',
                          overall_score     INT          NULL,
                          analysis_json     LONGTEXT     NULL COMMENT '大模型结构化输出快照',
                          retry_count       INT          NOT NULL DEFAULT 0,
                          attempt_id        VARCHAR(64)  NULL,
                          prompt_version    VARCHAR(32)  NULL,
                          created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          UNIQUE KEY uq_resume_user_hash (user_id, file_hash),
                          KEY idx_resume_user_status (user_id, status, created_at),
                          KEY idx_resume_job (job_id),
                          CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users(id),
                          CONSTRAINT fk_resume_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历文件与诊断结果';

-- -----------------------------------------------------
-- 11. resume_advices（简历改进建议）
-- -----------------------------------------------------
CREATE TABLE resume_advices (
                                id             BIGINT       NOT NULL AUTO_INCREMENT,
                                resume_id      BIGINT       NOT NULL,
                                category       VARCHAR(20)  NOT NULL COMMENT '内容/格式/技能/项目',
                                priority       VARCHAR(10)  NOT NULL COMMENT '高/中/低',
                                issue          VARCHAR(500) NOT NULL,
                                recommendation TEXT         NOT NULL,
                                evidence       VARCHAR(500) NULL,
                                created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (id),
                                KEY idx_advice_resume_priority (resume_id, priority),
                                CONSTRAINT fk_advice_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历改进建议';

-- -----------------------------------------------------
-- 12. resume_memory_candidates（简历转职业记忆候选）
-- -----------------------------------------------------
CREATE TABLE resume_memory_candidates (
                                           id         BIGINT   NOT NULL AUTO_INCREMENT,
                                           resume_id  BIGINT   NOT NULL,
                                           user_id    BIGINT   NOT NULL,
                                           content    TEXT     NOT NULL,
                                           tags_json  LONGTEXT NOT NULL COMMENT '标签候选 JSON 数组',
                                           status     TINYINT  NOT NULL DEFAULT 0 COMMENT '0待确认 1已导入 2已忽略',
                                           memory_id  BIGINT   NULL COMMENT '确认后写入的 memory_items.id',
                                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (id),
                                           KEY idx_rmc_resume_status (resume_id, status),
                                           CONSTRAINT fk_rmc_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
                                           CONSTRAINT fk_rmc_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历转职业记忆候选';

-- -----------------------------------------------------
-- 13. interview_sessions（模拟面试会话）
-- -----------------------------------------------------
CREATE TABLE interview_sessions (
                                    id                BIGINT       NOT NULL AUTO_INCREMENT,
                                    user_id           BIGINT       NOT NULL,
                                    resume_id         BIGINT       NULL,
                                    job_id            BIGINT       NULL,
                                    jd_text           TEXT         NULL,
                                    direction_tag_ids VARCHAR(500) NOT NULL COMMENT '1-6 个 skill 标签 ID，逗号分隔',
                                    difficulty        VARCHAR(20)  NOT NULL COMMENT 'basic/intermediate/advanced',
                                    question_count    INT          NOT NULL,
                                    follow_up_count   INT          NOT NULL DEFAULT 0,
                                    status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0生成中 1待作答 2评估中 3已完成 4失败',
                                    total_score       INT          NULL,
                                    summary           TEXT         NULL,
                                    strengths         LONGTEXT     NULL COMMENT 'JSON 数组',
                                    improvements      LONGTEXT     NULL COMMENT 'JSON 数组',
                                    prompt_version    VARCHAR(32)  NULL,
                                    attempt_id        VARCHAR(64)  NULL,
                                    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    PRIMARY KEY (id),
                                    KEY idx_is_user_status (user_id, status, created_at),
                                    KEY idx_is_resume (resume_id),
                                    KEY idx_is_job (job_id),
                                    CONSTRAINT fk_is_user FOREIGN KEY (user_id) REFERENCES users(id),
                                    CONSTRAINT fk_is_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE SET NULL,
                                    CONSTRAINT fk_is_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟面试会话';

-- -----------------------------------------------------
-- 14. interview_questions（题目：主问题 + 追问）
-- -----------------------------------------------------
CREATE TABLE interview_questions (
                                     id               BIGINT       NOT NULL AUTO_INCREMENT,
                                     session_id       BIGINT       NOT NULL,
                                     question_index   INT          NOT NULL COMMENT '0-based，会话内唯一',
                                     parent_index     INT          NOT NULL COMMENT '主问题填自身，追问填主问题 index',
                                     source           VARCHAR(10)  NOT NULL COMMENT 'RESUME/SKILL',
                                     tag_id           BIGINT       NULL,
                                     direction_name   VARCHAR(100) NULL,
                                     topic_summary    VARCHAR(50)  NOT NULL,
                                     title            TEXT         NOT NULL,
                                     reference_answer TEXT         NULL,
                                     points           TEXT         NULL,
                                     created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uq_iq_session_index (session_id, question_index),
                                     KEY idx_iq_session_parent (session_id, parent_index),
                                     KEY idx_iq_tag (tag_id),
                                     CONSTRAINT fk_iq_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_iq_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目';

-- -----------------------------------------------------
-- 15. interview_answers（逐题答案）
-- -----------------------------------------------------
CREATE TABLE interview_answers (
                                   id             BIGINT      NOT NULL AUTO_INCREMENT,
                                   session_id     BIGINT      NOT NULL,
                                   question_index INT         NOT NULL,
                                   answer_text    MEDIUMTEXT  NOT NULL,
                                   answered_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uq_ia_session_question (session_id, question_index),
                                   CONSTRAINT fk_ia_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户逐题答案';

-- -----------------------------------------------------
-- 16. interview_evaluations（逐题评估）
-- -----------------------------------------------------
CREATE TABLE interview_evaluations (
                                       id               BIGINT   NOT NULL AUTO_INCREMENT,
                                       session_id       BIGINT   NOT NULL,
                                       question_index   INT      NOT NULL,
                                       score            INT      NOT NULL,
                                       feedback         TEXT     NOT NULL,
                                       reference_answer TEXT     NULL,
                                       points           TEXT     NULL,
                                       evaluated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uq_ie_session_question (session_id, question_index),
                                       CONSTRAINT fk_ie_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐题评估结果';

-- -----------------------------------------------------
-- 17. interview_memory_candidates（面试转职业记忆候选）
-- -----------------------------------------------------
CREATE TABLE interview_memory_candidates (
                                              id          BIGINT       NOT NULL AUTO_INCREMENT,
                                              session_id  BIGINT       NOT NULL,
                                              user_id     BIGINT       NOT NULL,
                                              tag_id      BIGINT       NOT NULL,
                                              content     TEXT         NOT NULL,
                                              proficiency TINYINT      NOT NULL DEFAULT 0,
                                              confidence  DECIMAL(3,2) NOT NULL DEFAULT 0.80,
                                              evidence    VARCHAR(500) NULL,
                                              status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已忽略',
                                              memory_id   BIGINT       NULL,
                                              created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              PRIMARY KEY (id),
                                              KEY idx_imc_session_status (session_id, status),
                                              CONSTRAINT fk_imc_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE,
                                              CONSTRAINT fk_imc_user FOREIGN KEY (user_id) REFERENCES users(id),
                                              CONSTRAINT fk_imc_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评估转职业记忆候选';