package com.interviewai.service;

import com.interviewai.entity.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.List;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.stereotype.Service;

/**
 * 向量检索服务。封装 Qdrant 的语义搜索逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final QdrantVectorStore questionVectorStore;
    private final QdrantVectorStore resumeVectorStore;

    /**
     * 根据查询文本语义搜索最相关的面试题，返回 Qdrant 文档 ID 列表和相似度分数。
     */
    public List<Map<String, Object>> searchQuestions(String query, int topK) {
        var results = questionVectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "id", doc.getId(),
                        "content", doc.getText(),
                        "score", doc.getScore()))
                .toList();
    }

    /**
     * 搜索相似简历案例。
     */
    public List<Map<String, Object>> searchResumes(String query, int topK) {
        var results = resumeVectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "id", doc.getId(),
                        "content", doc.getText(),
                        "score", doc.getScore()))
                .toList();
    }

    /**
     * 将题目存入 Qdrant 面试题集合。
     */
    public void storeQuestion(Long questionId, String title, String content) {
        String text = title + "\n" + (content != null ? content : "");
        questionVectorStore.add(List.of(
                new org.springframework.ai.document.Document(text, Map.of("questionId", questionId.intValue()))));
    }

    /**
     * 批量向量化题目，N道题一次add调用，减少gRPC连接压力。
     */
    public void storeQuestionsBatch(List<com.interviewai.entity.Question> questions) {
        List<org.springframework.ai.document.Document> docs = questions.stream()
                .map(q -> {
                    String text = q.getTitle() + "\n" + (q.getContent() != null ? q.getContent() : "");
                    return new org.springframework.ai.document.Document(text,
                            Map.of("questionId", q.getId().intValue()));
                }).toList();
        questionVectorStore.add(docs);
    }

    public void storeResume(Long resumeId, String text) {
        resumeVectorStore.add(List.of(
                new org.springframework.ai.document.Document(text, Map.of("resumeId", resumeId.intValue()))));
    }
}
