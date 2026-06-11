<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { questionApi, type WrongItem, type QuestionItem } from '@/api/question'

const router = useRouter()
const loading = ref(true)
const wrongList = ref<WrongItem[]>([])
const relatedQuestions = ref<QuestionItem[]>([])

onMounted(async () => {
  try {
    const r = await questionApi.wrongList({ pageSize: 50 })
    wrongList.value = r.data.data.records
  } catch { wrongList.value = [] }
  finally { loading.value = false }
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>错题本</h2>
        <p>面试中答错的题自动收录，反复练习直到掌握</p>
      </div>
      <el-button @click="router.push('/questions')">
        <el-icon><Collection /></el-icon>浏览题库
      </el-button>
    </div>

    <el-card v-loading="loading">
      <div v-if="!loading && wrongList.length === 0" class="empty-state">
        <el-icon :size="56" color="#CBD5E1"><CircleCheckFilled /></el-icon>
        <p>暂无错题记录</p>
        <p class="sub">完成面试后，AI 会识别你的薄弱知识点并自动收录</p>
        <el-button type="primary" style="margin-top: 16px;" @click="router.push('/interview/start')">
          去模拟面试
        </el-button>
      </div>

      <div v-for="w in wrongList" :key="w.knowledgePoint" class="wrong-item"
        @click="router.push(`/questions/wrong/${w.latestId}`)">
        <div class="wrong-left">
          <div class="wrong-title">{{ w.knowledgePoint }}</div>
          <div class="wrong-tags">
            <el-tag size="small" effect="plain" type="warning">薄弱知识点</el-tag>
          </div>
        </div>
        <div class="wrong-right">
          <span class="wrong-count">出现 <strong>{{ w.frequency }}</strong> 次</span>
          <span class="wrong-date">{{ w.lastWrong?.slice(0, 10) || '' }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.empty-state { text-align: center; padding: 60px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }
.empty-state .sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.wrong-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 0; border-bottom: 1px solid var(--border-light);
  cursor: pointer; transition: background var(--transition-fast);
}
.wrong-item:hover {
  background: #FEF2F2; margin: 0 -20px; padding: 14px 20px;
  border-radius: var(--radius-sm);
}
.wrong-title { font-size: 14px; font-weight: 500; margin-bottom: 6px; }
.wrong-tags { display: flex; }
.wrong-right { text-align: right; flex-shrink: 0; margin-left: 16px; }
.wrong-count { font-size: 12px; color: var(--text-secondary); display: block; }
.wrong-count strong { color: var(--color-danger); }
.wrong-date { font-size: 11px; color: var(--text-muted); }
</style>
