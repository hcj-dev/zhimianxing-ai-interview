package com.interviewai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.RabbitMQConfig;
import com.interviewai.entity.InterviewMessage;
import com.interviewai.entity.InterviewSession;
import com.interviewai.entity.WrongQuestion;
import com.interviewai.mapper.InterviewMessageMapper;
import com.interviewai.mapper.InterviewSessionMapper;
import com.interviewai.mapper.WrongQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 面试报告生成队列消费者。
 * 汇总所有问答 → DeepSeek评分+总评+雷达图 → 写入interview_session表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReportConsumer {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_REPORT_QUEUE)
    public void process(Map<String, Object> message) {
        Long sessionId = Long.valueOf(message.get("sessionId").toString());
        log.info("开始生成面试报告, sessionId={}", sessionId);

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            log.error("面试场次不存在, sessionId={}", sessionId);
            return;
        }

        try {
            List<InterviewMessage> messages = messageMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewMessage>()
                            .eq(InterviewMessage::getSessionId, sessionId)
                            .orderByAsc(InterviewMessage::getSequence));

            StringBuilder qaText = new StringBuilder();
            for (InterviewMessage msg : messages) {
                qaText.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
            }

            String prompt = """
                    你是一位资深面试官。请根据以下面试对话，生成面试评估报告。

                    对话记录：
                    %s

                    请逐题评分（0-10分），综合评分使用百分制（0-100分，60分为及格线），识别薄弱点，并生成雷达图六维度分数（每维度0-100分）：
                    - javaBasic: Java基础
                    - framework: 框架应用
                    - database: 数据库
                    - middleware: 中间件
                    - project: 项目能力
                    - comprehensive: 综合素质

                    仅返回JSON，不要加任何文字：
                    {
                        "totalScore": 数字(0-100百分制，如75.0表示75分),
                        "qaScores": [{"sequence": 题号, "question": "题目摘要", "score": 分数(0-10), "feedback": "反馈"}],
                        "strengths": ["优势1", "优势2"],
                        "weaknesses": [{"knowledgePoint": "薄弱点", "frequency": 出现次数}],
                        "radarData": {"javaBasic": 分(0-100), "framework": 分(0-100), "database": 分(0-100), "middleware": 分(0-100), "project": 分(0-100), "comprehensive": 分(0-100)}
                    }
                    """.formatted(qaText.toString());

            String response = chatClient.prompt().user(prompt).call().content();
            String json = response.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);

            @SuppressWarnings("unchecked")
            Map<String, Object> report = objectMapper.readValue(json.trim(), Map.class);

            // 写入报告数据
            session.setTotalScore(Double.valueOf(report.get("totalScore").toString()));
            session.setStrengths(objectMapper.writeValueAsString(report.get("strengths")));
            session.setWeaknesses(objectMapper.writeValueAsString(report.get("weaknesses")));
            session.setRadarData(objectMapper.writeValueAsString(report.get("radarData")));
            sessionMapper.updateById(session);

            // 更新消息评分
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> qaScores = (List<Map<String, Object>>) report.get("qaScores");
            if (qaScores != null) {
                for (Map<String, Object> qa : qaScores) {
                    int seq = Integer.valueOf(qa.get("sequence").toString());
                    int score = Integer.valueOf(qa.get("score").toString());
                    String feedback = qa.get("feedback").toString();
                    // 找到对应sequence的USER消息并更新评分
                    List<InterviewMessage> userMsgs = messageMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewMessage>()
                                    .eq(InterviewMessage::getSessionId, sessionId)
                                    .eq(InterviewMessage::getSequence, seq)
                                    .eq(InterviewMessage::getRole, "USER"));
                    if (!userMsgs.isEmpty()) {
                        InterviewMessage um = userMsgs.get(0);
                        um.setScore(score);
                        um.setFeedback(feedback);
                        messageMapper.updateById(um);

                        // 得分<6 自动收录到错题本
                        if (score < 6) {
                            // 找对应的AI题目
                            List<InterviewMessage> aiMsgs = messageMapper.selectList(
                                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewMessage>()
                                            .eq(InterviewMessage::getSessionId, sessionId)
                                            .eq(InterviewMessage::getSequence, seq)
                                            .eq(InterviewMessage::getRole, "AI"));
                            String questionText = aiMsgs.isEmpty() ? "" : aiMsgs.get(0).getContent();
                            // 提取知识点
                            String knowledgePoint = qa.get("question").toString();
                            if (knowledgePoint.length() > 200) knowledgePoint = knowledgePoint.substring(0, 200);

                            WrongQuestion wq = new WrongQuestion();
                            wq.setUserId(session.getUserId());
                            wq.setSessionId(sessionId);
                            wq.setKnowledgePoint(knowledgePoint);
                            wq.setQuestionText(questionText);
                            wq.setUserAnswer(um.getContent());
                            wq.setScore(score);
                            wq.setFeedback(feedback);
                            wrongQuestionMapper.insert(wq);
                        }
                    }
                }
            }

            log.info("面试报告生成完成, sessionId={}, totalScore={}", sessionId, session.getTotalScore());

        } catch (Exception e) {
            log.error("面试报告生成失败, sessionId={}", sessionId, e);
        }
    }
}
