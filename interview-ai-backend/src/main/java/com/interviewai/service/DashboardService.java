package com.interviewai.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getStats(Long userId);
    Map<String, Object> getRadar(Long userId);
    Map<String, Object> getTrend(Long userId, int days);
}
