import request from './request'
import type { ApiResponse, LoginRequest, LoginVO, RegisterRequest, UserProfile } from '@/types/user'

export const userApi = {
  register(data: RegisterRequest) {
    return request.post<ApiResponse<null>>('/user/register', data)
  },

  login(data: LoginRequest) {
    return request.post<ApiResponse<LoginVO>>('/user/login', data)
  },

  getProfile() {
    return request.get<ApiResponse<UserProfile>>('/user/profile')
  },

  updateProfile(nickname?: string, email?: string) {
    return request.put<ApiResponse<null>>('/user/profile', null, {
      params: { nickname, email },
    })
  },

  changePassword(oldPassword: string, newPassword: string) {
    return request.put<ApiResponse<null>>('/user/password', null, {
      params: { oldPassword, newPassword },
    })
  },

  toggleRankingParticipation() {
    return request.put<ApiResponse<null>>('/user/ranking-participation')
  },

  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ApiResponse<{ avatarUrl: string }>>('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
