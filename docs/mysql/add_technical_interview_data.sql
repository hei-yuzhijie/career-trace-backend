-- =====================================================
-- 技术岗题库方向补充数据（可重复执行）
-- 适用：已经在使用现有 careertrace 数据，不想重建库
-- =====================================================
USE careertrace;

-- 技术方向标签
INSERT IGNORE INTO tags (id, name, slug, category) VALUES
    (201, 'Java', 'java', 'skill'),
    (202, 'MySQL', 'mysql', 'skill'),
    (203, 'Redis', 'redis', 'skill'),
    (204, 'Spring', 'spring', 'skill'),
    (205, 'AI Agent', 'ai_agent', 'skill'),
    (206, '系统设计', 'system_design', 'skill'),
    (207, '算法与数据结构', 'algorithm', 'skill'),
    (208, '前端工程', 'frontend', 'skill');

-- 技术方向别名
INSERT IGNORE INTO tag_aliases (tag_id, alias, alias_slug) VALUES
    (201, 'Java', 'java'),
    (201, 'Java 后端', 'java_hou_duan'),
    (201, 'Java开发', 'java_kai_fa'),
    (202, 'MySQL', 'mysql'),
    (202, 'Mysql', 'mysql_2'),
    (203, 'Redis', 'redis'),
    (203, '缓存', 'huan_cun'),
    (204, 'Spring', 'spring'),
    (204, 'Spring Boot', 'spring_boot'),
    (205, 'AI Agent', 'ai_agent'),
    (205, 'Agent', 'agent'),
    (205, '智能体', 'zhi_neng_ti'),
    (206, '系统设计', 'system_design'),
    (206, '架构设计', 'jia_gou_she_ji'),
    (207, '算法', 'algorithm'),
    (207, '数据结构', 'shu_ju_jie_gou'),
    (208, '前端', 'frontend'),
    (208, '前端工程', 'qian_duan_gong_cheng');

-- Java 后端岗位
INSERT IGNORE INTO jobs (id, external_id, source, company_id, title, city, salary_min, salary_max, jd_text, status) VALUES
    (7, 'boss_j_2007', 'boss', 1, 'Java 后端开发', '杭州', 18, 30,
     '负责后端服务设计与开发；要求 Java、Spring Boot、MySQL、Redis；有 AI Agent/RAG 经验加分；关注性能、稳定性和工程化。', 1);

INSERT IGNORE INTO job_tags (job_id, tag_id, weight, required_level) VALUES
    (7, 101, 1.00, 0),
    (7, 201, 1.00, 3),
    (7, 204, 1.00, 3),
    (7, 202, 1.00, 2),
    (7, 203, 1.00, 2),
    (7, 205, 0.50, 2),
    (7, 206, 0.50, 2),
    (7, 207, 0.50, 2);

-- 用户 2 的技术记忆与标签
INSERT IGNORE INTO memory_items (id, user_id, content, source_type, source_id, status) VALUES
    (7, 2, '熟悉 Java、Spring Boot、MySQL、Redis，做过一个基于 RAG 的 AI Agent 项目', 'manual', NULL, 1);

INSERT IGNORE INTO memory_tags (memory_id, tag_id, polarity, proficiency, confidence, evidence, assessed_by) VALUES
    (7, 201, 1, 3, 0.88, '熟悉 Java', 'manual'),
    (7, 204, 1, 3, 0.86, '熟悉 Spring Boot', 'manual'),
    (7, 202, 1, 3, 0.84, '熟悉 MySQL', 'manual'),
    (7, 203, 1, 2, 0.82, '熟悉 Redis', 'manual'),
    (7, 205, 1, 2, 0.80, '做过基于 RAG 的 AI Agent 项目', 'manual');

INSERT IGNORE INTO user_skill_profile (user_id, tag_id, level, confidence, memory_count, best_memory_id) VALUES
    (2, 201, 3, 0.88, 1, 7),
    (2, 204, 3, 0.86, 1, 7),
    (2, 202, 3, 0.84, 1, 7),
    (2, 203, 2, 0.82, 1, 7),
    (2, 205, 2, 0.80, 1, 7);
