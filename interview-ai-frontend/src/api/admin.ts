import request from './request'
import type { ApiResponse } from '@/types/user'

export interface PlatformStats {
  totalUsers: number; totalResumes: number; totalInterviews: number
  totalQuestions: number; todayInterviews: number; platformAvgScore: number
  completionRate: number; completedInterviews: number
  modeDistribution: Record<string, number>
  difficultyDistribution: Record<string, number>
  scoreDistribution: Record<string, number>
}

export interface AdminUser {
  id: number; username: string; nickname: string; email: string
  role: string; status: number; createdAt: string
}

export interface AdminUserList {
  records: AdminUser[]; total: number
}

export interface TrendPoint {
  date: string; interviews: number; newUsers: number
}

export interface TrendData {
  points: TrendPoint[]
}

export interface TopUser {
  userId: number; nickname: string; interviewCount: number; avgScore: number
}

export interface TopUsersData {
  users: TopUser[]
}

export interface PopularTag {
  name: string; count: number; percentage: number
}

export interface PopularTagsData {
  tags: PopularTag[]
}

export const adminApi = {
  getStats() { return request.get<ApiResponse<PlatformStats>>('/admin/stats') },
  getTrend(days: number = 30) {
    return request.get<ApiResponse<TrendData>>('/admin/trend', { params: { days } })
  },
  getTopUsers(limit: number = 5) {
    return request.get<ApiResponse<TopUsersData>>('/admin/top-users', { params: { limit } })
  },
  getPopularTags(limit: number = 7) {
    return request.get<ApiResponse<PopularTagsData>>('/admin/popular-tags', { params: { limit } })
  },
  getUsers(params: { page?: number; pageSize?: number; keyword?: string }) {
    return request.get<ApiResponse<AdminUserList>>('/admin/users', { params })
  },
  toggleUserStatus(userId: number) { return request.put<ApiResponse<null>>(`/admin/users/${userId}/toggle-status`) },
}
