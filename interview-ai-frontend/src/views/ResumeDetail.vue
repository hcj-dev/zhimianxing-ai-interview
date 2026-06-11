<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resumeApi } from '@/api/resume'
import type { ResumeDetail, Suggestion, StrengthWeakness } from '@/types/resume'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const data = ref<ResumeDetail | null>(null)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res = await resumeApi.getById(id)
    data.value = res.data.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
})

function parseIfString(v: any): any[] {
  if (!v) return []
  if (Array.isArray(v)) return v
  if (typeof v === 'string') {
    try { const parsed = JSON.parse(v); return Array.isArray(parsed) ? parsed : [] }
    catch { return [] }
  }
  return []
}

const strengths = computed<StrengthWeakness[]>(() => parseIfString(data.value?.analysis?.strengths))
const weaknesses = computed<StrengthWeakness[]>(() => parseIfString(data.value?.analysis?.weaknesses))
const suggestions = computed<Suggestion[]>(() => parseIfString(data.value?.analysis?.suggestions))

const scoreBars = computed(() => {
  const a = data.value?.analysis
  if (!a) return []
  return [
    { label: '技术栈完整度', score: a.skillScore ?? 0, color: '#4F46E5' },
    { label: '描述质量', score: a.descriptionQuality ?? 0, color: '#0D9488' },
    { label: '关键词覆盖', score: a.keywordCoverage ?? 0, color: '#D97706' },
    { label: '格式排版', score: a.formatScore ?? 0, color: '#DC2626' },
  ]
})

const activeTab = ref('analysis')

const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const res = await resumeApi.exportMarkdown(Number(route.params.id))
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url; a.download = data.value?.resume?.fileName?.replace(/\.[^.]+$/, '') + '_优化版.md' || 'resume.md'; a.click()
    URL.revokeObjectURL(url)
  } catch { /* ignore */ }
  finally { exporting.value = false }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 v-if="data">{{ data.resume.fileName }}</h2>
        <p v-if="data">
          版本 v{{ data.resume.version }} · 上传于 {{ data.resume.createdAt }}
          ·
          <el-tag v-if="data.resume.status === 'COMPLETED'" type="success" size="small" effect="light">已完成</el-tag>
          <el-tag v-else-if="data.resume.status === 'PROCESSING'" type="warning" size="small" effect="light">分析中</el-tag>
          <el-tag v-else type="danger" size="small" effect="light">失败</el-tag>
        </p>
      </div>
      <div class="header-actions" v-if="data">
        <el-button v-if="data.analysis" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon>导出Markdown
        </el-button>
        <el-button @click="router.push(`/resumes/${data.resume.id}/match-jd`)">
          <el-icon><Connection /></el-icon>JD匹配
        </el-button>
        <el-button type="primary" @click="router.push('/interview/start')">
          <el-icon><VideoCamera /></el-icon>用此简历面试
        </el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <div v-if="!loading && !data" class="empty-state">
        <el-icon :size="48" color="#CBD5E1"><Document /></el-icon>
        <p>简历不存在</p>
        <el-button style="margin-top: 12px;" @click="router.push('/resumes')">返回列表</el-button>
      </div>

      <!-- Analysis Report -->
      <template v-else-if="data?.analysis">
        <div class="overall-score-box">
          <div class="score-ring">
            <span class="score-num">{{ data.analysis.overallScore }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="score-bars">
            <div v-for="bar in scoreBars" :key="bar.label" class="score-bar-item">
              <div class="score-bar-label">{{ bar.label }}</div>
              <div class="score-bar-track">
                <div class="score-bar-fill" :style="{ width: bar.score + '%', background: bar.color }" />
              </div>
              <span class="score-bar-val">{{ bar.score }}</span>
            </div>
          </div>
        </div>

        <el-tabs v-model="activeTab" style="margin-top: 8px;">
          <el-tab-pane label="AI 分析报告" name="analysis">
            <el-row :gutter="20">
              <el-col :span="12">
                <h3 class="panel-title">✅ 优势</h3>
                <div v-for="s in strengths" :key="s.point" class="feedback-item good">
                  <div class="feedback-point">{{ s.point }}</div>
                  <div class="feedback-detail">{{ s.detail }}</div>
                </div>
              </el-col>
              <el-col :span="12">
                <h3 class="panel-title">⚠️ 待改进</h3>
                <div v-for="w in weaknesses" :key="w.point" class="feedback-item bad">
                  <div class="feedback-point">{{ w.point }}</div>
                  <div class="feedback-detail">{{ w.detail }}</div>
                </div>
              </el-col>
            </el-row>

            <h3 class="panel-title" style="margin-top: 24px;">📋 修改建议</h3>
            <div v-for="s in suggestions" :key="s.content" class="suggestion-item">
              <el-tag :type="s.type === 'high' ? 'danger' : s.type === 'medium' ? 'warning' : 'info'"
                size="small" effect="light">
                {{ s.type === 'high' ? '优先' : s.type === 'medium' ? '建议' : '可选' }}
              </el-tag>
              <span>{{ s.content }}</span>
            </div>
          </el-tab-pane>

          <el-tab-pane label="简历原文" name="raw">
            <div class="raw-text">{{ data.resume.rawText || '暂无文本内容' }}</div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- Still Processing -->
      <div v-else-if="data?.resume.status === 'PROCESSING'" class="processing-hint">
        <el-icon :size="48" color="#D97706"><Loading /></el-icon>
        <p>AI 正在分析你的简历，请稍后刷新页面查看结果</p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header { display: flex; align-items: flex-start; justify-content: space-between; }
.header-actions { display: flex; gap: 10px; }
.empty-state { text-align: center; padding: 60px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }

.overall-score-box { display: flex; align-items: center; gap: 40px; margin-bottom: 8px; }
.score-ring {
  width: 100px; height: 100px; border-radius: 50%;
  background: conic-gradient(var(--color-primary) 0% 78%, #E2E8F0 78% 100%);
  display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative;
}
.score-ring::after {
  content: ''; position: absolute; width: 78px; height: 78px;
  border-radius: 50%; background: white;
}
.score-num { font-size: 28px; font-weight: 800; color: var(--color-primary); z-index: 1; line-height: 1; }
.score-unit { font-size: 12px; color: var(--text-secondary); z-index: 1; }

.score-bars { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.score-bar-item { display: flex; align-items: center; gap: 12px; }
.score-bar-label { width: 90px; font-size: 13px; color: var(--text-secondary); text-align: right; flex-shrink: 0; }
.score-bar-track { flex: 1; height: 8px; background: #F1F5F9; border-radius: 4px; overflow: hidden; }
.score-bar-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.score-bar-val { width: 28px; font-size: 14px; font-weight: 700; color: var(--text-primary); }

.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; color: var(--text-primary); }
.feedback-item { padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 8px; border-left: 3px solid; }
.feedback-item.good { background: #F0FDF4; border-color: #22C55E; }
.feedback-item.bad { background: #FEF2F2; border-color: #EF4444; }
.feedback-point { font-weight: 600; font-size: 13px; margin-bottom: 4px; }
.feedback-detail { font-size: 12px; color: var(--text-secondary); }

.suggestion-item { display: flex; align-items: flex-start; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-light); font-size: 13px; }
.raw-text { white-space: pre-wrap; font-size: 13px; color: var(--text-secondary); line-height: 1.8; padding: 16px; background: var(--bg-input); border-radius: var(--radius-sm); }

.processing-hint { text-align: center; padding: 60px 0; color: var(--text-secondary); }
.processing-hint p { margin-top: 12px; font-size: 14px; }
</style>
