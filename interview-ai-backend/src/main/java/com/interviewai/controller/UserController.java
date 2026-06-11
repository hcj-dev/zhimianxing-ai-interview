package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.service.UserService;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = userService.login(request);
        return Result.ok(vo);
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(Authentication authentication,
                                      @RequestParam(required = false) String nickname,
                                      @RequestParam(required = false) String email) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updateProfile(userId, nickname, email);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @RequestParam String oldPassword,
                                       @RequestParam String newPassword) {
        Long userId = (Long) authentication.getPrincipal();
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.ok();
    }

    @PutMapping("/ranking-participation")
    public Result<Void> toggleRankingParticipation(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.toggleParticipateRanking(userId);
        return Result.ok();
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.ok(Map.of("avatarUrl", avatarUrl));
    }

    /**
     * 获取用户头像图片（无需登录）。通过 API 返回字节流，不依赖静态文件映射。
     * 前端 <img :src="`/api/v1/user/avatar/${userId}`"> 直接加载，浏览器原生请求不含 Token。
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        byte[] data = userService.getAvatarData(userId);
        if (data == null || data.length == 0) {
            return ResponseEntity.noContent().build();
        }
        // 根据文件头判断 Content-Type
        String contentType = "image/png";
        if (data.length > 3 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            contentType = "image/jpeg";
        } else if (data.length > 3 && data[0] == (byte) 0x47 && data[1] == (byte) 0x49) {
            contentType = "image/gif";
        } else if (data.length > 3 && data[0] == (byte) 0x52 && data[1] == (byte) 0x49) {
            contentType = "image/webp";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(data);
    }
}
