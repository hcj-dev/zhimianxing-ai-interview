package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.entity.InterviewSession;
import com.interviewai.service.InterviewService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/interview/init")
    public Result<Map<String, Object>> init(@RequestBody Map<String, Object> body,
                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long resumeId = body.get("resumeId") != null ? Long.valueOf(body.get("resumeId").toString()) : null;
        String mode = body.get("mode").toString();
        String difficulty = body.get("difficulty") != null ? body.get("difficulty").toString() : "MEDIUM";
        @SuppressWarnings("unchecked")
        List<String> techTags = (List<String>) body.get("techTags");
        return Result.ok(interviewService.createSession(userId, resumeId, mode, techTags, difficulty));
    }

    @GetMapping(value = "/interview/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long sessionId, HttpServletResponse response) {
        // 关键：立即 flush headers，禁用所有缓冲
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        SseEmitter emitter = interviewService.streamQuestion(sessionId);
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ignored) { /* ignore */ }
        return emitter;
    }

    @PostMapping(value = "/interview/{sessionId}/answer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter answer(@PathVariable Long sessionId,
                             @RequestBody Map<String, String> body,
                             Authentication authentication,
                             HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = interviewService.answer(userId, sessionId, body.get("answer"));
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ignored) { /* ignore */ }
        return emitter;
    }

    @PostMapping(value = "/interview/{sessionId}/skip", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter skip(@PathVariable Long sessionId, Authentication authentication,
                           HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = interviewService.skip(userId, sessionId);
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ignored) { /* ignore */ }
        return emitter;
    }

    @PostMapping("/interview/{sessionId}/end")
    public Result<Void> end(@PathVariable Long sessionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        interviewService.end(userId, sessionId);
        return Result.ok();
    }

    @GetMapping("/interview/{sessionId}/status")
    public Result<InterviewSession> status(@PathVariable Long sessionId) {
        return Result.ok(interviewService.getSession(sessionId));
    }

    @GetMapping("/report/{sessionId}")
    public Result<Map<String, Object>> report(@PathVariable Long sessionId) {
        return Result.ok(interviewService.getReport(sessionId));
    }
}
