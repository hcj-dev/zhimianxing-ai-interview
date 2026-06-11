import request from './request'
import type { ApiResponse } from '@/types/user'
import type { ResumeItem, ResumeDetail, JdItem } from '@/types/resume'

export const resumeApi = {
  list() {
    return request.get<ApiResponse<ResumeItem[]>>('/resume/list')
  },

  getById(id: number) {
    return request.get<ApiResponse<ResumeDetail>>(`/resume/${id}`)
  },

  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ApiResponse<{ taskId: string; status: string }>>('/resume/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  delete(id: number) {
    return request.delete<ApiResponse<null>>(`/resume/${id}`)
  },

  exportMarkdown(id: number) {
    return request.get(`/resume/${id}/export`, {
      responseType: 'blob',
    })
  },

  submitJdMatch(resumeId: number, jdText: string) {
    return request.post<ApiResponse<{ taskId: string; status: string }>>(
      `/resume/${resumeId}/match-jd`,
      { jdText }
    )
  },

  getJdMatchResults(resumeId: number) {
    return request.get<ApiResponse<JdItem[]>>(`/resume/${resumeId}/match-jd`)
  },
}
