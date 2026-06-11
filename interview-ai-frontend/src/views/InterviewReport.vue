<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { interviewApi, type InterviewReport } from '@/api/interview'

const route = useRoute()
const router = useRouter()
const sessionId = Number(route.params.sessionId)
const loading = ref(true)
const report = ref<InterviewReport | null>(null)

onMounted(async () => {
  try {
    const res = await interviewApi.getReport(sessionId)
    report.value = res.data.data
  } catch { report.value = null }
  finally { loading.value = false }
})

const s = computed(() => report.value?.session)

const radarParsed = computed(() => {
  if (!s.value?.radarData) return null
  try { return JSON.parse(s.value.radarData) } catch { return null }
})

const messages = computed(() => report.value?.messages || [])

const dims = [
  { key: 'javaBasic', label: 'Java基础', color: '#4F46E5' },
  { key: 'framework', label: '框架应用', color: '#0D9488' },
  { key: 'database', label: '数据库', color: '#D97706' },
  { key: 'middleware', label: '中间件', color: '#DC2626' },
  { key: 'project', label: '项目能力', color: '#7C3AED' },
  { key: 'comprehensive', label: '综合素质', color: '#2563EB' },
]

function parseJson(s: string | null): any {
  if (!s) return null
  try { return JSON.parse(s) } catch { return null }
}
</script>

<template>
  <div class="page-container" style="max-width:900px">
    <div v-loading="loading">
      <div v-if="!loading && !s" class="empty-state">
        <el-icon :size="48" color="#CBD5E1"><DataAnalysis /></el-icon>
        <p>报告生成中...</p>
        <el-button style="margin-top:12px" @click="router.push('/dashboard')">返回</el-button>
      </div>

      <template v-if="s">
        <div class="page-header"><h2>面试报告</h2><p>{{ s.mode==='RESUME_DEEP'?'简历深挖':s.mode==='TECH_SPECIAL'?'技术专项':'混合模式' }} · {{ s.duration }}秒</p></div>

        <div class="score-hero">
          <div class="score-main"><div class="big-score">{{ s.totalScore || '--' }}</div><div class="score-lbl">综合评分</div></div>
          <div class="score-divider" />
          <div class="score-summary">
            <div class="si"><span class="sn">{{ s.answeredCount }}</span><span class="sl">回答题数</span></div>
            <div class="si"><span class="sn">{{ s.totalQuestions }}</span><span class="sl">总题数</span></div>
          </div>
        </div>

        <!-- Radar -->
        <el-card style="margin-bottom:20px" v-if="radarParsed"><template #header>能力雷达图</template>
          <div class="radar-bars">
            <div v-for="d in dims" :key="d.key" class="rb-item">
              <div class="rb-label">{{ d.label }}</div>
              <div class="rb-track"><div class="rb-fill" :style="{width:(radarParsed[d.key]||0)+'%',background:d.color}"/></div>
              <span class="rb-val">{{ radarParsed[d.key] || 0 }}</span>
            </div>
          </div>
        </el-card>

        <!-- Q&A -->
        <el-card><template #header>问答记录</template>
          <div v-for="(m,i) in messages" :key="m.id" v-show="m.role==='AI' || messages[i+1]?.role==='USER'" class="qa-item">
            <div v-if="m.role==='AI'" class="qa-q"><span class="qa-num">Q{{ Math.ceil((i+1)/2) }}</span>{{ m.content }}</div>
          </div>
        </el-card>

        <div style="text-align:center;margin-top:24px;padding-bottom:40px">
          <el-button type="primary" @click="router.push('/interview/start')">再来一轮</el-button>
          <el-button @click="router.push('/dashboard')">返回仪表盘</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.empty-state{text-align:center;padding:80px 0}
.empty-state p{margin-top:12px;font-size:14px;color:var(--text-secondary)}
.score-hero{display:flex;align-items:center;gap:32px;background:linear-gradient(135deg,#4F46E5,#6366F1);border-radius:var(--radius-lg);padding:32px 40px;margin-bottom:20px;color:white}
.big-score{font-size:56px;font-weight:800;line-height:1}
.score-lbl{font-size:14px;opacity:.8;margin-top:4px}
.score-divider{width:1px;height:60px;background:rgba(255,255,255,.2)}
.score-summary{display:flex;gap:32px}
.si{text-align:center}
.sn{font-size:22px;font-weight:700}
.sl{font-size:12px;opacity:.7;display:block;margin-top:2px}
.radar-bars{padding:8px 0}
.rb-item{display:flex;align-items:center;gap:12px;margin-bottom:10px}
.rb-label{width:80px;font-size:13px;text-align:right;color:var(--text-secondary)}
.rb-track{flex:1;height:10px;background:#F1F5F9;border-radius:5px;overflow:hidden}
.rb-fill{height:100%;border-radius:5px;transition:width .5s}
.rb-val{width:30px;font-size:14px;font-weight:700;color:var(--text-primary)}
.qa-item{padding:12px 0;border-bottom:1px solid var(--border-light)}
.qa-q{font-size:14px;font-weight:500}
.qa-num{color:var(--color-primary);font-weight:700;margin-right:8px}
</style>
