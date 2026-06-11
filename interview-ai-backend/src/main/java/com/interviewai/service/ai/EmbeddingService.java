package com.interviewai.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 阿里百炼 Embedding 向量化服务。
 * DashScope OpenAI 兼容端点不支持 embedding，改用原生 API + RestTemplate 直连。
 */
@Slf4j
@Service
public class EmbeddingService {

    private static final String URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public EmbeddingService(
            @Value("${spring.dashscope.api-key}") String apiKey,
            @Value("${spring.dashscope.embedding.options.model:text-embedding-v2}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    public float[] embed(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", Map.of("texts", List.of(text)),
                    "parameters", Map.of("text_type", "document"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(URL, request, Map.class);

            if (response == null || response.get("output") == null) {
                log.error("Embedding 响应为空");
                return new float[1024];
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            @SuppressWarnings("unchecked")
            List<List<Double>> embeddings = (List<List<Double>>) output.get("embeddings");

            if (embeddings == null || embeddings.isEmpty()) {
                log.error("Embedding 结果为空");
                return new float[1024];
            }

            List<Double> vec = embeddings.get(0);
            float[] result = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) result[i] = vec.get(i).floatValue();
            return result;

        } catch (Exception e) {
            log.error("Embedding 调用失败", e);
            return new float[1024];
        }
    }

    public int dimension() {
        return 1024;
    }
}
