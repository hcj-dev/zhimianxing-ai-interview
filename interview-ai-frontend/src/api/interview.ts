import request from './request'
import type { ApiResponse } from '@/types/user'

export interface InterviewSession {
  id: number; userId: number; resumeId: number | null; mode: string
  techTags: string | null; status: string; totalQuestions: number
  answeredCount: number; totalScore: number | null
  strengths: string | null; weaknesses: string | null
  radarData: string | null; duration: number | null
  startedAt: string; endedAt: string | null
}

export interface InterviewMessage {
  id: number; sessionId: number; sequence: number; role: string
  content: string; questionId: number | null; score: number | null
  feedback: string | null; createdAt: string
}

export interface InterviewReport {
  session: InterviewSession
  messages: InterviewMessage[]
}

export const interviewApi = {
  init(body: { resumeId?: number; mode: string; techTags?: string[]; difficulty?: string }) {
    return request.post<ApiResponse<{ sessionId: number; status: string }>>('/interview/init', body)
  },

  end(sessionId: number) {
    return request.post<ApiResponse<null>>(`/interview/${sessionId}/end`)
  },

  getStatus(sessionId: number) {
    return request.get<ApiResponse<InterviewSession>>(`/interview/${sessionId}/status`)
  },

  getReport(sessionId: number) {
    return request.get<ApiResponse<InterviewReport>>(`/report/${sessionId}`)
  },
}

// SSE helpers — use fetch + ReadableStream since EventSource doesn't support JWT headers
function sseFetch(
  url: string, method: string, body: any,
  onToken: (t: string) => void,
  onDone: (data: any) => void,
  onError: (err: string) => void
): AbortController {
  const ctrl = new AbortController()
  fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: ctrl.signal,
  }).then(async res => {
    // 非 200 响应：读取错误体并回调 onError
    if (!res.ok) {
      let errMsg = `请求失败 (${res.status})`
      try {
        const errText = await res.text()
        // 尝试从 JSON 错误体中提取 message
        const errJson = JSON.parse(errText)
        if (errJson.message) errMsg = errJson.message
        else if (errJson.msg) errMsg = errJson.msg
      } catch { /* 无法解析错误体，使用默认消息 */ }
      onError(errMsg)
      return
    }

    const reader = res.body?.getReader()
    if (!reader) { onError('无法读取响应流'); return }

    // 从响应头检查是否为 SSE（text/event-stream），非 SSE 响应直接报错
    const contentType = res.headers.get('content-type') || ''
    if (!contentType.includes('text/event-stream')) {
      const text = await reader.read()
      const body = text?.value ? new TextDecoder().decode(text.value) : ''
      let errMsg = `服务器返回了非流式响应`
      try {
        const errJson = JSON.parse(body)
        if (errJson.message) errMsg = errJson.message
        else if (errJson.msg) errMsg = errJson.msg
      } catch { /* */ }
      onError(errMsg)
      return
    }

    const dec = new TextDecoder()
    let buf = ''
    let currentEvent = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          const d = line.substring(5).trim()
          if (currentEvent === 'ERROR') {
            try {
              const err = JSON.parse(d)
              onError(err.message || '服务异常')
            } catch { onError(d) }
            currentEvent = ''
          } else if (d.startsWith('{') && (d.includes('sessionId') || d.includes('finished'))) {
            try { onDone(JSON.parse(d)) } catch { /* skip */ }
          } else onToken(d)
        }
      }
    }
  }).catch(err => {
    if (err.name !== 'AbortError') {
      // 区分网络错误和超时
      const msg = err.name === 'TypeError' && err.message === 'Failed to fetch'
        ? '网络连接失败，请检查网络后刷新重试'
        : err.message || '连接异常，请稍后重试'
      onError(msg)
    }
  })
  return ctrl
}

export function streamQuestion(sessionId: number, onToken: (t: string) => void, onDone: (d: any) => void, onErr: (e: string) => void) {
  return sseFetch(`/api/v1/interview/${sessionId}/stream`, 'GET', null, onToken, onDone, onErr)
}

export function sendAnswerSSE(sessionId: number, answer: string, onToken: (t: string) => void, onDone: (d: any) => void, onErr: (e: string) => void) {
  return sseFetch(`/api/v1/interview/${sessionId}/answer`, 'POST', { answer }, onToken, onDone, onErr)
}

export function skipQuestionSSE(sessionId: number, onToken: (t: string) => void, onDone: (d: any) => void, onErr: (e: string) => void) {
  return sseFetch(`/api/v1/interview/${sessionId}/skip`, 'POST', null, onToken, onDone, onErr)
}
