-- =====================================================
-- 模拟数据
-- =====================================================
USE careertrace;

-- ---------- users ----------
-- 演示密码统一为 123456，MD5 = e10adc3949ba59abbe56e057f20f883e
INSERT INTO users (id, nickname, password) VALUES
                                     (1, '陈默', 'e10adc3949ba59abbe56e057f20f883e'),
                                     (2, '林一', 'e10adc3949ba59abbe56e057f20f883e');

-- ---------- companies ----------
INSERT INTO companies (id, external_id, name, industry, size, stage) VALUES
                                                                         (1, 'boss_c_1001', '字节跳动',     '互联网',   '10000人以上', '已上市'),
                                                                         (2, 'boss_c_1002', '网易',         '互联网',   '10000人以上', '已上市'),
                                                                         (3, 'boss_c_1003', '阿里本地生活', '互联网',   '10000人以上', '已上市'),
                                                                         (4, 'boss_c_1004', '某上海内容公司', '互联网', '100-499人',   'B轮'),
                                                                         (5, 'boss_c_1005', '某杭州电商公司', '互联网', '100-499人',   'A轮'),
                                                                         (6, 'boss_c_1006', '某快消品牌',    '快消',     '500-999人',   '未融资');

-- ---------- tags ----------
-- location
INSERT INTO tags (id, name, slug, category) VALUES
                                                (101, '杭州', 'hangzhou', 'location'),
                                                (102, '上海', 'shanghai', 'location');
-- preference
INSERT INTO tags (id, name, slug, category) VALUES
                                                (103, '大小周',   'da_xiao_zhou', 'preference'),
                                                (104, '带教',     'dai_jiao',     'preference'),
                                                (105, '销售',     'xiao_shou',    'preference'),
                                                (114, '互联网',   'hu_lian_wang', 'preference');
-- skill
INSERT INTO tags (id, name, slug, category) VALUES
                                                (106, '内容策划', 'nei_rong_ce_hua', 'skill'),
                                                (107, '数据复盘', 'shu_ju_fu_pan',   'skill'),
                                                (108, 'A/B 测试', 'ab_ce_shi',       'skill'),
                                                (109, '内容排期', 'nei_rong_pai_qi', 'skill'),
                                                (110, '用户增长', 'yong_hu_zeng_zhang', 'skill'),
                                                (115, '私域运营', 'si_yu_yun_ying',  'skill'),
                                                (116, '品牌活动', 'pin_pai_huo_dong', 'skill'),
                                                (117, '短视频脚本', 'duan_shi_pin_jiao_ben', 'skill'),
                                                -- 技术岗方向
                                                (201, 'Java', 'java', 'skill'),
                                                (202, 'MySQL', 'mysql', 'skill'),
                                                (203, 'Redis', 'redis', 'skill'),
                                                (204, 'Spring', 'spring', 'skill'),
                                                (205, 'AI Agent', 'ai_agent', 'skill'),
                                                (206, '系统设计', 'system_design', 'skill'),
                                                (207, '算法与数据结构', 'algorithm', 'skill'),
                                                (208, '前端工程', 'frontend', 'skill');
-- experience
INSERT INTO tags (id, name, slug, category) VALUES
    (111, '新媒体运营', 'xin_mei_ti_yun_ying', 'experience');
-- seniority
INSERT INTO tags (id, name, slug, category) VALUES
                                                (112, '本科',   'ben_ke',   'seniority'),
                                                (113, '3-5年',  '3_5_nian', 'seniority');

