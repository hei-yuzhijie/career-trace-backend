package com.yuzhijie.career.trace.module.interview.service;

import com.yuzhijie.career.trace.module.tag.entity.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按技能方向加载技术岗参考题库，拼接到出题 prompt 的 {referenceSection}。
 * 参考题库只用于校准考点，不直接作为标准答案。
 */
@Slf4j
@Service
public class InterviewReferenceService {

    private static final int MAX_TOTAL_CHARS = 14_000;

    private static final Map<String, String> SLUG_TO_RESOURCE = Map.ofEntries(
            Map.entry("java", "skills/java-backend/references/java.md"),
            Map.entry("mysql", "skills/java-backend/references/mysql.md"),
            Map.entry("redis", "skills/java-backend/references/redis.md"),
            Map.entry("spring", "skills/java-backend/references/spring.md"),
            Map.entry("ai_agent", "skills/ai-agent-dev/references/agent.md"),
            Map.entry("agent", "skills/ai-agent-dev/references/agent.md"),
            Map.entry("system_design", "skills/system-design/references/system-design.md"),
            Map.entry("algorithm", "skills/algorithm/references/algorithm.md"),
            Map.entry("frontend", "skills/frontend/references/frontend.md")
    );

    private static final Map<String, String> NAME_TO_KEY = Map.ofEntries(
            Map.entry("Java", "java"),
            Map.entry("MySQL", "mysql"),
            Map.entry("Redis", "redis"),
            Map.entry("Spring", "spring"),
            Map.entry("AI Agent", "ai_agent"),
            Map.entry("Agent", "agent"),
            Map.entry("系统设计", "system_design"),
            Map.entry("算法", "algorithm"),
            Map.entry("算法与数据结构", "algorithm"),
            Map.entry("前端", "frontend"),
            Map.entry("前端工程", "frontend")
    );

    public String load(List<Tag> directions) {
        if (directions == null || directions.isEmpty()) {
            return "无参考题库";
        }
        StringBuilder builder = new StringBuilder();
        Set<String> loaded = new LinkedHashSet<>();
        for (Tag tag : directions) {
            String key = key(tag);
            if (key == null) {
                continue;
            }
            String resource = SLUG_TO_RESOURCE.get(key);
            if (resource == null || !loaded.add(resource)) {
                continue;
            }
            String content = read(resource);
            if (content == null || content.isBlank()) {
                continue;
            }
            if (builder.length() + content.length() > MAX_TOTAL_CHARS) {
                int remain = MAX_TOTAL_CHARS - builder.length();
                if (remain > 500) {
                    builder.append("\n## 参考题库：").append(tag.getName()).append("\n")
                            .append(content, 0, Math.min(content.length(), remain - 100))
                            .append("\n（题库已截断）\n");
                }
                break;
            }
            builder.append("\n## 参考题库：").append(tag.getName()).append("\n")
                    .append(content).append("\n");
        }
        return builder.isEmpty() ? "无参考题库" : builder.toString();
    }

    private static String key(Tag tag) {
        if (tag == null) {
            return null;
        }
        String slug = tag.getSlug();
        if (slug != null) {
            String normalized = slug.trim().toLowerCase()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .replace('.', '_');
            if (SLUG_TO_RESOURCE.containsKey(normalized)) {
                return normalized;
            }
        }
        return NAME_TO_KEY.get(tag.getName());
    }

    private static String read(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream input = resource.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("加载技术题库失败 {}: {}", path, e.getMessage());
            return null;
        }
    }
}
