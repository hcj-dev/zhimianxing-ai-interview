<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resumeApi } from '@/api/resume'
import type { JdItem } from '@/types/resume'

const route = useRoute()
const resumeId = Number(route.params.id)
const jdText = ref('')
const submitting = ref(false)
const loadingResults = ref(true)
const jdResults = ref<JdItem[]>([])

onMounted(async () => {
  try {
    const res = await resumeApi.getJdMatchResults(resumeId)
    jdResults.value = res.data.data || []
  } catch {
    jdResults.value = []
  } finally {
    loadingResults.value = false
  }
})

async function startAnalysis() {
  if (!jdText.value.trim()) return
  submitting.value = true
  try {
    await resumeApi.submitJdMatch(resumeId, jdText.value)
    ElMessage.success('JD匹配任务已提交，AI 正在分析中，请稍后刷新查看结果')
    jdText.value = ''
    // 刷新结果列表
    const res = await resumeApi.getJdMatchResults(resumeId)
    jdResults.value = res.data.data || []
  } catch {
    // 错误已由拦截器处理
  } finally {
    submitting.value = false
  }
}

function parseSkillGap(gap: string | null): any[] {
  if (!gap) return []
  try { return JSON.parse(gap) } catch { return [] }
}

function parseSuggestions(s: string | null): string[] {
  if (!s) return []
  try { return JSON.parse(s) } catch { return [] }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>JD 匹配分析</h2>
        <p>粘贴目标岗位描述，AI 将分析你的简历与岗位的差距</p>
      </div>
    </div>

    <!-- Submit Form -->
    <el-card style="margin-bottom: 20px;">
      <el-input
        v-model="jdText" type="textarea" :rows="8"
        placeholder="在此粘贴目标岗位的 JD（职位描述）..."
      />
      <div style="margin-top: 16px; text-align: right;">
        <el-button type="primary" :loading="submitting" :disabled="!jdText.trim()"
          @click="startAnalysis">
          {{ submitting ? '提交中...' : '开始匹配分析' }}
        </el-button>
      </div>
    </el-card>

    <!-- Results List -->
    <h3 class="section-label" v-if="jdResults.length > 0">历史分析记录</h3>
    <div v-for="jd in jdResults" :key="jd.id">
      <el-card style="margin-bottom: 16px;" v-loading="loadingResults">
        <template #header>
          <div class="card-header-row">
            <span>分析结果 · {{ jd.createdAt }}</span>
            <el-tag v-if="jd.status === 'COMPLETED' && jd.matchScore != null"
              :type="jd.matchScore >= 70 ? 'success' : jd.matchScore >= 50 ? 'warning' : 'danger'"
              size="default" effect="light">
              匹配度 {{ jd.matchScore }}%
            </el-tag>
            <el-tag v-else-if="jd.status === 'PROCESSING'" type="warning" effect="light">分析中</el-tag>
            <el-tag v-else type="danger" effect="light">失败</el-tag>
          </div>
        </template>

        <template v-if="jd.status === 'COMPLETED'">
          <h4 class="section-label">🔍 技能差距</h4>
          <el-table :data="parseSkillGap(jd.skillGap)" style="width: 100%" size="small">
            <el-table-column prop="skill" label="技能" width="180" />
            <el-table-column prop="required" label="岗位要求" width="180">
              <template #default="{ row }">
                <el-tag type="danger" size="small" effect="light">{{ row.required }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="current" label="你的水平">
              <template #default="{ row }">
                <el-tag type="warning" size="small" effect="light">{{ row.current }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <h4 class="section-label" style="margin-top: 16px;">💡 改进建议</h4>
          <ul class="suggestion-list">
            <li v-for="s in parseSuggestions(jd.suggestions)" :key="s">{{ s }}</li>
          </ul>
        </template>
      </el-card>
    </div>

    <div v-if="!loadingResults && jdResults.length === 0" class="empty-state">
      <el-icon :size="48" color="#CBD5E1"><Connection /></el-icon>
      <p>还没有JD匹配记录</p>
      <p class="sub">在上方粘贴目标岗位JD，开始分析</p>
    </div>
  </div>
</template>

<style scoped>
.card-header-row { display: flex; align-items: center; justify-content: space-between; }
.section-label { font-size: 13px; font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
.suggestion-list { padding-left: 18px; }
.suggestion-list li { font-size: 13px; color: var(--text-secondary); margin-bottom: 6px; line-height: 1.6; }
.empty-state { text-align: center; padding: 60px 0; }
.empty-state p { margin-top: 12px; font-size: 14px; color: var(--text-secondary); }
.empty-state .sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
</style>