-- ---------- tag_aliases ----------
-- 约定：新建 tags 时同时把主名作为一条 alias 写入
INSERT INTO tag_aliases (tag_id, alias, alias_slug) VALUES
                                                        -- 杭州
                                                        (101, '杭州', 'hangzhou'),
                                                        (101, '想留在杭州', 'xiang_liu_zai_hangzhou'),
                                                        (101, '杭州工作', 'hangzhou_gong_zuo'),
                                                        -- 上海
                                                        (102, '上海', 'shanghai'),
                                                        -- 大小周
                                                        (103, '大小周', 'da_xiao_zhou'),
                                                        (103, '接受大小周', 'jie_shou_da_xiao_zhou'),
                                                        (103, '能接受大小周', 'neng_jie_shou_da_xiao_zhou'),
                                                        (103, '大小周没问题', 'da_xiao_zhou_mei_wen_ti'),
                                                        -- 带教
                                                        (104, '带教', 'dai_jiao'),
                                                        (104, '希望有带教', 'xi_wang_you_dai_jiao'),
                                                        (104, '有带教', 'you_dai_jiao'),
                                                        -- 销售
                                                        (105, '销售', 'xiao_shou'),
                                                        (105, '销售岗', 'xiao_shou_gang'),
                                                        (105, '地推', 'di_tui'),
                                                        -- 互联网
                                                        (114, '互联网', 'hu_lian_wang'),
                                                        -- 内容策划
                                                        (106, '内容策划', 'nei_rong_ce_hua'),
                                                        (106, '内容选题', 'nei_rong_xuan_ti'),
                                                        -- 数据复盘
                                                        (107, '数据复盘', 'shu_ju_fu_pan'),
                                                        (107, '数据回顾', 'shu_ju_hui_gu'),
                                                        -- A/B 测试
                                                        (108, 'A/B 测试', 'ab_ce_shi'),
                                                        (108, 'AB测试', 'ab_ce_shi_2'),
                                                        (108, 'A/B test', 'ab_test'),
                                                        -- 内容排期
                                                        (109, '内容排期', 'nei_rong_pai_qi'),
                                                        -- 用户增长
                                                        (110, '用户增长', 'yong_hu_zeng_zhang'),
                                                        -- 私域运营
                                                        (115, '私域运营', 'si_yu_yun_ying'),
                                                        (115, '私域', 'si_yu'),
                                                        -- 品牌活动
                                                        (116, '品牌活动', 'pin_pai_huo_dong'),
                                                        -- 短视频脚本
                                                        (117, '短视频脚本', 'duan_shi_pin_jiao_ben'),
                                                        -- 新媒体运营
                                                        (111, '新媒体运营', 'xin_mei_ti_yun_ying'),
                                                        -- 本科
                                                        (112, '本科', 'ben_ke'),
                                                        -- 3-5年
                                                        (113, '3-5年', '3_5_nian'),
                                                        -- Java
                                                        (201, 'Java', 'java'),
                                                        (201, 'Java 后端', 'java_hou_duan'),
                                                        (201, 'Java开发', 'java_kai_fa'),
                                                        -- MySQL
                                                        (202, 'MySQL', 'mysql'),
                                                        (202, 'Mysql', 'mysql_2'),
                                                        -- Redis
                                                        (203, 'Redis', 'redis'),
                                                        (203, '缓存', 'huan_cun'),
                                                        -- Spring
                                                        (204, 'Spring', 'spring'),
                                                        (204, 'Spring Boot', 'spring_boot'),
                                                        -- AI Agent
                                                        (205, 'AI Agent', 'ai_agent'),
                                                        (205, 'Agent', 'agent'),
                                                        (205, '智能体', 'zhi_neng_ti'),
                                                        -- 系统设计
                                                        (206, '系统设计', 'system_design'),
                                                        (206, '架构设计', 'jia_gou_she_ji'),
                                                        -- 算法
                                                        (207, '算法', 'algorithm'),
                                                        (207, '数据结构', 'shu_ju_jie_gou'),
                                                        -- 前端
                                                        (208, '前端', 'frontend'),
                                                        (208, '前端工程', 'qian_duan_gong_cheng');

