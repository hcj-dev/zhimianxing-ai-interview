export interface ResumeItem {
  id: number
  userId: number
  fileName: string
  fileUrl: string | null
  rawText: string | null
  structuredData: string | null
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  version: number
  createdAt: string
  updatedAt: string
}

export interface ResumeAnalysis {
  id: number
  resumeId: number
  skillScore: number | null
  descriptionQuality: number | null
  keywordCoverage: number | null
  formatScore: number | null
  overallScore: number | null
  suggestions: string | Suggestion[] | null
  strengths: string | StrengthWeakness[] | null
  weaknesses: string | StrengthWeakness[] | null
  optimizedResume: string | null
  createdAt: string
}

export interface Suggestion {
  type: string
  content: string
}

export interface StrengthWeakness {
  point: string
  detail: string
}

export interface ResumeDetail {
  resume: ResumeItem
  analysis: ResumeAnalysis | null
}

export interface JdItem {
  id: number
  resumeId: number
  userId: number
  jdText: string
  matchScore: number | null
  skillGap: string | null
  suggestions: string | null
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}
