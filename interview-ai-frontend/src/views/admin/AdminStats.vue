<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { adminApi, type PlatformStats, type TrendPoint, type TopUser, type PopularTag } from '@/api/admin'

const stats = ref<PlatformStats | null>(null)
const trendPoints = ref<TrendPoint[]>([])
const topUsers = ref<TopUser[]>([])
const popularTags = ref<PopularTag[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      adminApi.getStats(),
      adminApi.getTrend(30),
      adminApi.getTopUsers(5),
      adminApi.getPopularTags(7),
    ])
    stats.value = r1.data.data
    trendPoints.value = r2.data.data.points || []
    topUsers.value = r3.data.data.users || []
    popularTags.value = r4.data.data.tags || []
  } catch (e: any) {
    error.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
})

// ===== KPI 卡片 =====
const kpiCards = computed(() => [
  { label: '总用户数', value: stats.value?.totalUsers ?? '--', icon: 'UserFilled', color: '#4F46E5', delta: '↑ 12%', deltaUp: true },
  { label: '总简历数', value: stats.value?.totalResumes ?? '--', icon: 'Document', color: '#0D9488', delta: '↑ 8%', deltaUp: true },
  { label: '总面试数', value: stats.value?.totalInterviews ?? '--', icon: 'VideoCamera', color: '#D97706', delta: '↑ 15%', deltaUp: true },
  { label: '题库总量', value: stats.value?.totalQuestions ?? '--', icon: 'Collection', color: '#DC2626', delta: '↑ 3%', deltaUp: true },
  { label: '今日面试', value: stats.value?.todayInterviews ?? '--', icon: 'TrendCharts', color: '#7C3AED', delta: '↑ 22%', deltaUp: true },
  { label: '平台均分', value: stats.value?.platformAvgScore ?? '--', icon: 'Star', color: '#2563EB', delta: '↓ 2%', deltaUp: false },
])

// ===== 趋势图 SVG 计算 =====
const trendSvg = computed(() => {
  const pts = trendPoints.value
  if (pts.length === 0) return null
  const w = 700, h = 195, padL = 45, padR = 18, padT = 20, padB = 30
  const chartW = w - padL - padR
  const chartH = h - padT - padB
  const maxVal = Math.max(...pts.map(p => Math.max(p.interviews, p.newUsers)), 1)
  const dataRange = chartH * 0.95
  const y = (v: number) => padT + chartH - (v / maxVal * dataRange)
  const x = (i: number) => padL + (i / Math.max(pts.length - 1, 1)) * chartW

  // Interview line
  const intPts = pts.map((p, i) => `${x(i)},${y(p.interviews)}`).join(' ')
  // New user line
  const usrPts = pts.map((p, i) => `${x(i)},${y(p.newUsers)}`).join(' ')
  // Area polygon
  const areaPts = pts.map((p, i) => `${x(i)},${y(p.interviews)}`).join(' ') +
    ` ${x(pts.length - 1)},${padT + chartH} ${x(0)},${padT + chartH}`

  // Grid lines (use dataRange for consistent Y extents)
  const gridLines = [0, 0.25, 0.5, 0.75, 1].map(f => {
    const cy = padT + chartH - f * dataRange
    return `<line x1="${padL}" y1="${cy}" x2="${w - padR}" y2="${cy}" stroke="#F1F5F9" stroke-width="1"/>`
  }).join('')

  return {
    areaPts, intPts, usrPts, gridLines,
    lastX: x(pts.length - 1),
    lastY: y(pts[pts.length - 1].interviews),
    lastUsrY: y(pts[pts.length - 1].newUsers),
    firstDate: pts[0].date.slice(5),
    lastDate: pts[pts.length - 1].date.slice(5),
  }
})

// ===== 模式分布 =====
const modeDistribution = computed(() => {
  const dist = stats.value?.modeDistribution || {}
  const total = Object.values(dist).reduce((s, v) => s + v, 0) || 1
  return [
    { name: '简历深挖', key: 'RESUME_DEEP', color: '#4F46E5', pct: Math.round((dist['RESUME_DEEP'] || 0) / total * 100) },
    { name: '技术专项', key: 'TECH_SPECIAL', color: '#10B981', pct: Math.round((dist['TECH_SPECIAL'] || 0) / total * 100) },
    { name: '混合模式', key: 'MIXED', color: '#D97706', pct: Math.round((dist['MIXED'] || 0) / total * 100) },
  ]
})

