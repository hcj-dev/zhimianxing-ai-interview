package com.interviewai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.RabbitMQConfig;
import com.interviewai.entity.Resume;
import com.interviewai.entity.ResumeAnalysis;
import com.interviewai.mapper.ResumeAnalysisMapper;
import com.interviewai.mapper.ResumeMapper;
import com.interviewai.service.PdfService;
import com.interviewai.service.VectorSearchService;
import com.interviewai.service.ai.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 简历分析队列消费者。
 * 流程：解析PDF → AI提取结构化信息 → AI生成分析报告 → 写入数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisConsumer {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final PdfService pdfService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final VectorSearchService vectorSearchService;

    @RabbitListener(queues = RabbitMQConfig.RESUME_ANALYSIS_QUEUE)
    public void process(Map<String, Object> message) {
        Long resumeId = Long.valueOf(message.get("resumeId").toString());
        String filePath = message.get("filePath").toString();

        log.info("开始处理简历分析, resumeId={}", resumeId);
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            log.error("简历不存在, resumeId={}", resumeId);
            return;
        }

        try {
            // 1. 解析PDF文本
            String rawText = pdfService.extractText(new File(filePath));
            resume.setRawText(rawText);
            resumeMapper.updateById(resume);
            log.info("PDF解析完成, resumeId={}, 文本长度={}", resumeId, rawText.length());

            // 2. AI提取结构化信息
            Map<String, Object> structuredInfo = chatService.extractResumeInfo(rawText);
            String structuredJson = objectMapper.writeValueAsString(structuredInfo);
            log.info("结构化提取完成, resumeId={}", resumeId);

            // 3. AI生成分析报告
            Map<String, Object> analysis = chatService.analyzeResume(rawText, structuredJson);

            // 4. 写入分析报告表
            ResumeAnalysis ra = new ResumeAnalysis();
            ra.setResumeId(resumeId);
            ra.setSkillScore((Integer) analysis.get("skillScore"));
            ra.setDescriptionQuality((Integer) analysis.get("descriptionQuality"));
            ra.setKeywordCoverage((Integer) analysis.get("keywordCoverage"));
            ra.setFormatScore((Integer) analysis.get("formatScore"));
            ra.setOverallScore((Integer) analysis.get("overallScore"));
            ra.setStrengths(objectMapper.writeValueAsString(analysis.get("strengths")));
            ra.setWeaknesses(objectMapper.writeValueAsString(analysis.get("weaknesses")));
            ra.setSuggestions(objectMapper.writeValueAsString(analysis.get("suggestions")));
            ra.setOptimizedResume((String) analysis.get("optimizedResume"));

            // 查是否已有分析记录，有则更新，无则插入
            ResumeAnalysis existing = analysisMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                            .eq(ResumeAnalysis::getResumeId, resumeId));
            if (existing != null) {
                ra.setId(existing.getId());
                analysisMapper.updateById(ra);
            } else {
                analysisMapper.insert(ra);
            }

            // 5. 更新简历状态
            resume.setStructuredData(structuredJson);
            resume.setStatus("COMPLETED");
            resumeMapper.updateById(resume);

            // 6. 将简历向量化存入 Qdrant（用于 JD 匹配和案例检索）
            try {
                vectorSearchService.storeResume(resumeId, rawText);
                log.info("简历向量已存入Qdrant, resumeId={}", resumeId);
            } catch (Exception ve) {
                log.warn("简历向量化失败（Qdrant可能未启动）, resumeId={}", resumeId, ve);
            }

            log.info("简历分析完成, resumeId={}, overallScore={}", resumeId, ra.getOverallScore());

        } catch (Exception e) {
            log.error("简历分析失败, resumeId={}", resumeId, e);
            resume.setStatus("FAILED");
            resumeMapper.updateById(resume);
        }
    }
}
