package com.interviewai.service;

import java.util.List;
import java.util.Map;

public interface LeaderboardService {
    /** 简历评分排行榜 (top 10) */
    List<Map<String, Object>> getResumeRanking();

    /** 面试平均分排行榜 (top 10) */
    List<Map<String, Object>> getInterviewRanking();

    /** 综合排行榜 (top 10) */
    List<Map<String, Object>> getComprehensiveRanking();
}
