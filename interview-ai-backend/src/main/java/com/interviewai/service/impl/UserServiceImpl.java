package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewai.common.BizException;
import com.interviewai.common.JwtUtils;
import com.interviewai.common.ResultCode;
import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.entity.User;
import com.interviewai.mapper.UserMapper;
import com.interviewai.service.FileService;
import com.interviewai.service.UserService;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final FileService fileService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        log.info("新用户注册: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 生成JWT，存入Redis。存Redis是为了支持"强制下线"——删掉Redis key即可让token失效
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String redisKey = "access_token:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, 7, TimeUnit.DAYS);

        log.info("用户登录成功: {}", request.getUsername());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .participateRanking(user.getParticipateRanking() != null ? user.getParticipateRanking() : 1)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void updateProfile(Long userId, String nickname, String email) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setEmail(email);
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.USER_OLD_PASSWORD_ERROR);
        }
        // 新旧密码不能相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException(ResultCode.USER_PASSWORD_SAME);
        }
        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(updateUser);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public void toggleParticipateRanking(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        int current = user.getParticipateRanking() != null ? user.getParticipateRanking() : 1;
        User update = new User();
        update.setId(userId);
        update.setParticipateRanking(current == 1 ? 0 : 1);
        userMapper.updateById(update);
        log.info("用户 {} 排行榜参与状态: {} -> {}", user.getUsername(), current, current == 1 ? 0 : 1);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择头像文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持上传图片文件");
        }
        // 限制 2MB
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像大小不能超过 2MB");
        }
        String avatarUrl = fileService.save(file, "avatars");
        User update = new User();
        update.setId(userId);
        update.setAvatarUrl(avatarUrl);
        userMapper.updateById(update);
        log.info("用户 {} 更新头像: {}", userId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public byte[] getAvatarData(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getAvatarUrl() == null) {
            return null; // 无头像，前端用默认图标
        }
        try {
            Path filePath = fileService.getFilePath(user.getAvatarUrl());
            if (!Files.exists(filePath)) {
                log.warn("头像文件不存在: {}", filePath);
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取头像文件失败: userId={}", userId, e);
            return null;
        }
    }
}
