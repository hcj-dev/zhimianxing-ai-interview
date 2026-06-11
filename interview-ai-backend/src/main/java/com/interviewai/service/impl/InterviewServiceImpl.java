package com.interviewai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.BizException;
import com.interviewai.common.ResultCode;
import com.interviewai.config.RabbitMQConfig;
import com.interviewai.entity.InterviewMessage;
import com.interviewai.entity.InterviewSession;
import com.interviewai.entity.Resume;
import com.interviewai.mapper.InterviewMessageMapper;
import com.interviewai.mapper.InterviewSessionMapper;
import com.interviewai.mapper.ResumeMapper;
import com.interviewai.service.InterviewService;
import com.interviewai.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ResumeMapper resumeMapper;
    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final VectorSearchService vectorSearchService;

    private static final int MAX_QUESTIONS = 10;
    private static final String CONTEXT_PREFIX = "interview:context:";
    private static final String DAILY_LIMIT_PREFIX = "daily:interview:";

    @Override
    public Map<String, Object> createSession(Long userId, Long resumeId, String mode, List<String> techTags, String difficulty) {
        // 每日限制
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String limitKey = DAILY_LIMIT_PREFIX + userId + ":" + today;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count == 1) redisTemplate.expire(limitKey, 1, TimeUnit.DAYS);
        if (count > 10) throw new BizException(ResultCode.INTERVIEW_QUOTA_EXCEEDED);

        // 简历摘要
        String resumeSummary = "";
        if (resumeId != null) {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume != null && resume.getRawText() != null) {
                resumeSummary = resume.getRawText().length() > 2000
                        ? resume.getRawText().substring(0, 2000) : resume.getRawText();
            }
        }

        // 创建场次
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(resumeId);
        session.setMode(mode);
        session.setTechTags(techTags != null ? objectMapper.valueToTree(techTags).toString() : null);
        session.setDifficulty(difficulty != null ? difficulty : "MEDIUM");
        session.setStatus("IN_PROGRESS");
        session.setTotalQuestions(MAX_QUESTIONS);
        session.setAnsweredCount(0);
        session.setStartedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        // 初始化Redis上下文
        Map<String, String> ctx = new HashMap<>();
        ctx.put("mode", mode);
        ctx.put("resumeSummary", resumeSummary);
        ctx.put("currentIndex", "-1"); // -1表示还没出题
        ctx.put("history", "[]");
        ctx.put("techTags", techTags != null ? String.join(",", techTags) : "");
        ctx.put("difficulty", difficulty != null ? difficulty : "MEDIUM");
        redisTemplate.opsForHash().putAll(CONTEXT_PREFIX + session.getId(), ctx);
        redisTemplate.expire(CONTEXT_PREFIX + session.getId(), 30, TimeUnit.MINUTES);

        log.info("面试场次创建, sessionId={}, mode={}", session.getId(), mode);
        return Map.of("sessionId", session.getId(), "status", "IN_PROGRESS");
    }

    @Override
    public SseEmitter streamQuestion(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"IN_PROGRESS".equals(session.getStatus())) {
            throw new BizException(ResultCode.INTERVIEW_SESSION_EXPIRED);
        }

        int idx = getCurrentIndex(sessionId);
        int dbCount = session.getAnsweredCount() != null ? session.getAnsweredCount() : 0;
        int realIdx = Math.max(idx, dbCount - 1);
        String prompt;
        if (realIdx < 0) {
            // 第一题
            String mode = getCtx(sessionId, "mode");
            String resume = getCtx(sessionId, "resumeSummary");
            String tags = getCtx(sessionId, "techTags");
            String diff = getCtx(sessionId, "difficulty");
            prompt = buildFirstPrompt(mode, resume, tags, diff);
        } else {
            // 当前题（已在history中）
            prompt = buildNextPrompt(sessionId);
        }

        return doStream(sessionId, prompt, realIdx + 1);
    }

    @Override
    public SseEmitter answer(Long userId, Long sessionId, String answer) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId))
            throw new BizException(ResultCode.INTERVIEW_NOT_FOUND);
        if (!"IN_PROGRESS".equals(session.getStatus()))
            throw new BizException(ResultCode.INTERVIEW_SESSION_EXPIRED);

        // 以 DB answeredCount 和 Redis currentIndex 两者中较大的为准
        int idx = getCurrentIndex(sessionId);
        int dbCount = session.getAnsweredCount() != null ? session.getAnsweredCount() : 0;
        int realIdx = Math.max(idx, dbCount - 1);

        // 保存用户回答
        InterviewMessage msg = new InterviewMessage();
        msg.setSessionId(sessionId);
        msg.setSequence(realIdx + 1);
        msg.setRole("USER");
        msg.setContent(answer);
        messageMapper.insert(msg);
        appendHistory(sessionId, "用户: " + answer);

        // 更新已回答数
        int newCount = Math.min(realIdx + 1, MAX_QUESTIONS);
        session.setAnsweredCount(newCount);
        sessionMapper.updateById(session);

        // 是否结束
        if (newCount >= MAX_QUESTIONS) {
            // 最后一题：给最终反馈，然后结束
            String prompt = buildFinalFeedbackPrompt(sessionId);
            return doStream(sessionId, prompt, realIdx + 1, true);
        }

        String prompt = buildFeedbackPrompt(sessionId);
        return doStream(sessionId, prompt, realIdx + 1);
    }

    @Override
    public SseEmitter skip(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId))
            throw new BizException(ResultCode.INTERVIEW_NOT_FOUND);
        if (!"IN_PROGRESS".equals(session.getStatus()))
            throw new BizException(ResultCode.INTERVIEW_SESSION_EXPIRED);

        // 以 DB answeredCount 和 Redis currentIndex 两者中较大的为准，防止 Redis 过期导致计数重置
        int idx = getCurrentIndex(sessionId);
        int dbCount = session.getAnsweredCount() != null ? session.getAnsweredCount() : 0;
        int realIdx = Math.max(idx, dbCount - 1); // answeredCount 是已回答数=idx+1，所以 idx=answeredCount-1

        // 更新已回答数（跳过也算消耗一题）
        int newCount = Math.min(realIdx + 1, MAX_QUESTIONS);
        session.setAnsweredCount(newCount);
        sessionMapper.updateById(session);

        if (newCount >= MAX_QUESTIONS) {
            endInterview(session);
            SseEmitter emitter = new SseEmitter(5000L);
            try {
                emitter.send(SseEmitter.event().name("DONE").data("{\"finished\":true}"));
                emitter.complete();
            } catch (IOException e) { emitter.completeWithError(e); }
            return emitter;
        }
        appendHistory(sessionId, "用户跳过此题");
        String prompt = buildNextPrompt(sessionId);
        return doStream(sessionId, prompt, realIdx + 1);
    }

    @Override
    public void end(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId))
            throw new BizException(ResultCode.INTERVIEW_NOT_FOUND);
        endInterview(session);
    }

    @Override
    public InterviewSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public Map<String, Object> getReport(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(ResultCode.INTERVIEW_NOT_FOUND);
        List<InterviewMessage> messages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId).orderByAsc(InterviewMessage::getSequence));
        return Map.of("session", session, "messages", messages);
    }

    // ===== private =====

    private SseEmitter doStream(Long sessionId, String prompt, int newIndex) {
        return doStream(sessionId, prompt, newIndex, false);
    }

    private SseEmitter doStream(Long sessionId, String prompt, int newIndex, boolean isFinal) {
        SseEmitter emitter = new SseEmitter(300000L);
        updateCtx(sessionId, "currentIndex", String.valueOf(newIndex));

        Flux<String> flux = chatClient.prompt().user(prompt).stream().content();
        StringBuilder full = new StringBuilder();

        // 将AI输出的每个chunk拆成单字符流，加30ms延迟模拟打字机
        flux.concatMap(chunk -> {
            java.util.List<String> chars = new java.util.ArrayList<>();
            for (char c : chunk.toCharArray()) chars.add(String.valueOf(c));
            return reactor.core.publisher.Flux.fromIterable(chars)
                    .delayElements(java.time.Duration.ofMillis(30));
        }).subscribe(
            c -> {
                try {
                    emitter.send(SseEmitter.event().name("TOKEN").data(c));
                    full.append(c);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            },
            error -> {
                log.error("SSE error sessionId={}", sessionId, error);
                try {
                    String errMsg = error.getMessage() != null ? error.getMessage() : "AI 服务暂时不可用";
                    if (errMsg.contains("Connection reset") || errMsg.contains("timeout")) {
                        errMsg = "AI 服务连接中断，请稍后重试";
                    }
                    emitter.send(SseEmitter.event().name("ERROR").data("{\"message\":\"" + errMsg + "\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            },
            () -> {
                try {
                    InterviewMessage msg = new InterviewMessage();
                    msg.setSessionId(sessionId);
                    msg.setSequence(newIndex + 1);
                    msg.setRole("AI");
                    msg.setContent(full.toString());
                    messageMapper.insert(msg);
                    appendHistory(sessionId, "AI: " + full);

                    // 最后一题：结束面试
                    if (isFinal) {
                        InterviewSession session = sessionMapper.selectById(sessionId);
                        if (session != null && "IN_PROGRESS".equals(session.getStatus())) {
                            endInterview(session);
                        }
                    }

                    String doneData = "{\"sessionId\":" + sessionId + ",\"questionIndex\":" + (newIndex + 1);
                    if (isFinal) doneData += ",\"finished\":true";
                    doneData += "}";
                    emitter.send(SseEmitter.event().name("DONE").data(doneData));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        );

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    private void endInterview(InterviewSession session) {
        session.setStatus("COMPLETED");
        session.setEndedAt(LocalDateTime.now());
        if (session.getStartedAt() != null)
            session.setDuration((int) java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds());
        sessionMapper.updateById(session);
        rabbitTemplate.convertAndSend(RabbitMQConfig.INTERVIEW_REPORT_QUEUE, Map.of("sessionId", session.getId()));
        redisTemplate.delete(CONTEXT_PREFIX + session.getId());
    }

    private String getCtx(Long sid, String key) {
        Object v = redisTemplate.opsForHash().get(CONTEXT_PREFIX + sid, key);
        return v != null ? v.toString() : "";
    }

    private void updateCtx(Long sid, String key, String val) {
        redisTemplate.opsForHash().put(CONTEXT_PREFIX + sid, key, val);
        // 每次更新都刷新过期时间，防止长时间面试导致 Redis 过期
        redisTemplate.expire(CONTEXT_PREFIX + sid, 30, TimeUnit.MINUTES);
    }

    private int getCurrentIndex(Long sid) {
        String v = getCtx(sid, "currentIndex");
        return v.isEmpty() ? -1 : Integer.parseInt(v);
    }

    @SuppressWarnings("unchecked")
    private void appendHistory(Long sid, String entry) {
        try {
            Object o = redisTemplate.opsForHash().get(CONTEXT_PREFIX + sid, "history");
            List<String> h = o != null ? objectMapper.readValue(o.toString(), List.class) : new ArrayList<>();
            h.add(entry);
            if (h.size() > 50) h = h.subList(h.size() - 50, h.size());
            updateCtx(sid, "history", objectMapper.writeValueAsString(h));
        } catch (Exception e) { log.warn("appendHistory failed", e); }
    }

    private String getHistory(Long sid) { return getCtx(sid, "history"); }

    private String buildFirstPrompt(String mode, String resume, String tags, String difficulty) {
        String limit = "题目控制在80字以内，简洁明了。";
        String noMd = "【重要】不要使用任何 Markdown 语法（如**、#、```、~~、表格等），直接输出纯文本。";
        String soloQ = "【重要】只输出一道面试题本身，不要加任何额外内容。不要自问自答，不要解释为什么问这个题，不要点评题目好坏，不要加「好的」等开场白。";
        String diffDesc = getDifficultyForQuestion(difficulty);
        if ("RESUME_DEEP".equals(mode))
            return "你是资深面试官。基于简历深挖项目经历。" + limit + diffDesc + soloQ + noMd + "\n简历：" + resume;
        if ("TECH_SPECIAL".equals(mode))
            return "你是资深面试官。就以下方向出题：" + tags + "。" + limit + diffDesc + soloQ + noMd;
        return "你是资深面试官。混合技术基础与项目深挖。" + limit + diffDesc + soloQ + noMd + "\n简历：" + resume;
    }

    private String buildFeedbackPrompt(Long sid) {
        String diffDesc = getDifficultyForFeedback(getCtx(sid, "difficulty"));
        return "你是面试官。先对【用户刚才的回答】给1句简短点评（不是点评自己的题目！），然后提出下一题。总共控制在100字以内。" +
            "【重要】点评只针对用户回答的优劣，不要评价你自己的题目。只输出「点评+下一题」，不要有其他内容。" +
            diffDesc +
            "【重要】不要使用任何 Markdown 语法（如**、#、```、~~、表格等），直接输出纯文本。\n历史：\n" + getHistory(sid);
    }

    private String buildNextPrompt(Long sid) {
        String diffDesc = getDifficultyForQuestion(getCtx(sid, "difficulty"));
        return "你是面试官。提出下一道面试题，80字以内。" +
            "【重要】只输出一道面试题本身，不要加任何额外内容（不要自问自答，不要点评，不要解释）。" +
            diffDesc +
            "【重要】不要使用任何 Markdown 语法（如**、#、```、~~、表格等），直接输出纯文本。\n历史：\n" + getHistory(sid);
    }

    private String buildFinalFeedbackPrompt(Long sid) {
        return "你是面试官。面试已全部结束（共" + MAX_QUESTIONS + "题）。" +
            "请对用户在整个面试中的表现给出总结性评价，包括：整体亮点、主要不足、以及后续学习建议。控制在150字以内。" +
            "【重要】不要出下一题，不要使用 Markdown 语法，直接输出纯文本。\n历史：\n" + getHistory(sid);
    }

    /** 仅出题用的难度指令 — 不含任何"点评"相关描述，防止 AI 在出题时自问自答 */
    private String getDifficultyForQuestion(String difficulty) {
        if (difficulty == null) difficulty = "MEDIUM";
        return switch (difficulty) {
            case "EASY" -> "题目难度：简单。侧重基础概念和常见用法，适合初学者。";
            case "MEDIUM" -> "题目难度：中等。考察常规技术要点和实战场景。";
            case "HARD" -> "题目难度：困难。考察深度理解、底层原理和复杂场景设计。";
            case "HELL" -> "题目难度：炼狱。每道题只聚焦一个极其刁钻的知识点，必须深挖到源码级或OS级原理。题目要让人第一眼觉得无从下手，需要层层剥开才能找到答案。示例风格：『ConcurrentHashMap在JDK8中扩容时，如果两个线程同时触发transfer，怎么保证不会丢失数据？请从源码层面说明』。不允许问简单或中等难度的问题。";
            default -> "题目难度：中等。考察常规技术要点和实战场景。";
        };
    }

    /** 点评+出下一题用的难度指令 — 包含点评要求，供反馈阶段使用 */
    private String getDifficultyForFeedback(String difficulty) {
        if (difficulty == null) difficulty = "MEDIUM";
        return switch (difficulty) {
            case "EASY" -> "题目难度：简单。点评以鼓励为主。";
            case "MEDIUM" -> "题目难度：中等。点评要指出亮点和不足。";
            case "HARD" -> "题目难度：困难。点评要严格，直击要害。";
            case "HELL" -> "题目难度：炼狱。下一题必须源码级/OS级深度。点评要一针见血地指出缺陷，毫不留情。";
            default -> "题目难度：中等。点评要指出亮点和不足。";
        };
    }

    /**
     * 从 Qdrant 向量检索相关题目作为出题参考，让 AI 出题更贴近真实题库风格。
     */
    private String searchSimilarQuestions(String query) {
        try {
            var results = vectorSearchService.searchQuestions(query, 3);
            if (results.isEmpty()) return "（无参考题）";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                sb.append(i + 1).append(". ").append(results.get(i).get("content")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "（检索失败）";
        }
    }
}
