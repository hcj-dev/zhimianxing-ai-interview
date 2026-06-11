<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { dashboardApi, type DashboardStats, type RadarData, type TrendData } from '@/api/dashboard'
import { questionApi } from '@/api/question'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()
const questionCount = ref<number | null>(null)
const stats = ref<DashboardStats>({ totalInterviews: 0, avgScore: 0, maxScore: 0, streakDays: 0 })
const radar = ref<RadarData>({ hasData: false })
const trend = ref<TrendData>({ points: [] })

const radarChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()

onMounted(async () => {
  try { const r = await questionApi.list({ pageSize: 1 }); questionCount.value = r.data.data.total } catch { /* */ }
  try { const r = await dashboardApi.getStats(); stats.value = r.data.data } catch { /* */ }
  try { const r = await dashboardApi.getRadar(); radar.value = r.data.data } catch { /* */ }
  try { const r = await dashboardApi.getTrend(30); trend.value = r.data.data } catch { /* */ }
})

watch([radar, trend, () => radarChart.value, () => trendChart.value], () => {
  nextTick(() => {
    if (radarChart.value && radar.value.hasData && radar.value.radarData) {
      const data = JSON.parse(radar.value.radarData)
      const chart = echarts.init(radarChart.value)
      chart.setOption({
        radar: {
          indicator: [
            { name: 'Java基础', max: 100 }, { name: '框架', max: 100 },
            { name: '数据库', max: 100 }, { name: '中间件', max: 100 },
            { name: '项目', max: 100 }, { name: '综合', max: 100 },
          ],
          center: ['50%', '55%'], radius: '70%',
          axisName: { fontSize: 10, color: '#64748B' },
        },
        series: [{
          type: 'radar',
          data: [{ value: [data.javaBasic||0, data.framework||0, data.database||0, data.middleware||0, data.project||0, data.comprehensive||0],
            name: '能力值', areaStyle: { color: 'rgba(79,70,229,0.2)' },
            lineStyle: { color: '#4F46E5' }, itemStyle: { color: '#4F46E5' } }],
        }],
      }, true)
    }

    if (trendChart.value && trend.value.points?.length > 0) {
      const chart = echarts.init(trendChart.value)
      chart.setOption({
        grid: { left: 40, right: 20, top: 20, bottom: 30 },
        xAxis: { type: 'category', data: trend.value.points.map(p => p.date.slice(5)), axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', min: 0, max: 100, axisLabel: { fontSize: 10 } },
        series: [{ type: 'line', data: trend.value.points.map(p => p.score),
          smooth: true, lineStyle: { color: '#4F46E5' }, areaStyle: { color: 'rgba(79,70,229,0.1)' },
          itemStyle: { color: '#4F46E5' } }],
      }, true)
    }
  })
})

const quickActions = [
  { title: '上传简历', desc: '上传PDF简历，获取AI分析', icon: 'Upload', color: '#4F46E5', path: '/resumes/upload' },
  { title: '模拟面试', desc: '与AI面试官实时对练', icon: 'VideoCamera', color: '#6366F1', path: '/interview/start' },
  { title: '浏览题库', desc: '按知识点标签筛选面试题', icon: 'Collection', color: '#D97706', path: '/questions' },
  { title: '查看错题', desc: '复习薄弱知识点', icon: 'WarningFilled', color: '#DC2626', path: '/questions/wrong' },
]
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>欢迎回来，{{ userStore.profile?.nickname || '用户' }} </h2><p>持续练习，每一步都在接近目标 offer</p></div>

    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6"><div class="stat-card">
        <div class="stat-icon" :style="{background:'#4F46E5'+'15',color:'#4F46E5'}"><el-icon :size="22"><TrendCharts /></el-icon></div>
        <div class="stat-info"><div class="stat-val">{{ stats.totalInterviews }}</div><div class="stat-lbl">总面试次数</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card">
        <div class="stat-icon" :style="{background:'#6366F1'+'15',color:'#6366F1'}"><el-icon :size="22"><Star /></el-icon></div>
        <div class="stat-info"><div class="stat-val">{{ stats.avgScore || '--' }}</div><div class="stat-lbl">平均得分</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card">
        <div class="stat-icon" :style="{background:'#D97706'+'15',color:'#D97706'}"><el-icon :size="22"><Trophy /></el-icon></div>
        <div class="stat-info"><div class="stat-val">{{ stats.maxScore || '--' }}</div><div class="stat-lbl">最高得分</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card">
        <div class="stat-icon" :style="{background:'#DC2626'+'15',color:'#DC2626'}"><el-icon :size="22"><Collection /></el-icon></div>
        <div class="stat-info"><div class="stat-val">{{ questionCount ?? '--' }}</div><div class="stat-lbl">题库总量</div></div>
      </div></el-col>
    </el-row>

    <h3 class="sec-title">快捷入口</h3>
    <el-row :gutter="16" style="margin-bottom:24px">
      <el-col :span="6" v-for="a in quickActions" :key="a.title">
        <div class="act-card" @click="router.push(a.path)">
          <div class="act-icon" :style="{background:a.color}"><el-icon :size="24" color="#fff"><component :is="a.icon"/></el-icon></div>
          <div class="act-title">{{ a.title }}</div><div class="act-desc">{{ a.desc }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card><template #header><span>进步趋势</span></template>
          <div v-if="trend.points.length > 0" ref="trendChart" style="height:260px"></div>
          <div v-else class="empty-sm"><el-icon :size="40" color="#CBD5E1"><TrendCharts /></el-icon><p>完成面试后生成趋势图</p></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card><template #header><span>能力雷达图</span></template>
          <div v-if="radar.hasData" ref="radarChart" style="height:260px"></div>
          <div v-else class="empty-sm"><el-icon :size="40" color="#CBD5E1"><PieChart /></el-icon><p>完成面试后生成雷达图</p></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.sec-title{font-size:16px;font-weight:600;margin-bottom:12px;color:var(--text-primary)}
.stat-card{display:flex;align-items:center;gap:14px}
.stat-icon{width:48px;height:48px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;flex-shrink:0}
.stat-val{font-size:24px;font-weight:700;color:var(--text-primary);line-height:1.2}
.stat-lbl{font-size:13px;color:var(--text-secondary);margin-top:2px}
.act-card{background:var(--bg-card);border:1px solid var(--border-color);border-radius:var(--radius-md);padding:20px;cursor:pointer;transition:all var(--transition-fast);box-shadow:var(--shadow-sm)}
.act-card:hover{transform:translateY(-2px);box-shadow:var(--shadow-md);border-color:var(--color-primary-light)}
.act-icon{width:44px;height:44px;border-radius:var(--radius-sm);display:flex;align-items:center;justify-content:center;margin-bottom:12px}
.act-title{font-size:14px;font-weight:600;color:var(--text-primary);margin-bottom:4px}
.act-desc{font-size:12px;color:var(--text-secondary);line-height:1.4}
.empty-sm{text-align:center;padding:50px 0}
.empty-sm p{margin-top:10px;font-size:13px;color:var(--text-secondary)}
</style>
