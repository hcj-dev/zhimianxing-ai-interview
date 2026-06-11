import request from './request'
import type { ApiResponse } from '@/types/user'

export interface RankEntry {
  userId: number
  score: number
  username: string
  nickname: string
  resumeScore?: number  // only in comprehensive
  interviewScore?: number // only in comprehensive
}

export const leaderboardApi = {
  getResumeRanking() {
    return request.get<ApiResponse<RankEntry[]>>('/leaderboard/resume')
  },
  getInterviewRanking() {
    return request.get<ApiResponse<RankEntry[]>>('/leaderboard/interview')
  },
  getComprehensiveRanking() {
    return request.get<ApiResponse<RankEntry[]>>('/leaderboard/comprehensive')
  },
}
