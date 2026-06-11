export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  avatarUrl: string
  role: string
  participateRanking: number
  createdAt: string
}

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}
