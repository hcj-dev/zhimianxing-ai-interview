import request from './request'
import type { ApiResponse } from '@/types/user'

export interface DashboardStats {
  totalInterviews: number
  avgScore: number
  maxScore: number
  streakDays: number
}

export interface RadarData {
  hasData: boolean
  radarData?: string
}

export interface TrendData {
  points: Array<{ date: string; score: number }>
}

export const dashboardApi = {
  getStats() {
    return request.get<ApiResponse<DashboardStats>>('/dashboard/stats')
  },
  getRadar() {
    return request.get<ApiResponse<RadarData>>('/dashboard/radar')
  },
  getTrend(days: number = 30) {
    return request.get<ApiResponse<TrendData>>('/dashboard/trend', { params: { days } })
  },
}
