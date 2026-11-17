package com.yuzhijie.career.trace.integration.llm;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 classpath 加载提示词，进程内缓存。
 */
@Component
public class PromptLoader {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String load(String location) {
        String key = location == null || location.isBlank()
                ? "classpath:prompts/memory-tagging.md" : location.trim();
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Resource resource = new DefaultResourceLoader().getResource(key);
        try (InputStream inputStream = resource.getInputStream()) {
            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (prompt.isBlank()) {
                throw new IllegalStateException("提示词为空: " + key);
            }
            cache.put(key, prompt);
            return prompt;
        } catch (IOException e) {
            throw new IllegalStateException("无法加载提示词: " + key, e);
        }
    }
}