const modeDonut = computed(() => {
  const items = modeDistribution.value
  const circumference = 2 * Math.PI * 42 // r=42
  let offset = 0
  return items.map(item => {
    const dashLen = (item.pct / 100) * circumference
    const dashOffset = -offset
    offset += dashLen
    return { ...item, dashLen, dashOffset }
  })
})

// ===== 难度分布 =====
const difficultyBars = computed(() => {
  const dist = stats.value?.difficultyDistribution || {}
  const maxVal = Math.max(...Object.values(dist).map(v => v as number), 1)
  return [
    { name: '简单', key: 'EASY', color: '#10B981', pct: Math.round((dist['EASY'] || 0) / maxVal * 100), rawCount: dist['EASY'] || 0 },
    { name: '中等', key: 'MEDIUM', color: '#6366F1', pct: Math.round((dist['MEDIUM'] || 0) / maxVal * 100), rawCount: dist['MEDIUM'] || 0 },
    { name: '困难', key: 'HARD', color: '#D97706', pct: Math.round((dist['HARD'] || 0) / maxVal * 100), rawCount: dist['HARD'] || 0 },
    { name: '炼狱', key: 'HELL', color: '#DC2626', pct: Math.round((dist['HELL'] || 0) / maxVal * 100), rawCount: dist['HELL'] || 0 },
  ]
})

// ===== 完成率 =====
const completionDonut = computed(() => {
  const rate = stats.value?.completionRate || 0
  const circumference = 2 * Math.PI * 38
  const dashLen = (rate / 100) * circumference
  return { rate, dashLen, circumference }
})

// ===== 分数分布 =====
const scoreBars = computed(() => {
  const dist = stats.value?.scoreDistribution || {}
  const total = Object.values(dist).reduce((s, v) => s + v, 0) || 1
  return [
    { label: '0-20', key: 'r0_20', color: '#EF4444', pct: Math.round((dist['r0_20'] || 0) / total * 100) },
    { label: '20-40', key: 'r20_40', color: '#F59E0B', pct: Math.round((dist['r20_40'] || 0) / total * 100) },
    { label: '40-60', key: 'r40_60', color: '#3B82F6', pct: Math.round((dist['r40_60'] || 0) / total * 100) },
    { label: '60-80', key: 'r60_80', color: '#6366F1', pct: Math.round((dist['r60_80'] || 0) / total * 100) },
    { label: '80-100', key: 'r80_100', color: '#10B981', pct: Math.round((dist['r80_100'] || 0) / total * 100) },
  ]
})

// ===== 辅助 =====
const topUserStyle = (idx: number) => {
  if (idx === 0) return { bg: 'var(--color-primary-bg, #EEF2FF)', badgeColor: 'var(--color-primary, #4F46E5)', badgeBg: 'linear-gradient(135deg,var(--color-primary),#7C3AED)' }
  if (idx === 1) return { bg: 'var(--color-accent-bg, #F0FDFA)', badgeColor: 'var(--color-accent, #0D9488)', badgeBg: 'linear-gradient(135deg,var(--color-accent),#14B8A6)' }
  if (idx === 2) return { bg: 'transparent', badgeColor: '#D97706', badgeBg: 'linear-gradient(135deg,#D97706,#F59E0B)' }
  return { bg: 'transparent', badgeColor: 'var(--text-muted, #94A3B8)', badgeBg: '#E2E8F0' }
}

const tagBarColor = (idx: number) => {
  const colors = ['#4F46E5', '#6366F1', '#0D9488', '#14B8A6', '#D97706', '#F59E0B', '#7C3AED']
  return colors[idx % colors.length]
}
</script>

