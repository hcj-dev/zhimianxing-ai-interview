package com.interviewai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 阿里百炼 Embedding（DashScope 原生 API）。
 * compatible-mode 端点不支持 embedding，用 RestTemplate 直连原生 API，
 * 包装成 SpringAI EmbeddingModel 接口供 QdrantVectorStore 使用。
 */
@Configuration
public class EmbeddingConfig {

    @Value("${spring.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.dashscope.embedding.options.model:text-embedding-v2}")
    private String model;

    @Bean
    public EmbeddingModel embeddingModel() {
        RestTemplate rt = new RestTemplate();

        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                for (String text : request.getInstructions()) {
                    float[] vec = embed(text);
                    embeddings.add(new Embedding(vec, 0));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getText());
            }

            @Override
            public float[] embed(String text) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(apiKey);

                    Map<String, Object> body = Map.of(
                            "model", model,
                            "input", Map.of("texts", List.of(text)),
                            "parameters", Map.of("text_type", "document"));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> resp = rt.postForObject(
                            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding",
                            new HttpEntity<>(body, headers), Map.class);

                    if (resp != null && resp.get("output") != null) {
                        @SuppressWarnings("unchecked")
                        var output = (Map<String, Object>) resp.get("output");
                        // DashScope 返回格式: {"embeddings": [{"embedding": [...], "text_index": 0}]}
                        @SuppressWarnings("unchecked")
                        var embList = (List<Map<String, Object>>) output.get("embeddings");
                        if (embList != null && !embList.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            List<Double> v = (List<Double>) embList.get(0).get("embedding");
                            if (v != null) {
                                float[] r = new float[v.size()];
                                for (int i = 0; i < v.size(); i++) r[i] = v.get(i).floatValue();
                                return r;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
                }
                return new float[dimensions()];
            }

            @Override
            public int dimensions() {
                return model.contains("v4") || model.contains("v3") ? 1024 : 1536;
            }
        };
    }
}
