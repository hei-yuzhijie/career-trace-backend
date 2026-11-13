package com.yuzhijie.career.trace.module.tag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuzhijie.career.trace.module.tag.entity.Tag;
import com.yuzhijie.career.trace.module.tag.entity.TagAlias;
import com.yuzhijie.career.trace.module.tag.mapper.TagAliasMapper;
import com.yuzhijie.career.trace.module.tag.mapper.TagMapper;
import com.yuzhijie.career.trace.module.tag.support.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 标签归一化：大模型输出的 name → tag_aliases / tags → tag_id。
 *
 * <p>命中顺序：alias 归一化文本 → alias_slug（拼音风格）→ tags.slug；
 * 仍未命中时创建 tags + tag_aliases（需求文档允许“或进人工审核队列”，
 * 第一版为闭环选择自动创建，并把主名同步写成 alias）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagNormalizationService {

    /** 语气词 / 否定词前缀：归一化时尝试剥离，命中已有标签就不新建同义词 */
    private static final Pattern NEGATION_PREFIX = Pattern.compile(
            "^(不考虑|不接受|不想要|不想做|不想去|不想|不要|不能接受|拒绝|排除|不喜欢|不倾向|不打算|不愿)");
    private static final Pattern PREFERENCE_PREFIX = Pattern.compile(
            "^(希望有|希望|想要有|想要|最好有|最好|倾向于|偏好|愿意|可以接受|能接受|接受|喜欢)");
    private static final Pattern ROLE_SUFFIX = Pattern.compile("(类)?(岗位|职位|工作|方向|行业|岗)$");

    private final TagMapper tagMapper;
    private final TagAliasMapper tagAliasMapper;

    public TagIndex loadIndex() {
        TagIndex index = new TagIndex();
        List<Tag> tags = tagMapper.selectList(null);
        for (Tag tag : tags) {
            index.addTag(tag);
        }
        List<TagAlias> aliases = tagAliasMapper.selectList(null);
        for (TagAlias alias : aliases) {
            index.addAlias(alias);
        }
        return index;
    }

    public Long resolveOrCreate(TagIndex index, String rawName, String rawCategory, Integer polarity) {
        String name = rawName == null ? "" : rawName.trim();
        String category = normalizeCategory(rawCategory);
        if (name.isEmpty() || category == null) {
            return null;
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        // 先按大模型原词精确匹配（保留种子别名，如“想留在杭州”）
        Long cached = findByAnyName(index, name);
        if (cached != null) {
            return cached;
        }

        // 再剥离“不考虑 / 希望 / 岗位”等语气词，尽量命中已有标签，避免同义词污染 tags
        String canonical = canonicalName(name, polarity);
        if (!canonical.isEmpty() && !canonical.equals(name)) {
            cached = findByAnyName(index, canonical);
            if (cached != null) {
                return cached;
            }
            name = canonical;
        }

        String baseSlug = SlugUtil.toSlug(name);
        String aliasKey = SlugUtil.normalizeText(name);
        synchronized (this) {
            cached = findByAnyName(index, name);
            if (cached != null) {
                return cached;
            }
            return createTag(index, name, category, baseSlug, aliasKey);
        }
    }

    private Long findByAnyName(TagIndex index, String name) {
        Long cached = index.findByAlias(SlugUtil.normalizeText(name));
        if (cached != null) {
            return cached;
        }
        return index.findBySlug(SlugUtil.toSlug(name));
    }

    /**
     * “不考虑销售岗” → 销售；“希望带教” → 带教。仅用于归一化查找与新建标签名，不改变原文证据。
     */
    static String canonicalName(String name, Integer polarity) {
        String value = name == null ? "" : name.trim();
        for (int i = 0; i < 2; i++) {
            String before = value;
            if (polarity != null && polarity == -1) {
                value = NEGATION_PREFIX.matcher(value).replaceFirst("");
            }
            value = PREFERENCE_PREFIX.matcher(value).replaceFirst("");
            value = ROLE_SUFFIX.matcher(value).replaceFirst("");
            if (value.equals(before)) {
                break;
            }
        }
        return value.trim();
    }

    private Long createTag(TagIndex index, String name, String category, String baseSlug, String aliasKey) {
        String slug = baseSlug;
        int suffix = 2;
        while (index.findBySlug(slug) != null || slugExistsInDb(slug)) {
            String candidate = baseSlug + "_" + suffix;
            slug = candidate.length() > 100 ? candidate.substring(0, 100) : candidate;
            suffix++;
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tag.setCategory(category);
        tag.setStatus(1);
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            // 并发创建同一 slug：以先落库的为准
            Tag existing = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery()
                    .eq(Tag::getSlug, slug).last("LIMIT 1"));
            if (existing != null) {
                index.addTag(existing);
                return existing.getId();
            }
            throw e;
        }

        TagAlias alias = new TagAlias();
        alias.setTagId(tag.getId());
        alias.setAlias(name);
        alias.setAliasSlug(slug);
        try {
            tagAliasMapper.insert(alias);
        } catch (DuplicateKeyException e) {
            log.warn("alias 已存在，跳过写入: aliasSlug={}", slug);
        }

        index.addTag(tag);
        index.addAlias(aliasKey, slug, tag.getId());
        log.info("新建标签: id={}, name={}, slug={}, category={}", tag.getId(), name, slug, category);
        return tag.getId();
    }

    private boolean slugExistsInDb(String slug) {
        return tagMapper.selectCount(Wrappers.<Tag>lambdaQuery().eq(Tag::getSlug, slug)) > 0;
    }

    /**
     * 大模型类别 → 数据库类别；非法类别返回 null（该标签丢弃，避免污染字典）。
     */
    public static String normalizeCategory(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "skill", "技能", "能力" -> "skill";
            case "experience", "经历", "经验" -> "experience";
            case "preference", "偏好", "意向" -> "preference";
            case "location", "地点", "城市" -> "location";
            case "seniority", "资历", "学历", "年限" -> "seniority";
            case "gap", "差距" -> "gap";
            default -> null;
        };
    }

    /**
     * 一次打标任务内复用的标签字典索引，避免逐条查库。
     */
    public static class TagIndex {

        private final Map<String, Long> byAliasText = new HashMap<>();
        private final Map<String, Long> bySlug = new HashMap<>();

        private void addTag(Tag tag) {
            bySlug.put(tag.getSlug(), tag.getId());
        }

        private void addAlias(TagAlias alias) {
            addAlias(SlugUtil.normalizeText(alias.getAlias()), alias.getAliasSlug(), alias.getTagId());
        }

        private void addAlias(String aliasKey, String aliasSlug, Long tagId) {
            if (aliasKey != null && !aliasKey.isEmpty()) {
                byAliasText.putIfAbsent(aliasKey, tagId);
            }
            if (aliasSlug != null && !aliasSlug.isEmpty()) {
                bySlug.putIfAbsent(aliasSlug, tagId);
            }
        }

        private Long findByAlias(String aliasKey) {
            return aliasKey == null ? null : byAliasText.get(aliasKey);
        }

        private Long findBySlug(String slug) {
            return slug == null ? null : bySlug.get(slug);
        }
    }
}
