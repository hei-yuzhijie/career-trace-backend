package com.yuzhijie.career.trace.integration.llm;

import com.yuzhijie.career.trace.integration.llm.model.LlmTag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DeepSeek Chat Completions 客户端：把职业记忆原文抽取为标签 JSON。
 *
 * <p>只负责“调用 + 解析 + 基础清洗”，归一化（tag_aliases）在 TagNormalizationService 中完成。</p>
 */
@Slf4j
@Component
public class DeepSeekClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final String systemPrompt;
    private final HttpClient httpClient;

    public DeepSeekClient(@Value("${career-trace.llm.api-key:}") String apiKey,
                          @Value("${career-trace.llm.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${career-trace.llm.model:deepseek-chat}") String model,
                          @Value("${career-trace.llm.timeout-seconds:90}") long timeoutSeconds,
                          @Value("${career-trace.llm.tag-prompt:classpath:prompts/memory-tagging.md}")
                          String tagPromptLocation) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl == null ? "https://api.deepseek.com" : baseUrl.trim();
        this.model = model == null ? "deepseek-chat" : model.trim();
        this.timeout = Duration.ofSeconds(timeoutSeconds <= 0 ? 90 : timeoutSeconds);
        this.systemPrompt = loadSystemPrompt(tagPromptLocation);
        // 部分地区到 api.deepseek.com 的 TLS 建连较慢，建连超时与整体超时分开设置
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(30, this.timeout.toSeconds())))
                .build();
    }

    public List<LlmTag> extractTags(String content) {
        if (apiKey.isEmpty()) {
            throw new LlmException("未配置 DEEPSEEK_API_KEY", false);
        }
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String requestBody = buildRequestBody(content);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stripTrailingSlash(baseUrl) + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new LlmException("DeepSeek 返回 HTTP " + response.statusCode() + ": "
                        + abbreviate(response.body(), 300), isRetryableStatus(response.statusCode()));
            }
            return parseTags(response.body());
        } catch (LlmException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("DeepSeek 调用被中断", e, false);
        } catch (Exception e) {
            throw new LlmException("DeepSeek 调用失败: " + e.getMessage(), e, true);
        }
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode >= 500;
    }

    /**
     * 通用结构化输出调用：返回大模型 message.content（通常是一个 JSON 对象字符串）。
     */
    public String completeJson(String prompt, String userMessage) {
        if (apiKey.isEmpty()) {
            throw new LlmException("未配置 DEEPSEEK_API_KEY", false);
        }
        if (prompt == null || prompt.isBlank()) {
            throw new LlmException("提示词为空", false);
        }
        try {
            String requestBody = buildRequestBody(prompt, userMessage, 0.2, 4000);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stripTrailingSlash(baseUrl) + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new LlmException("DeepSeek 返回 HTTP " + response.statusCode() + ": "
                        + abbreviate(response.body(), 300), isRetryableStatus(response.statusCode()));
            }
            return extractMessageContent(response.body());
        } catch (LlmException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("DeepSeek 调用被中断", e, false);
        } catch (Exception e) {
            throw new LlmException("DeepSeek 调用失败: " + e.getMessage(), e, true);
        }
    }

    private String buildRequestBody(String prompt, String userMessage, double temperature, int maxTokens) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);
        root.put("max_tokens", maxTokens);
        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", prompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userMessage == null ? "" : userMessage);

        return MAPPER.writeValueAsString(root);
    }

    private String extractMessageContent(String responseBody) {
        DeepSeekResponse response = MAPPER.readValue(responseBody, DeepSeekResponse.class);
        if (response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new LlmException("DeepSeek 返回内容为空");
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new LlmException("DeepSeek 未返回内容");
        }
        return content;
    }

    private String buildRequestBody(String content) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1);
        root.put("max_tokens", 900);
        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", content);

        return MAPPER.writeValueAsString(root);
    }

    private List<LlmTag> parseTags(String responseBody) {
        DeepSeekResponse response = MAPPER.readValue(responseBody, DeepSeekResponse.class);
        if (response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new LlmException("DeepSeek 返回内容为空");
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new LlmException("DeepSeek 未返回标签内容");
        }
        String json = extractJsonObject(content);

        JsonNode node = MAPPER.readTree(json);
        JsonNode tagsNode = node.path("tags");
        if (!tagsNode.isArray()) {
            throw new LlmException("DeepSeek 返回格式非法：缺少 tags 数组");
        }
        List<LlmTag> tags = new ArrayList<>();
        for (JsonNode tagNode : tagsNode) {
            LlmTag tag = MAPPER.treeToValue(tagNode, LlmTag.class);
            if (tag != null) {
                tags.add(tag);
                if (tags.size() >= 8) {
                    break;
                }
            }
        }
        log.info("DeepSeek 抽取完成，原始标签数={}", tags.size());
        return tags;
    }

    private static String extractJsonObject(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new LlmException("DeepSeek 返回内容不是 JSON 对象");
        }
        return text.substring(start, end + 1);
    }

    private static String loadSystemPrompt(String location) {
        String path = location == null || location.isBlank()
                ? "classpath:prompts/memory-tagging.md" : location.trim();
        Resource resource = new DefaultResourceLoader().getResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (prompt.isBlank()) {
                throw new IllegalStateException("标签抽取提示词为空: " + path);
            }
            log.info("已加载标签抽取提示词: {} ({} 字符)", path, prompt.length());
            return prompt;
        } catch (IOException e) {
            throw new IllegalStateException("无法加载标签抽取提示词: " + path, e);
        }
    }

    private static String stripTrailingSlash(String url) {
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    @Data
    public static class DeepSeekResponse {
        private List<Choice> choices;
    }

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String content;
    }
}
