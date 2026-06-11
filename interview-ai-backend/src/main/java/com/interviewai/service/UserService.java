package com.interviewai.service;

import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    void register(RegisterRequest request);
    LoginVO login(LoginRequest request);
    UserProfileVO getProfile(Long userId);
    void updateProfile(Long userId, String nickname, String email);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void toggleParticipateRanking(Long userId);
    String uploadAvatar(Long userId, MultipartFile file);
    byte[] getAvatarData(Long userId);
}
