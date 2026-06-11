<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { questionApi, type QuestionItem } from '@/api/question'

const route = useRoute()
const router = useRouter()
const showAnswer = ref(false)
const loading = ref(true)

const question = ref<QuestionItem | null>(null)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res = await questionApi.getById(id)
    question.value = res.data.data
  } catch {
    question.value = null
  } finally {
    loading.value = false
  }
})

function parseTags(tags: string | null): string[] {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function getDifficultyLabel(d: number): string {
  const map: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
  return map[d] || String(d)
}

function getDifficultyType(d: number): string {
  const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[d] || 'info'
}
</script>

<template>
  <div class="page-container" style="max-width: 800px;">
    <div class="page-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
    </div>

    <el-card v-loading="loading">
      <div v-if="!loading && !question" class="empty-state">
        <el-icon :size="48" color="#CBD5E1"><WarningFilled /></el-icon>
        <p>题目不存在</p>
      </div>

      <template v-else-if="question">
        <div class="q-header">
          <div class="q-meta">
            <el-tag v-for="t in parseTags(question.tags)" :key="t" size="small" effect="plain"
              style="margin-right: 6px;">
              {{ t }}
            </el-tag>
            <el-tag :type="getDifficultyType(question.difficulty)" size="small" effect="light">
              {{ getDifficultyLabel(question.difficulty) }}
            </el-tag>
          </div>
          <h2 class="q-title">{{ question.title }}</h2>
          <div class="q-content">{{ question.content }}</div>
        </div>

        <div class="answer-section">
          <el-button v-if="!showAnswer" type="primary" @click="showAnswer = true">
            <el-icon><View /></el-icon>查看参考答案
          </el-button>
          <template v-else>
            <div class="answer-box">
              <div class="answer-label">
                <el-icon style="margin-right:4px;vertical-align:-2px"><Document /></el-icon>参考答案
              </div>
              <div class="answer-text">{{ question.answer || '暂无参考答案' }}</div>
            </div>
          </template>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.empty-state { text-align: center; padding: 60px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }

.q-header { margin-bottom: 20px; }
.q-meta { display: flex; align-items: center; margin-bottom: 12px; }
.q-title { font-size: 20px; font-weight: 700; margin-bottom: 12px; line-height: 1.4; }
.q-content { font-size: 14px; color: var(--text-secondary); line-height: 1.8; }

.answer-section { border-top: 1px solid var(--border-light); padding-top: 20px; text-align: center; }
.answer-box { text-align: left; }
.answer-label { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.answer-text {
  font-size: 13px; color: var(--text-secondary); line-height: 1.9;
  white-space: pre-wrap; padding: 16px; background: #F8FAFC;
  border-radius: var(--radius-sm); border: 1px solid var(--border-light);
}
</style>
