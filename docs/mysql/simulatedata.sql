-- =====================================================
-- 模拟数据
-- =====================================================

-- ---------- users ----------
INSERT INTO users (id, nickname) VALUES
                                     (1, '陈默'),
                                     (2, '林一');

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
                                                (117, '短视频脚本', 'duan_shi_pin_jiao_ben', 'skill');
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
                                                        (113, '3-5年', '3_5_nian');

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
                                                                                                                  '负责公众号周更与用户增长；需要内容排期与数据复盘能力。', 1);

-- ---------- memory_items ----------
INSERT INTO memory_items (id, user_id, content, source_type, source_id, status) VALUES
                                                                                    (1, 1, '想留在杭州，能接受大小周，希望有带教', 'manual', NULL, 1),
                                                                                    (2, 1, '不考虑销售、地推类岗位', 'manual', NULL, 1),
                                                                                    (3, 1, '9 月做了 3 篇小红书笔记，其中 1 篇阅读 4.2 万，带来 300+ 私域咨询', 'resume', NULL, 1),
                                                                                    (4, 1, '暑期运营实习 3 个月，独立负责公众号周更，粉丝从 1.2k 到 3.6k', 'resume', NULL, 1),
                                                                                    (5, 1, '每周复盘完读率与涨粉曲线，用数据指导选题', 'resume', NULL, 1),
                                                                                    (6, 2, '希望做后端开发，最好在杭州', 'manual', NULL, 1);

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
                                                                                                          (6, 101, 1, 0, 0.96, '最好在杭州', 'llm');

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

-- ---------- user_skill_profile（仅 skill 类） ----------
INSERT INTO user_skill_profile (user_id, tag_id, level, confidence, memory_count, best_memory_id) VALUES
                                                                                                      (1, 106, 3, 0.92, 1, 3),
                                                                                                      (1, 107, 3, 0.90, 1, 5),
                                                                                                      (1, 109, 2, 0.86, 1, 4),
                                                                                                      (1, 110, 2, 0.84, 1, 4),
                                                                                                      (1, 115, 2, 0.85, 1, 3);