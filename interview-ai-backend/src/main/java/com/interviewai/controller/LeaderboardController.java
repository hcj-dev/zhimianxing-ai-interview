package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/resume")
    public Result<List<Map<String, Object>>> resumeRanking() {
        return Result.ok(leaderboardService.getResumeRanking());
    }

    @GetMapping("/interview")
    public Result<List<Map<String, Object>>> interviewRanking() {
        return Result.ok(leaderboardService.getInterviewRanking());
    }

    @GetMapping("/comprehensive")
    public Result<List<Map<String, Object>>> comprehensiveRanking() {
        return Result.ok(leaderboardService.getComprehensiveRanking());
    }
}
