package com.interviewai.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：声明异步任务队列。
 * 简历分析和JD匹配都是耗时操作，通过队列异步处理，避免阻塞HTTP请求。
 */
@Configuration
public class RabbitMQConfig {

    public static final String RESUME_ANALYSIS_QUEUE = "resume.analysis.queue";
    public static final String JD_MATCH_QUEUE = "jd.match.queue";
    public static final String INTERVIEW_REPORT_QUEUE = "interview.report.queue";

    @Bean
    public Queue resumeAnalysisQueue() {
        return new Queue(RESUME_ANALYSIS_QUEUE, true);
    }

    @Bean
    public Queue jdMatchQueue() {
        return new Queue(JD_MATCH_QUEUE, true);
    }

    @Bean
    public Queue interviewReportQueue() {
        return new Queue(INTERVIEW_REPORT_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
