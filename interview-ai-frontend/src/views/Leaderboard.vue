<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { leaderboardApi, type RankEntry } from '@/api/leaderboard'

type Tab = 'resume' | 'interview' | 'comprehensive'
const currentTab = ref<Tab>('comprehensive')
const rankings = ref<RankEntry[]>([])
const loading = ref(true)

const tabs: { key: Tab; label: string; desc: string }[] = [
  { key: 'comprehensive', label: '综合排行', desc: '简历评分 + 面试平均分' },
  { key: 'resume', label: '简历评分', desc: '简历分析最高分排行' },
  { key: 'interview', label: '面试均分', desc: '面试平均分排行' },
]

async function loadRanking(tab: Tab) {
  loading.value = true
  try {
    const api =
      tab === 'resume' ? leaderboardApi.getResumeRanking() :
      tab === 'interview' ? leaderboardApi.getInterviewRanking() :
      leaderboardApi.getComprehensiveRanking()
    const r = await api
    rankings.value = r.data.data
  } catch {
    rankings.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => loadRanking('comprehensive'))

function switchTab(tab: Tab) {
  currentTab.value = tab
  loadRanking(tab)
}

// Podium layout: left=2nd, center=1st, right=3rd
// podiumOrder maps display position (0=left,1=center,2=right) → rankings index
const podiumOrder = [1, 0, 2]
// Actual rank for each podium position (2, 1, 3)
const podiumRanks = [2, 1, 3]

const medals = ['🥈', '🥇', '🥉']
const podiumColors = ['#C0C0C0', '#FFD700', '#CD7F32']
const podiumHeights = [100, 130, 60]

function podiumEntry(displayIndex: 0 | 1 | 2): RankEntry | undefined {
  const rankIdx = podiumOrder[displayIndex]
  return rankings.value[rankIdx]
}

// 构造头像 API 地址。无论用户是否上传头像，统一走 /api/v1/user/avatar/{userId}
// 未上传时后端返回 204，el-avatar 自动 fallback 到图标
function getAvatarSrc(userId: number): string {
  return `/api/v1/user/avatar/${userId}`
}

// Pad to always have 10 slots (real entries + placeholders)
const totalSlots = computed(() => Math.max(10, rankings.value.length))

function placeholderEntries(): number[] {
  const real = rankings.value.length
  if (real >= 10) return []
  return Array.from({ length: 10 - real }, (_, i) => real + i + 1)
}
</script>

<template>
  <div class="page-container" style="max-width:800px">
    <div class="page-header">
      <h2>排行榜</h2>
      <p>看看你在社区中的排名</p>
    </div>

    <!-- tabs -->
    <div class="tab-row">
      <div v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ active: currentTab === t.key }" @click="switchTab(t.key)">
        <div class="tab-label">{{ t.label }}</div>
        <div class="tab-desc">{{ t.desc }}</div>
      </div>
    </div>

    <el-card v-loading="loading" style="min-height: 400px;">
      <div v-if="!loading && rankings.length === 0" class="empty-state">
        <el-icon :size="48" color="#CBD5E1"><Trophy /></el-icon>
        <p>暂无排行数据</p>
        <p class="sub">去完成一次简历分析或模拟面试，登上排行榜</p>
      </div>

      <template v-if="rankings.length > 0">
        <!-- Podium Top 3 -->
        <div class="podium">
          <div v-for="di in 3" :key="di" class="podium-col" :class="'place-' + di">
            <template v-if="podiumEntry((di - 1) as 0 | 1 | 2)">
              <div class="podium-avatar" :style="{ borderColor: podiumColors[di - 1] }">
                <el-avatar :size="di === 2 ? 64 : 48" :src="getAvatarSrc(podiumEntry((di - 1) as 0 | 1 | 2)!.userId)" fit="cover">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </div>
              <div class="podium-medal">{{ medals[di - 1] }}</div>
              <div class="podium-name">{{ podiumEntry((di - 1) as 0 | 1 | 2)!.nickname || podiumEntry((di - 1) as 0 | 1 | 2)!.username }}</div>
              <!-- 综合排行：大号综合分 + 小字简历/面试分 -->
              <div v-if="currentTab === 'comprehensive' && podiumEntry((di - 1) as 0 | 1 | 2)!.resumeScore != null && podiumEntry((di - 1) as 0 | 1 | 2)!.interviewScore != null" class="podium-score comp-score">
                <div class="comp-main">{{ podiumEntry((di - 1) as 0 | 1 | 2)!.score }}<span class="comp-unit">分</span></div>
                <div class="comp-sub">简历 {{ podiumEntry((di - 1) as 0 | 1 | 2)!.resumeScore }} · 面试 {{ podiumEntry((di - 1) as 0 | 1 | 2)!.interviewScore }}</div>
              </div>
              <!-- 其他排行：统一大号分数 -->
              <div v-else class="podium-score comp-score">
                <div class="comp-main">{{ podiumEntry((di - 1) as 0 | 1 | 2)!.score }}<span class="comp-unit">分</span></div>
              </div>
              <div class="podium-block" :style="{ height: podiumHeights[di - 1] + 'px', background: podiumColors[di - 1] }">
                <span class="podium-rank">{{ podiumRanks[di - 1] }}</span>
              </div>
            </template>
            <!-- Empty podium slot -->
            <template v-else>
              <div class="podium-avatar" :style="{ borderColor: podiumColors[di - 1] }">
                <div class="empty-avatar" />
              </div>
              <div class="podium-medal">{{ medals[di - 1] }}</div>
              <div class="podium-name empty">虚位以待</div>
              <div class="podium-score empty-score">--</div>
              <div class="podium-block" :style="{ height: podiumHeights[di - 1] + 'px', background: podiumColors[di - 1] }">
                <span class="podium-rank">{{ podiumRanks[di - 1] }}</span>
              </div>
            </template>
          </div>
        </div>

        <!-- List 4-10 -->
        <div class="rank-list">
          <!-- Real entries (rank 4-10) -->
          <div v-for="(entry, idx) in rankings.slice(3, 10)" :key="entry.userId" class="rank-item">
            <span class="rank-num">{{ idx + 4 }}</span>
            <span class="rank-name">{{ entry.nickname || entry.username }}</span>
            <!-- 综合排行：大号综合分 + 小字简历/面试分 -->
            <span v-if="currentTab === 'comprehensive' && entry.resumeScore != null && entry.interviewScore != null" class="rank-score comp-score">
              <span class="comp-main">{{ entry.score }}<span class="comp-unit">分</span></span>
              <span class="comp-sub">简历 {{ entry.resumeScore }} · 面试 {{ entry.interviewScore }}</span>
            </span>
            <span v-else class="rank-score comp-score">
              <span class="comp-main">{{ entry.score }}<span class="comp-unit">分</span></span>
            </span>
          </div>
          <!-- Empty placeholders for missing ranks -->
          <div v-for="r in placeholderEntries()" :key="'empty-' + r" class="rank-item empty-row">
            <span class="rank-num">{{ r }}</span>
            <span class="rank-name empty">虚位以待</span>
            <span class="rank-score empty-score">--</span>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.tab-row { display: flex; gap: 10px; margin-bottom: 20px; }