-- ---------- jobs ----------
INSERT INTO jobs (id, external_id, source, company_id, title, city, salary_min, salary_max, jd_text, status) VALUES
                                                                                                                 (1, 'boss_j_2001', 'boss', 1, '内容运营（校招）', '杭州', 15, 25,
                                                                                                                  '负责品牌内容矩阵的日常运营与选题策划；结合数据复盘优化内容结构；参与 A/B 测试与私域运营。', 1),
                                                                                                                 (2, 'boss_j_2002', 'boss', 4, '内容运营', '上海', 14, 22,
                                                                                                                  '负责新媒体内容策划与排期；希望有带教体系；本科及以上。', 1),
                                                                                                                 (3, 'boss_j_2003', 'boss', 5, '内容运营', '杭州', 12, 18,
                                                                                                                  '负责小红书/公众号内容策划与更新；接受大小周；有带教。', 1),
                                                                                                                 (4, 'boss_j_2004', 'boss', 5, '销售专员', '杭州', 8, 15,
                                                                                                                  '负责地推与客户开发；销售岗，底薪+提成。', 1),
                                                                                                                 (5, 'boss_j_2005', 'boss', 1, '品牌内容运营', '杭州', 16, 26,
                                                                                                                  '负责品牌内容矩阵运营；需要短视频脚本与品牌活动经验；本科，3-5 年。', 1),
                                                                                                                 (6, 'boss_j_2006', 'boss', 2, '新媒体运营', '杭州', 10, 16,
                                                                                                                  '负责公众号周更与用户增长；需要内容排期与数据复盘能力。', 1),
                                                                                                                 (7, 'boss_j_2007', 'boss', 1, 'Java 后端开发', '杭州', 18, 30,
                                                                                                                  '负责后端服务设计与开发；要求 Java、Spring Boot、MySQL、Redis；有 AI Agent/RAG 经验加分；关注性能、稳定性和工程化。', 1);

-- ---------- memory_items ----------
INSERT INTO memory_items (id, user_id, content, source_type, source_id, status) VALUES
                                                                                    (1, 1, '想留在杭州，能接受大小周，希望有带教', 'manual', NULL, 1),
                                                                                    (2, 1, '不考虑销售、地推类岗位', 'manual', NULL, 1),
                                                                                    (3, 1, '9 月做了 3 篇小红书笔记，其中 1 篇阅读 4.2 万，带来 300+ 私域咨询', 'resume', NULL, 1),
                                                                                    (4, 1, '暑期运营实习 3 个月，独立负责公众号周更，粉丝从 1.2k 到 3.6k', 'resume', NULL, 1),
                                                                                    (5, 1, '每周复盘完读率与涨粉曲线，用数据指导选题', 'resume', NULL, 1),
                                                                                    (6, 2, '希望做后端开发，最好在杭州', 'manual', NULL, 1),
                                                                                    (7, 2, '熟悉 Java、Spring Boot、MySQL、Redis，做过一个基于 RAG 的 AI Agent 项目', 'manual', NULL, 1);

-- ---------- memory_tags ----------
-- 用户 1：偏好 / 排除
INSERT INTO memory_tags (memory_id, tag_id, polarity, proficiency, confidence, evidence, assessed_by) VALUES
                                                                                                          (1, 101, 1, 0, 0.98, '想留在杭州', 'llm'),
                                                                                                          (1, 103, 1, 0, 0.95, '能接受大小周', 'llm'),
                                                                                                          (1, 104, 1, 0, 0.93, '希望有带教', 'llm'),
                                                                                                          (2, 105, -1, 0, 0.97, '不考虑销售、地推类岗位', 'llm'),
-- 用户 1：skill / experience（带熟练度与证据）
                                                                                                          (3, 106, 1, 3, 0.92, '做了 3 篇小红书笔记，1 篇阅读 4.2 万', 'llm'),
                                                                                                          (3, 115, 1, 2, 0.85, '带来 300+ 私域咨询', 'llm'),
                                                                                                          (4, 109, 1, 2, 0.86, '独立负责公众号周更', 'llm'),
                                                                                                          (4, 110, 1, 2, 0.84, '粉丝从 1.2k 到 3.6k', 'llm'),
                                                                                                          (4, 111, 1, 3, 0.88, '暑期运营实习 3 个月', 'llm'),
                                                                                                          (5, 107, 1, 3, 0.90, '每周复盘完读率与涨粉曲线', 'llm'),
