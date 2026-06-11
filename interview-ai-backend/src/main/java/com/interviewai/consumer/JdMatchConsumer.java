package com.interviewai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.RabbitMQConfig;
import com.interviewai.entity.JobDescription;
import com.interviewai.entity.Resume;
import com.interviewai.mapper.JobDescriptionMapper;
import com.interviewai.mapper.ResumeMapper;
import com.interviewai.service.ai.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JD匹配分析队列消费者。
 * 流程：读取JD和简历文本 → AI对比分析 → 写入job_description表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdMatchConsumer {

    private final ResumeMapper resumeMapper;
    private final JobDescriptionMapper jdMapper;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.JD_MATCH_QUEUE)
    public void process(Map<String, Object> message) {
        Long jdId = Long.valueOf(message.get("jdId").toString());
        Long resumeId = Long.valueOf(message.get("resumeId").toString());

        log.info("开始JD匹配分析, jdId={}, resumeId={}", jdId, resumeId);

        JobDescription jd = jdMapper.selectById(jdId);
        Resume resume = resumeMapper.selectById(resumeId);
        if (jd == null || resume == null) {
            log.error("JD或简历不存在, jdId={}, resumeId={}", jdId, resumeId);
            return;
        }

        try {
            String resumeText = resume.getRawText();
            if (resumeText == null) resumeText = "";

            Map<String, Object> result = chatService.matchJD(resumeText, jd.getJdText());

            jd.setMatchScore((Integer) result.get("matchScore"));
            jd.setSkillGap(objectMapper.writeValueAsString(result.get("skillGap")));
            jd.setSuggestions(objectMapper.writeValueAsString(result.get("suggestions")));
            jd.setStatus("COMPLETED");
            jdMapper.updateById(jd);

            log.info("JD匹配分析完成, jdId={}, matchScore={}", jdId, jd.getMatchScore());

        } catch (Exception e) {
            log.error("JD匹配分析失败, jdId={}", jdId, e);
            jd.setStatus("FAILED");
            jdMapper.updateById(jd);
        }
    }
}
