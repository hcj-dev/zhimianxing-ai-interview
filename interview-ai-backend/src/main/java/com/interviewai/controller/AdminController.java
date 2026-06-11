package com.interviewai.controller;

import com.interviewai.common.BizException;
import com.interviewai.common.Result;
import com.interviewai.common.ResultCode;
import com.interviewai.entity.User;
import com.interviewai.mapper.UserMapper;
import com.interviewai.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;

    // 每个方法调用前校验管理员身份
    private User requireAdmin(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return user;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(Authentication auth) {
        requireAdmin(auth);
        return Result.ok(adminService.getPlatformStats());
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "30") int days, Authentication auth) {
        requireAdmin(auth);
        return Result.ok(adminService.getTrend(days));
    }

    @GetMapping("/top-users")
    public Result<Map<String, Object>> topUsers(@RequestParam(defaultValue = "5") int limit, Authentication auth) {
        requireAdmin(auth);
        return Result.ok(adminService.getTopUsers(limit));
    }

    @GetMapping("/popular-tags")
    public Result<Map<String, Object>> popularTags(@RequestParam(defaultValue = "7") int limit, Authentication auth) {
        requireAdmin(auth);
        return Result.ok(adminService.getPopularTags(limit));
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        requireAdmin(auth);
        return Result.ok(adminService.getUserList(page, pageSize, keyword));
    }

    @PutMapping("/users/{id}/toggle-status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, Authentication auth) {
        requireAdmin(auth);
        adminService.toggleUserStatus(id);
        return Result.ok();
    }
}