<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-header">
      <h2>平台数据概览</h2>
      <p>全平台运营数据统计 · 实时更新</p>
    </div>

    <div v-if="error" class="err-banner">{{ error }}</div>

    <div v-loading="loading">
      <!-- ===== 6 KPI 卡片 ===== -->
      <div class="kpi-row">
        <div v-for="c in kpiCards" :key="c.label" class="stat-card">
          <div class="stat-ico" :style="{ background: c.color }">
            <el-icon :size="18"><component :is="c.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-v">{{ c.value }}</div>
            <div class="stat-l">{{ c.label }}</div>
          </div>
          <span class="stat-delta" :class="c.deltaUp ? 'up' : 'down'">{{ c.delta }}</span>
        </div>
      </div>

      <!-- ===== 第1行：三列网格 — 趋势(跨2列) + 模式环形 ===== -->
      <div class="row-top-s4">
        <!-- 30天趋势 (跨2列) -->
        <div class="panel span2">
          <h4>近30天面试趋势</h4>
          <svg v-if="trendSvg" viewBox="0 0 700 195" class="chart-svg">
            <defs>
              <linearGradient id="tarea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#4F46E5" stop-opacity=".15"/>
                <stop offset="100%" stop-color="#4F46E5" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <!-- Grid -->
            <g v-html="trendSvg.gridLines"></g>
            <!-- Area -->
            <polygon :points="trendSvg.areaPts" fill="url(#tarea)"/>
            <!-- Interview line -->
            <polyline :points="trendSvg.intPts" fill="none" stroke="#4F46E5" stroke-width="2.5" stroke-linecap="round"/>
            <circle :cx="trendSvg.lastX" :cy="trendSvg.lastY" r="4" fill="#4F46E5"/>
            <!-- New user dashed line -->
            <polyline :points="trendSvg.usrPts" fill="none" stroke="#10B981" stroke-width="1.8" stroke-linecap="round" stroke-dasharray="6,4"/>
            <circle :cx="trendSvg.lastX" :cy="trendSvg.lastUsrY" r="3" fill="#10B981"/>
            <!-- Legend -->
            <rect x="530" y="8" width="10" height="10" rx="2" fill="#4F46E5"/>
            <text x="544" y="17" font-size="10" fill="#64748B">面试数</text>
            <rect x="590" y="8" width="10" height="10" rx="2" fill="#10B981"/>
            <text x="604" y="17" font-size="10" fill="#64748B">新增用户</text>
            <!-- X labels -->
            <text :x="45" y="185" font-size="9" fill="#94A3B8">{{ trendSvg.firstDate }}</text>
            <text :x="682" y="185" font-size="9" fill="#94A3B8" text-anchor="end">{{ trendSvg.lastDate }}</text>
          </svg>
          <div v-else class="empty">暂无趋势数据</div>
        </div>

        <!-- 模式环形 -->
        <div class="panel">
          <h4>面试模式分布</h4>
          <div class="donut-wrap">
            <svg viewBox="0 0 110 110" width="110" height="110">
              <circle cx="55" cy="55" r="42" fill="none" stroke="#F1F5F9" stroke-width="10"/>
              <circle
                v-for="seg in modeDonut" :key="seg.key"
                cx="55" cy="55" r="42" fill="none"
                :stroke="seg.color" stroke-width="10"
                :stroke-dasharray="`${seg.dashLen} ${2*Math.PI*42}`"
                :stroke-dashoffset="seg.dashOffset"
                stroke-linecap="round"
              />
            </svg>
            <div class="donut-center">
              <div class="donut-num">{{ stats?.totalInterviews ?? '--' }}</div>
              <div class="donut-lbl">总面试</div>
            </div>
          </div>
          <div class="donut-legend">
            <div v-for="m in modeDistribution" :key="m.key" class="legend-item">
              <span class="legend-dot" :style="{ background: m.color }"></span>
              <span>{{ m.name }} {{ m.pct }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 第2行：三列网格 — 难度 + Top5 + 标签 ===== -->
      <div class="row-mid-s4">
        <!-- 难度柱状 -->
        <div class="panel diff-panel-s4">
          <h4>难度等级分布</h4>
          <div class="diff-bars-s4">
            <div v-for="d in difficultyBars" :key="d.key" class="diff-bar-col">
              <div class="diff-pct">{{ d.rawCount }}</div>
              <div
                class="diff-bar"
                :style="{ height: Math.max(d.pct * 0.9, 8) + 'px', background: `linear-gradient(180deg,${d.color},${d.color}88)` }"
              ></div>
              <div class="diff-label">{{ d.name }}</div>
            </div>
          </div>
        </div>

        <!-- 活跃用户 -->
        <div class="panel">
          <h4>活跃用户 Top 5</h4>
          <div class="top-users">
            <div
              v-for="(u, idx) in topUsers" :key="u.userId"
              class="top-user-row"
              :style="{ background: topUserStyle(idx).bg }"
            >
              <span class="tu-rank" :style="{ color: topUserStyle(idx).badgeColor }">{{ idx + 1 }}</span>
              <div class="tu-avatar" :style="{ background: topUserStyle(idx).badgeBg }">
                {{ u.nickname?.charAt(0) || '?' }}
              </div>
              <span class="tu-name">{{ u.nickname }}</span>
              <span class="tu-count">{{ u.interviewCount }}次</span>
            </div>
            <div v-if="topUsers.length === 0" class="empty">暂无数据</div>
          </div>
        </div>

        <!-- 热门标签 -->
        <div class="panel">
          <h4>热门技术方向</h4>
          <div class="tag-bars">
            <div v-for="(t, idx) in popularTags" :key="t.name" class="tag-bar-row">
              <span class="tb-label">{{ t.name }}</span>
              <div class="tb-track">
                <div
                  class="tb-fill"
                  :style="{ width: t.percentage + '%', background: tagBarColor(idx) }"
                ></div>
              </div>
              <span class="tb-pct">{{ t.percentage }}%</span>
            </div>
            <div v-if="popularTags.length === 0" class="empty">暂无数据</div>
          </div>
        </div>
      </div>

      <!-- ===== 第3行：两列网格 — 完成率 + 分数分布 ===== -->
      <div class="row-bottom-s4">
        <!-- 完成率 -->
        <div class="panel">
          <h4>面试完成率</h4>
          <div class="completion-wrap">
            <svg viewBox="0 0 100 100" width="100" height="100">
              <circle cx="50" cy="50" r="38" fill="none" stroke="#F1F5F9" stroke-width="9"/>
              <circle
                cx="50" cy="50" r="38" fill="none" stroke="#10B981" stroke-width="9"
                :stroke-dasharray="`${completionDonut.dashLen} ${completionDonut.circumference}`"
                stroke-linecap="round"
                transform="rotate(-90 50 50)"
              />
            </svg>
            <div class="donut-center" style="position:absolute;inset:0">
              <div class="donut-num" style="color:#10B981;font-size:20px">{{ completionDonut.rate }}%</div>
              <div class="donut-lbl">完成率</div>
            </div>
          </div>
        </div>

        <!-- 分数分布 -->
        <div class="panel">
          <h4>分数分布直方图</h4>
          <div class="score-bars">
            <div v-for="s in scoreBars" :key="s.key" class="score-bar-col">
              <div class="score-pct">{{ s.pct }}%</div>
              <div
                class="score-bar"
                :style="{ height: Math.max(s.pct * 0.9, 6) + 'px', background: s.color }"
              ></div>
              <div class="score-label">{{ s.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 错误横幅 ===== */
.err-banner {
  text-align: center; padding: 16px; color: var(--color-danger);
  font-size: 14px; background: #FEF2F2; border-radius: var(--radius-sm);
  margin-bottom: 16px;
}

/* ===== KPI 行 ===== */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 18px 16px;
  box-shadow: var(--shadow-sm);
  display: flex; align-items: center; gap: 12px;
}
.stat-ico {
  width: 42px; height: 42px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-size: 18px; color: #fff;
}
.stat-body { flex: 1; min-width: 0; }
.stat-v {
  font-size: 22px; font-weight: 700;
  color: var(--text-primary); line-height: 1.2;
  font-family: "JetBrains Mono","Fira Code","Consolas",monospace;
}
.stat-l { font-size: 12px; color: var(--text-secondary); }
.stat-delta { font-size: 11px; font-weight: 600; flex-shrink: 0; }
.stat-delta.up { color: var(--color-success, #10B981); }
.stat-delta.down { color: var(--color-danger, #EF4444); }

/* ===== 面板通用 ===== */
.panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
}
.panel h4 {
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  margin-bottom: 14px; display: flex; align-items: center; gap: 6px;
}
.panel h4::before {
  content: ''; width: 3px; height: 13px;
  background: var(--color-primary); border-radius: 2px;
}

/* ===== 第1行：三列网格 — 趋势跨2列 + 模式 ===== */
.row-top-s4 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}
.span2 { grid-column: span 2; }

.chart-svg { width: 100%; display: block; }
.empty { text-align: center; padding: 40px 20px; font-size: 13px; color: var(--text-muted); }

/* Donut */
.donut-wrap { position: relative; width: 110px; height: 110px; margin: 0 auto 8px; }
.donut-wrap svg { transform: rotate(-90deg); }
.donut-center {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.donut-num { font-size: 22px; font-weight: 700; color: var(--text-primary); font-family: "JetBrains Mono","Consolas",monospace; }
.donut-lbl { font-size: 10px; color: var(--text-secondary); }
.donut-legend { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: var(--text-secondary); }
.legend-item { display: flex; align-items: center; gap: 6px; }
.legend-dot { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }

/* Difficulty panel — flex column, bars sink to bottom */
.diff-panel-s4 {
  display: flex;
  flex-direction: column;
}
.diff-panel-s4 h4 { flex-shrink: 0; }
.diff-bars-s4 {
  flex: 1;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding-top: 0;
}
.diff-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; }
.diff-pct { font-size: 12px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.diff-bar { width: 100%; border-radius: 4px 4px 0 0; opacity: .85; }
.diff-label { font-size: 11px; color: var(--text-secondary); margin-top: 6px; }

/* ===== 第2行：三列网格 — 难度 + Top5 + 标签 ===== */
.row-mid-s4 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}

/* Top users */
.top-users { display: flex; flex-direction: column; gap: 8px; }
.top-user-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border-radius: 6px;
}
.tu-rank { font-weight: 700; font-size: 14px; width: 20px; }
.tu-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.tu-name { font-size: 13px; font-weight: 600; flex: 1; }
.tu-count { font-size: 12px; color: var(--text-secondary); font-family: "JetBrains Mono","Consolas",monospace; }

/* Popular tags */
.tag-bars { display: flex; flex-direction: column; gap: 7px; }
.tag-bar-row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.tb-label { width: 70px; text-align: right; color: var(--text-secondary); flex-shrink: 0; }
.tb-track { flex: 1; height: 8px; background: #F1F5F9; border-radius: 4px; overflow: hidden; }
.tb-fill { height: 100%; border-radius: 4px; transition: width .4s ease; }
.tb-pct { width: 28px; color: var(--text-primary); font-weight: 600; font-family: "JetBrains Mono","Consolas",monospace; }

/* ===== 第3行：两列网格 — 完成率 + 分数分布 ===== */
.row-bottom-s4 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

/* Completion */
.completion-wrap {
  position: relative; width: 100px; height: 100px;
  margin: 0 auto; display: flex; align-items: center; justify-content: center;
}

/* Score histogram */
.score-bars { display: flex; gap: 5px; align-items: flex-end; height: 120px; }
.score-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; }
.score-pct { font-size: 10px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.score-bar { width: 100%; border-radius: 3px 3px 0 0; opacity: .85; min-height: 6px; }
.score-label { font-size: 10px; color: var(--text-muted); margin-top: 6px; }

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
  .row-top-s4 { grid-template-columns: 1fr 1fr; }
  .span2 { grid-column: span 1; }
  .row-mid-s4 { grid-template-columns: 1fr 1fr; }
  .row-bottom-s4 { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .row-top-s4 { grid-template-columns: 1fr; }
  .row-mid-s4 { grid-template-columns: 1fr; }
  .row-bottom-s4 { grid-template-columns: 1fr; }
}
</style>
