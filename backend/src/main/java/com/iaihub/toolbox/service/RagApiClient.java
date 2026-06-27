package com.iaihub.toolbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class RagApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public RagApiClient(HttpClient httpClient,
                        @Value("${app.rag.base-url}") String baseUrl,
                        ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * PUT /api/collections/{name}/config — 初始化或更新 collection 配置
     */
    public Map<String, Object> configureCollection(String name, Map<String, Object> config) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String body = objectMapper.writeValueAsString(config);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName + "/config"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("RAG configureCollection failed: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("RAG 配置更新失败: " + response.body());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG configureCollection error: {}", e.getMessage());
            throw new RuntimeException("RAG 服务不可用: " + e.getMessage(), e);
        }
    }

    /**
     * GET /api/collections/{name}/config — 获取 collection 配置
     */
    public Map<String, Object> getCollectionConfig(String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName + "/config"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("RAG getCollectionConfig failed: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("RAG 获取配置失败: " + response.body());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG getCollectionConfig error: {}", e.getMessage());
            throw new RuntimeException("RAG 服务不可用: " + e.getMessage(), e);
        }
    }

    /**
     * DELETE /api/collections/{name} — 删除整个 collection
     */
    public void deleteCollection(String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName))
                    .DELETE()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("RAG deleteCollection failed: {} - {} (tolerated)", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("RAG deleteCollection error (tolerated): {}", e.getMessage());
        }
    }

    /**
     * POST /api/collections/{name}/search — 语义搜索
     */
    public List<Map<String, Object>> search(String collection, String query,
                                             int topK, Boolean rerank, int expandContext) {
        try {
            String encodedName = URLEncoder.encode(collection, StandardCharsets.UTF_8);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", query);
            body.put("top_k", topK);
            if (rerank != null) body.put("rerank", rerank);
            if (expandContext > 0) body.put("expand_context", expandContext);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName + "/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("RAG search failed: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("RAG 搜索失败: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            List<Map<String, Object>> results = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("text", item.has("text") ? item.get("text").asText() : "");
                    result.put("source", item.has("source") ? item.get("source").asText() : "");
                    result.put("score", item.has("score") ? item.get("score").asDouble() : 0.0);
                    result.put("chunkIndex", item.has("chunk_index") ? item.get("chunk_index").asInt() : 0);
                    results.add(result);
                }
            }
            return results;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG search error: {}", e.getMessage());
            throw new RuntimeException("RAG 服务不可用: " + e.getMessage(), e);
        }
    }
}
