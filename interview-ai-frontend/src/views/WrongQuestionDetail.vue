<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, type WrongDetail } from '@/api/question'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const mastering = ref(false)
const detail = ref<WrongDetail | null>(null)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const r = await questionApi.wrongDetail(id)
    detail.value = r.data.data
  } catch { detail.value = null }
  finally { loading.value = false }
})

async function markMastered() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      '确定已掌握该知识点？该错题记录将被清除。',
      '确认操作',
      { confirmButtonText: '已掌握', cancelButtonText: '再等等', type: 'success' }
    )
  } catch { return }

  mastering.value = true
  try {
    await questionApi.deleteWrong(detail.value.wrong.id)
    ElMessage.success('恭喜掌握！错题已清除')
    router.back()
  } catch {
  } finally {
    mastering.value = false
  }
}
</script>

<template>
  <div class="page-container" style="max-width:900px">
    <div class="page-header">
      <el-button text @click="router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
    </div>

    <el-card v-loading="loading">
      <template v-if="detail">
        <div class="detail-header">
          <h2>{{ detail.wrong.knowledgePoint }}</h2>
          <div style="display:flex;gap:8px;align-items:center">
            <el-tag type="danger" size="default" effect="light">得分 {{ detail.wrong.score }}/10</el-tag>
            <el-button type="success" :loading="mastering" @click="markMastered">
              <el-icon><CircleCheckFilled /></el-icon>已掌握
            </el-button>
          </div>
        </div>

        <el-divider />

        <h3 class="sec-title">AI 出的题目</h3>
        <div class="content-box q-box">{{ detail.wrong.questionText }}</div>

        <h3 class="sec-title">你的回答</h3>
        <div class="content-box a-box">{{ detail.wrong.userAnswer || '（未作答）' }}</div>

        <h3 class="sec-title">AI 点评</h3>
        <div class="content-box f-box">{{ detail.wrong.feedback || '暂无点评' }}</div>

        <!-- 同类错题 -->
        <template v-if="detail.similarWrongs?.length">
          <el-divider />
          <h3 class="sec-title">同类错题记录（{{ detail.similarWrongs.length }}次）</h3>
          <div v-for="sw in detail.similarWrongs" :key="sw.id" class="similar-item">
            <div class="sim-q">{{ sw.questionText?.slice(0, 100) }}{{ sw.questionText?.length > 100 ? '...' : '' }}</div>
            <div class="sim-meta">
              <el-tag :type="sw.score >= 8 ? 'success' : sw.score >= 6 ? 'warning' : 'danger'" size="small" effect="light">
                {{ sw.score }}分
              </el-tag>
              <span class="sim-date">{{ sw.createdAt?.slice(0, 10) }}</span>
            </div>
          </div>
        </template>

        <!-- 推荐题库 -->
        <template v-if="detail.relatedQuestions?.length">
          <el-divider />
          <h3 class="sec-title">相关题库推荐</h3>
          <div v-for="q in detail.relatedQuestions" :key="q.id" class="rel-item"
            @click="router.push(`/questions/${q.id}`)">
            <div class="rel-title">{{ q.title }}</div>
            <el-tag v-for="t in (()=>{try{return JSON.parse(q.tags||'[]')}catch{return[]}})()" :key="t" size="small" effect="plain" style="margin-right:4px">{{ t }}</el-tag>
          </div>
        </template>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.detail-header { display: flex; align-items: center; justify-content: space-between; }
.detail-header h2 { font-size: 18px; }
.sec-title { font-size: 14px; font-weight: 600; margin: 16px 0 8px; color: var(--text-primary); }
.content-box { padding: 14px 16px; border-radius: var(--radius-sm); font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.q-box { background: var(--color-primary-bg); border: 1px solid #C7D2FE; }
.a-box { background: #FEF2F2; border: 1px solid #FECACA; }
.f-box { background: #F0FDF4; border: 1px solid #BBF7D0; }
.similar-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.sim-q { font-size: 13px; color: var(--text-secondary); max-width: 70%; }
.sim-meta { display: flex; align-items: center; gap: 8px; }
.sim-date { font-size: 11px; color: var(--text-muted); }
.rel-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--border-light); cursor: pointer; border-radius: 4px; }
.rel-item:hover { background: var(--bg-input); }
.rel-title { font-size: 14px; font-weight: 500; flex: 1; }
</style>
