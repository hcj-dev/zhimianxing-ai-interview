import request from './request'
import type { ApiResponse } from '@/types/user'

export interface QuestionItem {
  id: number
  title: string
  content: string
  answer: string | null
  tags: string | null
  difficulty: number
  embeddingId: string | null
  createdAt: string
}

export interface QuestionListResponse {
  records: QuestionItem[]
  total: number
  page: number
  pageSize: number
}

export interface WrongItem {
  knowledgePoint: string
  frequency: number
  latestId: number
  lastWrong: string
}

export interface WrongDetail {
  wrong: {
    id: number; userId: number; sessionId: number
    knowledgePoint: string; questionText: string; userAnswer: string
    score: number; feedback: string; createdAt: string
  }
  similarWrongs: Array<{
    id: number; knowledgePoint: string; questionText: string
    userAnswer: string; score: number; feedback: string; createdAt: string
  }>
  relatedQuestions: QuestionItem[]
}

export const questionApi = {
  list(params: { keyword?: string; tag?: string; difficulty?: number; page?: number; pageSize?: number }) {
    return request.get<ApiResponse<QuestionListResponse>>('/questions', { params })
  },

  getById(id: number) {
    return request.get<ApiResponse<QuestionItem>>(`/questions/${id}`)
  },

  wrongList(params: { page?: number; pageSize?: number }) {
    return request.get<ApiResponse<{ records: WrongItem[]; total: number }>>('/questions/wrong', { params })
  },

  wrongDetail(id: number) {
    return request.get<ApiResponse<WrongDetail>>(`/questions/wrong/${id}`)
  },

  create(data: Partial<QuestionItem>) {
    return request.post<ApiResponse<QuestionItem>>('/questions', data)
  },

  update(id: number, data: Partial<QuestionItem>) {
    return request.put<ApiResponse<QuestionItem>>(`/questions/${id}`, data)
  },

  delete(id: number) {
    return request.delete<ApiResponse<null>>(`/questions/${id}`)
  },

  deleteWrong(id: number) {
    return request.delete<ApiResponse<null>>(`/questions/wrong/${id}`)
  },
}
