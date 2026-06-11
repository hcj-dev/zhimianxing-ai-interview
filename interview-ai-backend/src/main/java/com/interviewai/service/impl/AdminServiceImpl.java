package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.BizException;
import com.interviewai.common.ResultCode;
import com.interviewai.entity.InterviewSession;
import com.interviewai.entity.User;
import com.interviewai.mapper.InterviewSessionMapper;
import com.interviewai.mapper.QuestionMapper;
import com.interviewai.mapper.ResumeMapper;
import com.interviewai.mapper.UserMapper;
import com.interviewai.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final InterviewSessionMapper interviewMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalUsers = userMapper.selectCount(null);
        long totalResumes = resumeMapper.selectCount(null);
        long totalInterviews = interviewMapper.selectCount(null);
        long totalQuestions = questionMapper.selectCount(null);

        stats.put("totalUsers", totalUsers);
        stats.put("totalResumes", totalResumes);
        stats.put("totalInterviews", totalInterviews);
        stats.put("totalQuestions", totalQuestions);

        // 今日面试数
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InterviewSession> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(InterviewSession::getStartedAt, today.atStartOfDay());
        stats.put("todayInterviews", interviewMapper.selectCount(todayWrapper));

        // 所有面试
        List<InterviewSession> allSessions = interviewMapper.selectList(null);

        // 已完成且有分数的面试
        List<InterviewSession> done = allSessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()) && s.getTotalScore() != null)
                .toList();
        double avgScore = done.stream().mapToDouble(InterviewSession::getTotalScore).average().orElse(0);
        stats.put("platformAvgScore", Math.round(avgScore * 10) / 10.0);

        // 面试完成率
        double completionRate = totalInterviews > 0
                ? Math.round((double) done.size() / totalInterviews * 1000) / 10.0
                : 0;
        stats.put("completionRate", completionRate);
        stats.put("completedInterviews", done.size());

        // 模式分布
        Map<String, Long> modeDist = allSessions.stream()
                .filter(s -> s.getMode() != null)
                .collect(Collectors.groupingBy(InterviewSession::getMode, Collectors.counting()));
        stats.put("modeDistribution", modeDist);

        // 难度分布
        Map<String, Long> diffDist = allSessions.stream()
                .filter(s -> s.getDifficulty() != null)
                .collect(Collectors.groupingBy(InterviewSession::getDifficulty, Collectors.counting()));
        stats.put("difficultyDistribution", diffDist);

        // 分数分布（0-20, 20-40, 40-60, 60-80, 80-100）
        Map<String, Long> scoreDist = new LinkedHashMap<>();
        scoreDist.put("r0_20", done.stream().filter(s -> s.getTotalScore() < 20).count());
        scoreDist.put("r20_40", done.stream().filter(s -> s.getTotalScore() >= 20 && s.getTotalScore() < 40).count());
        scoreDist.put("r40_60", done.stream().filter(s -> s.getTotalScore() >= 40 && s.getTotalScore() < 60).count());
        scoreDist.put("r60_80", done.stream().filter(s -> s.getTotalScore() >= 60 && s.getTotalScore() < 80).count());
        scoreDist.put("r80_100", done.stream().filter(s -> s.getTotalScore() >= 80).count());
        stats.put("scoreDistribution", scoreDist);

        return stats;
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 查询范围内的面试
        LambdaQueryWrapper<InterviewSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.ge(InterviewSession::getStartedAt, startDate.atStartOfDay());
        List<InterviewSession> sessions = interviewMapper.selectList(sessionWrapper);

        // 查询范围内的新用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ge(User::getCreatedAt, startDate.atStartOfDay());
        List<User> newUsers = userMapper.selectList(userWrapper);

        // 按日期分组统计
        Map<LocalDate, Long> interviewByDate = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().toLocalDate(),
                        Collectors.counting()));

        Map<LocalDate, Long> usersByDate = newUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate(),
                        Collectors.counting()));

        // 构建每日数据点
        List<Map<String, Object>> points = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", d.toString());
            point.put("interviews", interviewByDate.getOrDefault(d, 0L));
            point.put("newUsers", usersByDate.getOrDefault(d, 0L));
            points.add(point);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        return result;
    }

    @Override
    public Map<String, Object> getTopUsers(int limit) {
        List<InterviewSession> allSessions = interviewMapper.selectList(null);

        // 按 userId 分组统计面试次数和平均分
        Map<Long, List<InterviewSession>> byUser = allSessions.stream()
                .filter(s -> s.getUserId() != null)
                .collect(Collectors.groupingBy(InterviewSession::getUserId));

        List<Map<String, Object>> topUsers = byUser.entrySet().stream()
                .map(entry -> {
                    long userId = entry.getKey();
                    List<InterviewSession> userSessions = entry.getValue();
                    int count = userSessions.size();
                    double avg = userSessions.stream()
                            .filter(s -> s.getTotalScore() != null)
                            .mapToDouble(InterviewSession::getTotalScore)
                            .average().orElse(0);

                    User user = userMapper.selectById(userId);
                    Map<String, Object> u = new HashMap<>();
                    u.put("userId", userId);
                    u.put("nickname", user != null ? user.getNickname() : "未知用户");
                    u.put("interviewCount", count);
                    u.put("avgScore", Math.round(avg * 10) / 10.0);
                    return u;
                })
                .sorted((a, b) -> Integer.compare(
                        (int) b.get("interviewCount"),
                        (int) a.get("interviewCount")))
                .limit(limit)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("users", topUsers);
        return result;
    }

    @Override
    public Map<String, Object> getPopularTags(int limit) {
        List<InterviewSession> allSessions = interviewMapper.selectList(null);

        // 解析 techTags JSON 数组并统计标签出现次数
        Map<String, Long> tagCounts = new HashMap<>();
        for (InterviewSession s : allSessions) {
            if (s.getTechTags() != null && !s.getTechTags().isBlank()) {
                try {
                    List<String> tags = objectMapper.readValue(
                            s.getTechTags(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    for (String tag : tags) {
                        tagCounts.merge(tag, 1L, Long::sum);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse techTags for session {}", s.getId());
                }
            }
        }

        long total = tagCounts.values().stream().mapToLong(Long::longValue).sum();

        List<Map<String, Object>> tags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("name", entry.getKey());
                    t.put("count", entry.getValue());
                    t.put("percentage", total > 0
                            ? Math.round(entry.getValue() * 1000.0 / total) / 10.0
                            : 0);
                    return t;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("tags", tags);
        return result;
    }

    @Override
    public Map<String, Object> getUserList(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        return map;
    }

    @Override
    public void toggleUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
    }
}
