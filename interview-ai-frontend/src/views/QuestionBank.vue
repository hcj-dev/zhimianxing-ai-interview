<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { questionApi, type QuestionItem } from '@/api/question'

const router = useRouter()

const searchKeyword = ref('')
const selectedTag = ref('')
const selectedDifficulty = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const questions = ref<QuestionItem[]>([])
const loading = ref(true)

async function fetchQuestions() {
  loading.value = true
  try {
    const res = await questionApi.list({
      keyword: searchKeyword.value || undefined,
      tag: selectedTag.value || undefined,
      difficulty: selectedDifficulty.value ?? undefined,
      page: currentPage.value,
      pageSize,
    })
    const data = res.data.data
    questions.value = data.records
    total.value = data.total
  } catch {
    questions.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// Extract unique tags from loaded questions for filter dropdown
const availableTags = ref<string[]>(['Java', 'Spring', 'MySQL', 'Redis', '并发', 'JVM', '微服务', '网络', '计算机基础', '数据结构', '分布式'])

let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchChange() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchQuestions()
  }, 300)
}

onMounted(fetchQuestions)
watch([selectedTag, selectedDifficulty, currentPage], fetchQuestions)
watch(searchKeyword, onSearchChange)

function getDifficultyTag(d: number) {
  const map: Record<number, any> = { 1: 'success', 2: 'warning', 3: 'danger' }
  const label: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
  return { type: map[d] || 'info', label: label[d] || String(d) }
}

function parseTags(tags: string | null): string[] {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>题库浏览</h2>
        <p>按知识点标签筛选，找到你需要练习的题目</p>
      </div>
      <el-button @click="router.push('/questions/wrong')">
        <el-icon><WarningFilled /></el-icon>我的错题本
      </el-button>
    </div>

    <el-card style="margin-bottom: 16px;">
      <div class="filters">
        <el-input v-model="searchKeyword" placeholder="搜索题目..." clearable style="width: 240px;" />
        <el-select v-model="selectedTag" placeholder="知识点标签" clearable style="width: 160px;">
          <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-select v-model="selectedDifficulty" placeholder="难度" clearable style="width: 120px;">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <span class="filter-result">共 {{ total }} 道题</span>
      </div>
    </el-card>

    <el-card v-loading="loading">
      <div v-if="!loading && questions.length === 0" class="empty-state">
        <el-icon :size="48" color="#CBD5E1"><Collection /></el-icon>
        <p>暂无题目数据</p>
        <p class="sub">题目库正在建设中，请稍后再来</p>
      </div>

      <div v-for="q in questions" :key="q.id" class="question-item"
        @click="router.push(`/questions/${q.id}`)">
        <div class="q-left">
          <div class="q-title">{{ q.title }}</div>
          <div class="q-tags">
            <el-tag v-for="t in parseTags(q.tags)" :key="t" size="small" effect="plain" style="margin-right: 6px;">
              {{ t }}
            </el-tag>
          </div>
        </div>
        <div class="q-right">
          <el-tag :type="getDifficultyTag(q.difficulty).type" size="small" effect="light">
            {{ getDifficultyTag(q.difficulty).label }}
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  gap: 12px;
}
.filter-result { font-size: 13px; color: var(--text-secondary); margin-left: auto; }

.empty-state { text-align: center; padding: 60px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }
.empty-state .sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.question-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 0; border-bottom: 1px solid var(--border-light);
  cursor: pointer; transition: background var(--transition-fast);
}
.question-item:hover {
  background: var(--color-primary-bg); margin: 0 -20px; padding: 14px 20px;
  border-radius: var(--radius-sm);
}
.q-title { font-size: 14px; font-weight: 500; margin-bottom: 6px; }
.q-tags { display: flex; flex-wrap: wrap; }
.q-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; margin-left: 16px; }
</style>
