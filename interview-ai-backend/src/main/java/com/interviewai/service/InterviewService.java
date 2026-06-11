package com.interviewai.service;

import com.interviewai.entity.InterviewSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface InterviewService {
    Map<String, Object> createSession(Long userId, Long resumeId, String mode, List<String> techTags, String difficulty);
    SseEmitter streamQuestion(Long sessionId);
    SseEmitter answer(Long userId, Long sessionId, String answer);
    SseEmitter skip(Long userId, Long sessionId);
    void end(Long userId, Long sessionId);
    InterviewSession getSession(Long sessionId);
    Map<String, Object> getReport(Long sessionId);
}