-- 用户 2
                                                                                                          (6, 101, 1, 0, 0.96, '最好在杭州', 'llm'),
                                                                                                          (7, 201, 1, 3, 0.88, '熟悉 Java', 'manual'),
                                                                                                          (7, 204, 1, 3, 0.86, '熟悉 Spring Boot', 'manual'),
                                                                                                          (7, 202, 1, 3, 0.84, '熟悉 MySQL', 'manual'),
                                                                                                          (7, 203, 1, 2, 0.82, '熟悉 Redis', 'manual'),
                                                                                                          (7, 205, 1, 2, 0.80, '做过基于 RAG 的 AI Agent 项目', 'manual');

-- ---------- job_tags ----------
-- J1 字节内容运营（杭州）
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (1, 101, 1.00, 0),   -- 杭州（硬门槛）
                                                                  (1, 104, 0.50, 0),   -- 带教（加分）
                                                                  (1, 106, 1.00, 3),   -- 内容策划（必备，要求 L3）
                                                                  (1, 107, 1.00, 2),   -- 数据复盘（必备，要求 L2）
                                                                  (1, 108, 0.50, 2),   -- A/B 测试（加分）
                                                                  (1, 115, 0.50, 2),   -- 私域运营（加分）
                                                                  (1, 112, 1.00, 0),   -- 本科
                                                                  (1, 113, 1.00, 0);   -- 3-5 年
-- J2 上海内容运营
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (2, 102, 1.00, 0),
                                                                  (2, 104, 0.50, 0),
                                                                  (2, 106, 1.00, 2),
                                                                  (2, 112, 1.00, 0);
-- J3 杭州内容运营（大小周）
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (3, 101, 1.00, 0),
                                                                  (3, 103, 0.50, 0),
                                                                  (3, 104, 0.50, 0),
                                                                  (3, 106, 1.00, 2),
                                                                  (3, 115, 0.50, 2);
-- J4 杭州销售（会被用户排除项过滤）
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (4, 101, 1.00, 0),
                                                                  (4, 105, 1.00, 0);
-- J5 字节品牌内容运营
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (5, 101, 1.00, 0),
                                                                  (5, 106, 1.00, 3),
                                                                  (5, 116, 0.50, 2),
                                                                  (5, 117, 0.50, 2),
                                                                  (5, 112, 1.00, 0),
                                                                  (5, 113, 1.00, 0);
-- J6 网易新媒体运营
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (6, 101, 1.00, 0),
                                                                  (6, 109, 1.00, 2),
                                                                  (6, 110, 1.00, 2),
                                                                  (6, 107, 0.50, 2),
                                                                  (6, 111, 1.00, 0);
-- J7 字节 Java 后端
INSERT INTO job_tags (job_id, tag_id, weight, required_level) VALUES
                                                                  (7, 101, 1.00, 0),   -- 杭州
                                                                  (7, 201, 1.00, 3),   -- Java
                                                                  (7, 204, 1.00, 3),   -- Spring
                                                                  (7, 202, 1.00, 2),   -- MySQL
                                                                  (7, 203, 1.00, 2),   -- Redis
                                                                  (7, 205, 0.50, 2),   -- AI Agent（加分）
                                                                  (7, 206, 0.50, 2),   -- 系统设计（加分）
                                                                  (7, 207, 0.50, 2);   -- 算法（加分）

-- ---------- user_skill_profile（仅 skill 类） ----------
INSERT INTO user_skill_profile (user_id, tag_id, level, confidence, memory_count, best_memory_id) VALUES
                                                                                                      (1, 106, 3, 0.92, 1, 3),
                                                                                                      (1, 107, 3, 0.90, 1, 5),
                                                                                                      (1, 109, 2, 0.86, 1, 4),
                                                                                                      (1, 110, 2, 0.84, 1, 4),
                                                                                                      (1, 115, 2, 0.85, 1, 3),
                                                                                                      (2, 201, 3, 0.88, 1, 7),
                                                                                                      (2, 204, 3, 0.86, 1, 7),
                                                                                                      (2, 202, 3, 0.84, 1, 7),
                                                                                                      (2, 203, 2, 0.82, 1, 7),
                                                                                                      (2, 205, 2, 0.80, 1, 7);

