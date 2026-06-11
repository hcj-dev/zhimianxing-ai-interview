package com.interviewai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置。
 * ChatClient.Builder 由 Spring AI 自动装配（基于 ChatModel），
 * 但 ChatClient 本身不会自动创建，需要手动注册 Bean。
 */
@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
