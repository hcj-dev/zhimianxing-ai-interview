package com.interviewai.controller;

import com.interviewai.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI连通性测试。
 * Phase 1 只验证能否正常调用DeepSeek API。
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        String reply = chatClient.prompt()
                .user("请用一句话介绍你自己")
                .call()
                .content();
        return Result.ok(Map.of("reply", reply));
    }
}
