package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(dashboardService.getStats(userId));
    }

    @GetMapping("/radar")
    public Result<Map<String, Object>> radar(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(dashboardService.getRadar(userId));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "30") int days,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(dashboardService.getTrend(userId, days));
    }
}