-- ---------- resumes（简历诊断示例） ----------
INSERT INTO resumes (id, user_id, job_id, file_hash, original_filename, content_type, file_key, text_content,
                     status, overall_score, analysis_json, retry_count, prompt_version) VALUES
    (1, 1, 3, 'demo_resume_hash_001', '陈默-内容运营-简历.pdf', 'application/pdf',
     '1/20261114/demo-resume.pdf',
     '陈默，内容运营。2025 年 9 月做了 3 篇小红书笔记，其中 1 篇阅读 4.2 万，带来 300+ 私域咨询。暑期运营实习 3 个月，独立负责公众号周更，粉丝从 1.2k 到 3.6k。每周复盘完读率与涨粉曲线，用数据指导选题。',
     2, 82,
     '{"scoreDetail":{"experienceDepth":27,"achievement":15,"skillMatch":17,"content":12,"expression":11},"summary":"内容运营经历有成果，但项目背景与复盘过程偏少。","strengths":["有可量化内容成果","地点与目标岗位一致"],"suggestions":[{"category":"项目","priority":"高","issue":"项目成果只有阅读量，没有业务影响","recommendation":"如果有私域咨询/转化数据，补充真实数据；没有则描述影响范围，不要编造。","evidence":"1 篇阅读 4.2 万"},{"category":"技能","priority":"中","issue":"数据分析缺少过程","recommendation":"补充复盘指标与调整动作，形成可复用的方法。","evidence":"每周复盘完读率与涨粉曲线"}],"memories":[{"content":"独立负责公众号周更，粉丝从 1.2k 增长到 3.6k","tags":[{"name":"内容策划","category":"skill","proficiency":3,"confidence":0.92,"evidence":"独立负责公众号周更"}]},{"content":"9 月做了 3 篇小红书笔记，1 篇阅读 4.2 万，带来 300+ 私域咨询","tags":[{"name":"内容策划","category":"skill","proficiency":3,"confidence":0.90,"evidence":"1 篇阅读 4.2 万"},{"name":"私域运营","category":"skill","proficiency":2,"confidence":0.85,"evidence":"带来 300+ 私域咨询"}]}]}',
     0, 'v0.1');

INSERT INTO resume_advices (id, resume_id, category, priority, issue, recommendation, evidence) VALUES
    (1, 1, '项目', '高', '项目成果只有阅读量，没有业务影响', '如果有私域咨询/转化数据，补充真实数据；没有则描述影响范围，不要编造。', '1 篇阅读 4.2 万'),
    (2, 1, '技能', '中', '数据分析缺少过程', '补充复盘指标与调整动作，形成可复用的方法。', '每周复盘完读率与涨粉曲线');

INSERT INTO resume_memory_candidates (id, resume_id, user_id, content, tags_json, status) VALUES
    (1, 1, 1, '独立负责公众号周更，粉丝从 1.2k 增长到 3.6k',
     '[{"name":"内容策划","category":"skill","proficiency":3,"confidence":0.92,"evidence":"独立负责公众号周更"}]', 0),
    (2, 1, 1, '9 月做了 3 篇小红书笔记，1 篇阅读 4.2 万，带来 300+ 私域咨询',
     '[{"name":"内容策划","category":"skill","proficiency":3,"confidence":0.90,"evidence":"1 篇阅读 4.2 万"},{"name":"私域运营","category":"skill","proficiency":2,"confidence":0.85,"evidence":"带来 300+ 私域咨询"}]', 0);

-- ---------- interview_sessions（模拟面试示例） ----------
INSERT INTO interview_sessions (id, user_id, resume_id, job_id, jd_text, direction_tag_ids, difficulty,
                                question_count, follow_up_count, status, total_score, summary, strengths,
                                improvements, prompt_version) VALUES
    (1, 1, 1, 3, '负责小红书/公众号内容策划与更新；接受大小周；有带教。', '[106,107]', 'intermediate',
     2, 1, 3, 76, '内容策划基础扎实，但数据复盘深度不足。',
     '["选题拆解有条理","能结合用户画像"]',
     '["补充 A/B 测试案例","用数据说明调整效果"]',
     'v0.1'),
    (2, 1, 1, 3, '负责小红书/公众号内容策划与更新；接受大小周；有带教。', '[106,109]', 'basic',
     2, 0, 1, NULL, NULL, NULL, NULL, 'v0.1');

