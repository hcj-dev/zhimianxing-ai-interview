package com.interviewai.service;

import java.util.Map;

public interface AdminService {
    Map<String, Object> getPlatformStats();
    Map<String, Object> getTrend(int days);
    Map<String, Object> getTopUsers(int limit);
    Map<String, Object> getPopularTags(int limit);
    Map<String, Object> getUserList(int page, int pageSize, String keyword);
    void toggleUserStatus(Long userId);
}