.tab-btn { flex: 1; padding: 12px 16px; border-radius: var(--radius-md); border: 2px solid var(--border-color); cursor: pointer; text-align: center; transition: all var(--transition-fast); background: var(--bg-card); }
.tab-btn:hover { border-color: var(--color-primary-light); }
.tab-btn.active { border-color: var(--color-primary); background: var(--color-primary-bg); }
.tab-label { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.tab-desc { font-size: 11px; color: var(--text-muted); margin-top: 2px; }

.empty-state { text-align: center; padding: 80px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }
.empty-state .sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

/* Podium */
.podium { display: flex; align-items: flex-end; justify-content: center; gap: 16px; padding: 32px 0 0; }
.podium-col { display: flex; flex-direction: column; align-items: center; width: 150px; }
/* 天然 DOM 顺序：di=1(银) di=2(金) di=3(铜)，即 左→右 = 第二→第一→第三，无需 order */

.podium-avatar {
  border-radius: 50%;
  border: 3px solid;
  margin-bottom: 4px;
  display: inline-flex;
  line-height: 0;
  overflow: hidden;
}
.empty-avatar { width: 48px; height: 48px; }
.podium-col.place-2 .empty-avatar { width: 64px; height: 64px; }
.podium-medal { font-size: 24px; margin-bottom: 2px; }
.podium-name { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 2px; text-align: center; }
.podium-name.empty { color: var(--text-muted); font-weight: 400; }
.podium-score { font-size: 13px; font-weight: 700; color: var(--color-primary); margin-bottom: 6px; }
.empty-score { color: var(--text-muted) !important; font-weight: 400; }
.podium-unit { font-size: 11px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
.sub-scores { display: block; font-size: 11px; font-weight: 400; color: var(--text-muted); margin-top: 2px; }

/* 综合排行：大号综合分居中 + 小字简历/面试分 */
.comp-score { text-align: center; }
.comp-main { font-size: 18px; font-weight: 800; color: var(--color-primary); line-height: 1.2; }
.comp-unit { font-size: 12px; font-weight: 500; color: var(--text-secondary); margin-left: 2px; }
.comp-sub { font-size: 11px; font-weight: 400; color: var(--text-muted); margin-top: 2px; }
.rank-score.comp-score { display: flex; flex-direction: column; align-items: center; }
.rank-item .comp-main { font-size: 16px; }
.rank-item .comp-sub { font-size: 11px; }
.podium-block { width: 100%; border-radius: var(--radius-sm) var(--radius-sm) 0 0; display: flex; align-items: flex-start; justify-content: center; padding-top: 8px; transition: height 0.5s ease; }
.podium-rank { font-size: 32px; font-weight: 800; color: rgba(255,255,255,0.85); line-height: 1; }

/* List */
.rank-list { margin-top: 8px; padding: 0 20px; }
.rank-item { display: flex; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--border-light); border-radius: var(--radius-sm); transition: background var(--transition-fast); }
.rank-item:hover { background: var(--bg-input); }
.empty-row:hover { background: transparent; }
.rank-num { width: 30px; font-size: 14px; font-weight: 700; color: var(--text-muted); }
.rank-name { flex: 1; font-size: 14px; font-weight: 500; }
.rank-name.empty { color: var(--text-muted); font-weight: 400; font-style: italic; }
.rank-score { font-size: 14px; font-weight: 700; color: var(--color-primary); text-align: right; }
.rank-unit { font-size: 11px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
.rank-score .sub-scores { display: block; }
</style>