INSERT INTO interview_questions (id, session_id, question_index, parent_index, source, tag_id, direction_name,
                                 topic_summary, title, reference_answer, points) VALUES
    (1, 1, 0, 0, 'SKILL', 106, '内容策划', '内容选题拆解', '请结合一个项目，说明你如何拆解内容选题？',
     '按用户画像、历史数据、目标平台建立选题池，再用小流量验证。', '用户画像、选题池、小流量验证'),
    (2, 1, 1, 0, 'SKILL', 106, '内容策划', '数据不达预期的调整', '如果阅读量不达预期，你会怎么调整？',
     '先定位是选题、标题、封面还是分发问题，再做单变量实验。', '定位问题、单变量实验、复盘'),
    (3, 1, 2, 2, 'SKILL', 107, '数据复盘', '复盘指标选择', '你通常用哪些指标复盘内容效果？',
     '阅读完成率、互动率、涨粉、私域咨询/转化等，结合目标选择。', '指标与目标对齐、转化'),
    (4, 1, 3, 2, 'SKILL', 107, '数据复盘', '复盘结论落地', '复盘后如何把结论变成下一次行动？',
     '把结论写成选题/标题/分发规则，下一次实验验证。', '结论结构化、行动闭环'),
    (5, 2, 0, 0, 'SKILL', 106, '内容策划', '内容选题拆解', '请说明你如何为一个新账号做内容选题？',
     NULL, NULL),
    (6, 2, 1, 1, 'SKILL', 109, '内容排期', '内容排期方法', '你如何安排一周的内容排期？',
     NULL, NULL);

INSERT INTO interview_answers (id, session_id, question_index, answer_text) VALUES
    (1, 1, 0, '我先按用户画像拆选题，再用历史数据筛掉低效方向，最后用小流量验证。'),
    (2, 1, 1, '我会先看是选题还是标题问题，再单独改一个变量做对比。'),
    (3, 1, 2, '主要看阅读完成率、互动率、涨粉和私域咨询，目标是转化时更关注后两项。'),
    (4, 1, 3, '把复盘结论写成下一次的选题和标题规则，再用一周数据验证。');

INSERT INTO interview_evaluations (id, session_id, question_index, score, feedback, reference_answer, points) VALUES
    (1, 1, 0, 82, '结构清晰，能结合用户画像，但缺少失败案例。', '按用户画像、历史数据、目标平台建立选题池，再用小流量验证。', '用户画像、选题池、小流量验证'),
    (2, 1, 1, 78, '有单变量意识，但定位过程可以更具体。', '先定位是选题、标题、封面还是分发问题，再做单变量实验。', '定位问题、单变量实验、复盘'),
    (3, 1, 2, 70, '指标覆盖较全，但缺少与目标的优先级。', '阅读完成率、互动率、涨粉、私域咨询/转化等，结合目标选择。', '指标与目标对齐、转化'),
    (4, 1, 3, 75, '有行动闭环，但规则化程度不足。', '把结论写成选题/标题/分发规则，下一次实验验证。', '结论结构化、行动闭环');

INSERT INTO interview_memory_candidates (id, session_id, user_id, tag_id, content, proficiency, confidence, evidence, status) VALUES
    (1, 1, 1, 106, '模拟面试：能按用户画像拆解内容选题，并说明数据不达预期时的调整思路。', 3, 0.80, '先按用户画像拆选题，再用历史数据筛掉低效方向', 0),
    (2, 1, 1, 107, '模拟面试：能选择阅读完成率、互动率、涨粉与私域咨询等指标复盘内容效果。', 2, 0.72, '主要看阅读完成率、互动率、涨粉和私域咨询', 0);