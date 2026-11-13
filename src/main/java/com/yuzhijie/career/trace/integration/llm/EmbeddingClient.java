package com.yuzhijie.career.trace.integration.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本向量化客户端，兼容阿里云百炼 OpenAI 兼容接口：
 * {@code POST {host}/compatible-mode/v1/embeddings}。
 *
 * <p>.env 里的 EMBEDDING_BASE_URL 可能是 {@code .../api/v1}，
 * 客户端会自动归一化为 {@code .../compatible-mode/v1/embeddings}。</p>
 */
@Slf4j
@Component
public class EmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_TEXT_LENGTH = 6000;

    private final boolean enabled;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final int batchSize;
    private final int dimensions;
    private final Duration timeout;
    private final HttpClient httpClient;

    public EmbeddingClient(@Value("${career-trace.embedding.enabled:true}") boolean enabled,
                           @Value("${career-trace.embedding.api-key:}") String apiKey,
                           @Value("${career-trace.embedding.base-url:}") String baseUrl,
                           @Value("${career-trace.embedding.endpoint:}") String endpoint,
                           @Value("${career-trace.embedding.model:text-embedding-v3}") String model,
                           @Value("${career-trace.embedding.timeout-seconds:30}") long timeoutSeconds,
                           @Value("${career-trace.embedding.batch-size:10}") int batchSize,
                           @Value("${career-trace.embedding.dimensions:1024}") int dimensions) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "text-embedding-v3" : model.trim();
        this.batchSize = Math.max(1, Math.min(10, batchSize));
        this.dimensions = dimensions;
        this.timeout = Duration.ofSeconds(timeoutSeconds <= 0 ? 30 : timeoutSeconds);
        this.endpoint = resolveEndpoint(baseUrl, endpoint);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(30, this.timeout.toSeconds())))
                .build();

        if (isAvailable()) {
            log.info("Embedding 已启用: model={}, endpoint={}", this.model, this.endpoint);
        } else {
            log.warn("Embedding 未启用或配置不完整，推荐将只使用结构化分");
        }
    }

    public boolean isAvailable() {
        return enabled && !apiKey.isEmpty() && !endpoint.isEmpty();
    }

    public String getModel() {
        return model;
    }

    /**
     * 批量向量化，返回顺序与入参一致。
     */
    public List<float[]> embed(List<String> texts) {
        if (!isAvailable()) {
            throw new EmbeddingException("Embedding 未启用或未配置 API Key");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> result = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            result.addAll(embedBatch(texts.subList(start, end)));
        }
        return result;
    }

    private List<float[]> embedBatch(List<String> texts) {
        try {
            String requestBody = buildRequestBody(texts);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new EmbeddingException("Embedding HTTP " + response.statusCode() + ": "
                        + abbreviate(response.body(), 300));
            }
            return parse(response.body(), texts.size());
        } catch (EmbeddingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Embedding 调用被中断", e);
        } catch (Exception e) {
            throw new EmbeddingException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(List<String> texts) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        if (dimensions > 0) {
            root.put("dimensions", dimensions);
        }
        root.put("encoding_format", "float");
        ArrayNode input = root.putArray("input");
        for (String text : texts) {
            input.add(truncate(text));
        }
        return MAPPER.writeValueAsString(root);
    }

    private List<float[]> parse(String responseBody, int expectedSize) {
        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.size() != expectedSize) {
            throw new EmbeddingException("Embedding 返回数量不匹配，期望 " + expectedSize
                    + "，实际 " + data.size());
        }

        List<float[]> vectors = new ArrayList<>(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            vectors.add(null);
        }
        int fallbackIndex = 0;
        for (JsonNode item : data) {
            int index = item.has("index") ? item.path("index").asInt() : fallbackIndex;
            fallbackIndex++;
            JsonNode embedding = item.path("embedding");
            if (index < 0 || index >= expectedSize || !embedding.isArray()) {
                throw new EmbeddingException("Embedding 返回结构非法");
            }
            float[] vector = new float[embedding.size()];
            for (int j = 0; j < vector.length; j++) {
                vector[j] = (float) embedding.get(j).asDouble();
            }
            vectors.set(index, vector);
        }
        for (float[] vector : vectors) {
            if (vector == null) {
                throw new EmbeddingException("Embedding 返回缺少向量");
            }
        }
        return vectors;
    }

    private static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "无";
        }
        String value = text.trim();
        return value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }

    private static String resolveEndpoint(String baseUrl, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.trim();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String root = baseUrl.trim();
        while (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        if (root.endsWith("/api/v1")) {
            root = root.substring(0, root.length() - "/api/v1".length());
        } else if (root.endsWith("/compatible-mode/v1")) {
            root = root.substring(0, root.length() - "/compatible-mode/v1".length());
        }
        return root + "/compatible-mode/v1/embeddings";
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
