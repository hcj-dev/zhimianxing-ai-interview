package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewai.entity.InterviewSession;
import com.interviewai.mapper.InterviewSessionMapper;
import com.interviewai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InterviewSessionMapper sessionMapper;

    @Override
    public Map<String, Object> getStats(Long userId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getStatus, "COMPLETED");

        List<InterviewSession> sessions = sessionMapper.selectList(wrapper);
        int total = sessions.size();
        double avgScore = sessions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToDouble(InterviewSession::getTotalScore)
                .average().orElse(0);
        double maxScore = sessions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToDouble(InterviewSession::getTotalScore)
                .max().orElse(0);
        int streak = calcStreak(sessions);

        return Map.of(
                "totalInterviews", total,
                "avgScore", Math.round(avgScore * 10) / 10.0,
                "maxScore", Math.round(maxScore * 10) / 10.0,
                "streakDays", streak
        );
    }

    @Override
    public Map<String, Object> getRadar(Long userId) {
        // 取最近一次面试的雷达数据
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getStatus, "COMPLETED")
               .isNotNull(InterviewSession::getRadarData)
               .orderByDesc(InterviewSession::getEndedAt)
               .last("LIMIT 1");

        InterviewSession session = sessionMapper.selectOne(wrapper);
        if (session == null || session.getRadarData() == null) {
            return Map.of("hasData", false);
        }
        return Map.of("hasData", true, "radarData", session.getRadarData());
    }

    @Override
    public Map<String, Object> getTrend(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getStatus, "COMPLETED")
               .ge(InterviewSession::getStartedAt, since)
               .orderByAsc(InterviewSession::getStartedAt);

        List<InterviewSession> sessions = sessionMapper.selectList(wrapper);
        List<Map<String, Object>> points = sessions.stream()
                .filter(s -> s.getTotalScore() != null)
                .map(s -> Map.of(
                        "date", s.getStartedAt().toLocalDate().toString(),
                        "score", (Object) s.getTotalScore()))
                .toList();

        return Map.of("points", points);
    }

    private int calcStreak(List<InterviewSession> sessions) {
        Set<LocalDate> dates = new HashSet<>();
        sessions.forEach(s -> {
            if (s.getStartedAt() != null) dates.add(s.getStartedAt().toLocalDate());
        });
        int streak = 0;
        LocalDate today = LocalDate.now();
        while (dates.contains(today)) {
            streak++;
            today = today.minusDays(1);
        }
        // 如果今天没有，检查昨天是否在连续
        if (streak == 0) {
            today = LocalDate.now().minusDays(1);
            while (dates.contains(today)) {
                streak++;
                today = today.minusDays(1);
            }
        }
        return streak;
    }
}
