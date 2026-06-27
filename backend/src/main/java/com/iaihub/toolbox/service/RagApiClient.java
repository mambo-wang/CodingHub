package com.iaihub.toolbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
     * POST /api/collections/{name}/documents — 上传文档（multipart 转发）
     * 返回 {status, filename, chunks}
     */
    public Map<String, Object> uploadDocument(String collection, MultipartFile file,
                                               Integer chunkSize, String chunkMode) {
        try {
            String encodedName = URLEncoder.encode(collection, StandardCharsets.UTF_8);
            String url = baseUrl + "/api/collections/" + encodedName + "/documents";

            // Build query params
            List<String> params = new ArrayList<>();
            if (chunkSize != null) params.add("chunk_size=" + chunkSize);
            if (chunkMode != null) params.add("chunk_mode=" + URLEncoder.encode(chunkMode, StandardCharsets.UTF_8));
            if (!params.isEmpty()) url += "?" + String.join("&", params);

            // Build multipart body manually
            String boundary = UUID.randomUUID().toString();
            byte[] bodyBytes = buildMultipartBody(boundary, file);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .timeout(Duration.ofSeconds(300))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("RAG uploadDocument failed: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("RAG 文档上传失败: " + response.body());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG uploadDocument error: {}", e.getMessage());
            throw new RuntimeException("RAG 服务不可用: " + e.getMessage(), e);
        }
    }

    /**
     * DELETE /api/collections/{name}/documents — 删除文档
     */
    public void deleteDocument(String collection, String filepath) {
        try {
            String encodedName = URLEncoder.encode(collection, StandardCharsets.UTF_8);
            String body = objectMapper.writeValueAsString(Map.of("filepath", filepath));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName + "/documents"))
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // HttpClient DELETE doesn't support body natively, use workaround
            HttpRequest deleteWithBody = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/collections/" + encodedName + "/documents"))
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(deleteWithBody, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("RAG deleteDocument failed: {} - {} (tolerated)", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("RAG deleteDocument error (tolerated): {}", e.getMessage());
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

    /**
     * Build multipart/form-data body manually (no external dependency needed)
     */
    private byte[] buildMultipartBody(String boundary, MultipartFile file) throws Exception {
        String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();

        // File part
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
          .append(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed")
          .append("\"").append(CRLF);
        sb.append("Content-Type: ").append(file.getContentType() != null ? file.getContentType() : "application/octet-stream").append(CRLF);
        sb.append(CRLF);

        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fileData = file.getBytes();
        byte[] footer = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[header.length + fileData.length + footer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(fileData, 0, result, header.length, fileData.length);
        System.arraycopy(footer, 0, result, header.length + fileData.length, footer.length);

        return result;
    }
}
