package com.interviewai.config;

import com.interviewai.entity.Question;
import com.interviewai.mapper.QuestionMapper;
import com.interviewai.service.VectorSearchService;
import io.qdrant.client.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 应用启动时，将数据库中所有题目批量向量化存入 Qdrant。
 * 仅当 Qdrant 集合为空时才执行（避免重复写入），
 * 新增/修改题目由 QuestionService 实时同步。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionVectorInit implements ApplicationRunner {

    private final QuestionMapper questionMapper;
    private final VectorSearchService vectorSearchService;
    private final QdrantClient qdrantClient;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等 Qdrant gRPC 连接完全就绪
            } catch (InterruptedException ignored) { }

            try {
                // 检查集合是否已有数据，避免每次重启重复写入
                long existingCount = qdrantClient.countAsync("interview_questions").get();
                List<Question> questions = questionMapper.selectList(null);
                long dbCount = questions.size();

                if (existingCount >= dbCount) {
                    log.info("Qdrant 已有 {} 条题目向量 (DB={})，跳过初始化", existingCount, dbCount);
                    return;
                }

                log.info("Qdrant 集合中仅有 {} 条，DB 有 {} 条，开始增量向量化", existingCount, dbCount);

                // 每10道一批，批次间暂停1秒
                List<Question> batch = new ArrayList<>();
                for (int i = 0; i < questions.size(); i++) {
                    batch.add(questions.get(i));
                    if (batch.size() >= 10 || i == questions.size() - 1) {
                        vectorSearchService.storeQuestionsBatch(batch);
                        log.info("向量化进度: {}/{}", i + 1, questions.size());
                        batch.clear();
                        Thread.sleep(1000);
                    }
                }
                log.info("题目向量化完成，总计 {} 道", questions.size());
            } catch (ExecutionException e) {
                log.warn("Qdrant count 查询失败（可能集合尚不存在），将全量写入: {}", e.getMessage());
                try {
                    List<Question> questions = questionMapper.selectList(null);
                    List<Question> batch = new ArrayList<>();
                    for (int i = 0; i < questions.size(); i++) {
                        batch.add(questions.get(i));
                        if (batch.size() >= 10 || i == questions.size() - 1) {
                            vectorSearchService.storeQuestionsBatch(batch);
                            batch.clear();
                            Thread.sleep(1000);
                        }
                    }
                    log.info("题目向量化完成，总计 {} 道", questions.size());
                } catch (Exception ex) {
                    log.error("题目向量化失败（Qdrant未就绪）", ex);
                }
            } catch (Exception e) {
                log.error("题目向量化失败（Qdrant未就绪）", e);
            }
        }, "qdrant-init").start();
    }
}
