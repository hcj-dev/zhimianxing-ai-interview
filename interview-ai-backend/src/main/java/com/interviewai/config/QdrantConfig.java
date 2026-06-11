package com.interviewai.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutionException;

/**
 * Qdrant 向量数据库配置。
 * 显式创建 collection（SpringAI 不会自动建），然后构建 VectorStore。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QdrantConfig {

    @Value("${app.qdrant.host:localhost}")
    private String host;

    @Value("${app.qdrant.port:6334}")
    private int port;

    @Value("${app.qdrant.api-key:}")
    private String apiKey;

    private final EmbeddingModel embeddingModel;

    // 先于 VectorStore Bean 初始化，确保 collection 存在
    private QdrantClient qdrantClient;

    @PostConstruct
    public void ensureCollections() {
        var builder = QdrantGrpcClient.newBuilder(host, port, false);
        if (apiKey != null && !apiKey.isBlank()) builder.withApiKey(apiKey);
        qdrantClient = new QdrantClient(builder.build());

        int dim = embeddingModel.dimensions();
        createIfNotExists("interview_questions", dim);
        createIfNotExists("resume_cases", dim);
    }

    private void createIfNotExists(String name, int dim) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(name).get();
            if (!exists) {
                qdrantClient.createCollectionAsync(name,
                        Collections.VectorParams.newBuilder()
                                .setSize(dim)
                                .setDistance(Collections.Distance.Cosine)
                                .build()).get();
                log.info("Qdrant collection 已创建: {} (维度={})", name, dim);
            } else {
                log.info("Qdrant collection 已存在: {}", name);
            }
        } catch (Exception e) {
            log.error("创建 Qdrant collection 失败: {} (Qdrant 可能未启动)", name, e);
        }
    }

    @Bean
    public QdrantClient qdrantClientBean() {
        return qdrantClient;
    }

    @Bean
    public QdrantVectorStore questionVectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("interview_questions")
                .build();
    }

    @Bean
    public QdrantVectorStore resumeVectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("resume_cases")
                .build();
    }
}
