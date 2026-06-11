package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewai.entity.*;
import com.interviewai.mapper.*;
import com.interviewai.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final ResumeAnalysisMapper resumeAnalysisMapper;
    private final InterviewSessionMapper sessionMapper;
    private final ResumeMapper resumeMapper;
    private final UserMapper userMapper;

    @Override
    public List<Map<String, Object>> getResumeRanking() {
        Set<Long> optOutUsers = getOptOutUserIds();
        // 1. 所有简历分析记录
        List<ResumeAnalysis> analyses = resumeAnalysisMapper.selectList(null);
        // 2. 所有简历 → resumeId -> userId
        Map<Long, Long> resumeUserMap = new HashMap<>();
        List<Resume> resumes = resumeMapper.selectList(null);
        for (Resume r : resumes) {
            resumeUserMap.put(r.getId(), r.getUserId());
        }
        // 3. 按 userId 取最高分（排除退出排行的用户）
        Map<Long, Integer> bestScore = new HashMap<>();
        for (ResumeAnalysis a : analyses) {
            Long uid = resumeUserMap.get(a.getResumeId());
            if (uid == null || optOutUsers.contains(uid)) continue;
            int s = a.getOverallScore() != null ? a.getOverallScore() : 0;
            bestScore.merge(uid, s, Math::max);
        }
        // 4. 排序取 top10，补用户信息
        return buildRanking(bestScore);
    }

    @Override
    public List<Map<String, Object>> getInterviewRanking() {
        Set<Long> optOutUsers = getOptOutUserIds();
        // 已完成且已评分的面试
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, "COMPLETED")
                        .isNotNull(InterviewSession::getTotalScore));
        // 按 userId 算均分（排除退出排行的用户）
        Map<Long, List<Double>> userScores = new HashMap<>();
        for (InterviewSession s : sessions) {
            if (s.getTotalScore() == null || optOutUsers.contains(s.getUserId())) continue;
            userScores.computeIfAbsent(s.getUserId(), k -> new ArrayList<>())
                    .add(s.getTotalScore().doubleValue());
        }
        // 排序取 top10
        Map<Long, Double> avgScores = new HashMap<>();
        for (var e : userScores.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            avgScores.put(e.getKey(), Math.round(avg * 10.0) / 10.0);
        }
        return buildRanking(avgScores);
    }

    @Override
    public List<Map<String, Object>> getComprehensiveRanking() {
        Set<Long> optOutUsers = getOptOutUserIds();
        // 简历最高分
        List<ResumeAnalysis> analyses = resumeAnalysisMapper.selectList(null);
        Map<Long, Long> resumeUserMap = new HashMap<>();
        for (Resume r : resumeMapper.selectList(null)) {
            resumeUserMap.put(r.getId(), r.getUserId());
        }
        Map<Long, Integer> rMap = new HashMap<>();
        for (ResumeAnalysis a : analyses) {
            Long uid = resumeUserMap.get(a.getResumeId());
            if (uid == null || a.getOverallScore() == null || optOutUsers.contains(uid)) continue;
            rMap.merge(uid, a.getOverallScore(), Math::max);
        }

        // 面试均分
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, "COMPLETED")
                        .isNotNull(InterviewSession::getTotalScore));
        Map<Long, List<Double>> userScoreList = new HashMap<>();
        for (InterviewSession s : sessions) {
            if (s.getTotalScore() == null || optOutUsers.contains(s.getUserId())) continue;
            userScoreList.computeIfAbsent(s.getUserId(), k -> new ArrayList<>())
                    .add(s.getTotalScore().doubleValue());
        }
        Map<Long, Double> iMap = new HashMap<>();
        for (var e : userScoreList.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            iMap.put(e.getKey(), Math.round(avg * 10.0) / 10.0);
        }

        // 收集所有用户
        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(rMap.keySet());
        allUserIds.addAll(iMap.keySet());

        // 归一化取综合分
        double rMax = rMap.values().stream().max(Integer::compare).orElse(100);
        double iMax = iMap.values().stream().max(Double::compare).orElse(100.0);
        if (rMax == 0) rMax = 100;
        if (iMax == 0) iMax = 100;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long uid : allUserIds) {
            double rNorm = rMap.getOrDefault(uid, 0) / rMax * 100;
            double iNorm = iMap.getOrDefault(uid, 0.0) / iMax * 100;
            double composite = Math.round((rNorm + iNorm) / 2.0 * 10.0) / 10.0;

            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", uid);
            entry.put("score", composite);
            entry.put("resumeScore", rMap.containsKey(uid) ? rMap.get(uid) : null);
            entry.put("interviewScore", iMap.containsKey(uid) ? iMap.get(uid) : null);
            result.add(entry);
        }
        result.sort((a, b) -> Double.compare(toDouble(b.get("score")), toDouble(a.get("score"))));
        return enrichUserInfo(result.subList(0, Math.min(10, result.size())));
    }

    // ===== helpers =====

    /** 从 userId -> Number 的 map 构建排序后的 top10 结果列表 */
    private List<Map<String, Object>> buildRanking(Map<Long, ? extends Number> userScoreMap) {
        return userScoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue().doubleValue(), a.getValue().doubleValue()))
                .limit(10)
                .map(e -> {
                    Long uid = e.getKey();
                    Number score = e.getValue();
                    var user = userMapper.selectById(uid);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("userId", uid);
                    entry.put("score", score.doubleValue() % 1 == 0 ? score.intValue() : score);
                    entry.put("username", user != null ? user.getUsername() : "未知");
                    entry.put("nickname", user != null ? user.getNickname() : "未知");
                    return entry;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> enrichUserInfo(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Long uid = toLong(row.get("userId"));
            var user = userMapper.selectById(uid);
            row.put("username", user != null ? user.getUsername() : "未知");
            row.put("nickname", user != null ? user.getNickname() : "未知");
        }
        return rows;
    }

    /** 查出所有不参与排行榜的用户ID */
    private Set<Long> getOptOutUserIds() {
        List<User> all = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .eq(User::getParticipateRanking, 0));
        return all.stream().map(User::getId).collect(Collectors.toSet());
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        return Long.parseLong(v.toString());
    }

    private double toDouble(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }
}
