<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resumeApi } from '@/api/resume'
import { interviewApi } from '@/api/interview'
import type { ResumeItem } from '@/types/resume'

const router = useRouter()
const resumes = ref<ResumeItem[]>([])
const loadingResumes = ref(true)
const selectedResumeId = ref<number | null>(null)
const selectedMode = ref('RESUME_DEEP')
const selectedDifficulty = ref('MEDIUM')
const selectedTags = ref<string[]>([])
const starting = ref(false)

onMounted(async () => {
  try { const r = await resumeApi.list(); resumes.value = r.data.data || [] }
  catch { resumes.value = [] }
  finally { loadingResumes.value = false }
})

const modes = [
  { value: 'RESUME_DEEP', label: '简历深挖', desc: '基于项目经历提问', letter: 'R', color: '#4F46E5' },
  { value: 'TECH_SPECIAL', label: '技术专项', desc: '选择方向定向出题', letter: 'T', color: '#10B981' },
  { value: 'MIXED', label: '混合模式', desc: '技术题 + 简历题交替', letter: 'M', color: '#D97706' },
]

const difficulties = [
  { value: 'EASY', label: '简单', sub: '入门基础', color: '#10B981' },
  { value: 'MEDIUM', label: '中等', sub: '常规面试', color: '#6366F1' },
  { value: 'HARD', label: '困难', sub: '底层原理', color: '#D97706' },
  { value: 'HELL', label: '炼狱', sub: '源码级别', color: '#DC2626' },
]

const techTags = [
  'Java 基础','Spring Boot','MyBatis','MySQL','Redis','并发编程','JVM',
  '微服务','消息队列','Docker & K8s','Linux','设计模式',
  'Vue.js','React','JavaScript','TypeScript','前端工程化',
  '算法与数据结构','系统设计','计算机网络','操作系统',
  '自动化测试','CI/CD','安全','Python','Go',
]

function toggleTag(tag: string) {
  const i = selectedTags.value.indexOf(tag)
  i > -1 ? selectedTags.value.splice(i, 1) : selectedTags.value.push(tag)
}

// ===== 右侧预览面板 computed =====
const selectedResumeName = computed(() => {
  const r = resumes.value.find(r => r.id === selectedResumeId.value)
  return r?.fileName || '未选择'
})

const selectedModeItem = computed(() => modes.find(m => m.value === selectedMode.value))
const selectedDifficultyItem = computed(() => difficulties.find(d => d.value === selectedDifficulty.value))

async function startInterview() {
  if (!selectedResumeId.value) { ElMessage.warning('请选择一份简历'); return }
  if (selectedMode.value === 'TECH_SPECIAL' && selectedTags.value.length === 0) {
    ElMessage.warning('技术专项模式请至少选择一个标签'); return
  }
  starting.value = true
  try {
    const res = await interviewApi.init({
      resumeId: selectedResumeId.value,
      mode: selectedMode.value,
      difficulty: selectedDifficulty.value,
      techTags: selectedMode.value === 'TECH_SPECIAL' ? selectedTags.value : [],
    })
    const sessionId = res.data.data.sessionId
    router.push(`/interview/${sessionId}`)
  } catch { starting.value = false }
}
</script>

<template>
  <div class="l1-page">
    <!-- ===== 左侧：配置区 ===== -->
    <div class="l1-config">
      <!-- 选择简历 -->
      <div class="l1-section">
        <div class="l1-section-title">选择简历</div>
        <div class="l1-resume-list" v-loading="loadingResumes">
          <template v-if="!loadingResumes && resumes.length === 0">
            <div class="empty-hint">
              <el-icon :size="18"><WarningFilled /></el-icon>
              <span>还没有简历，请先上传</span>
              <el-button size="small" type="primary" style="margin-left:12px" @click="router.push('/resumes/upload')">上传</el-button>
            </div>
          </template>
          <div
            v-for="r in resumes" :key="r.id"
            class="resume-option"
            :class="{ selected: selectedResumeId === r.id }"
            @click="selectedResumeId = r.id"
          >
            <div class="resume-radio"></div>
            <span>{{ r.fileName }}</span>
          </div>
        </div>
      </div>

      <!-- 面试模式 -->
      <div class="l1-section">
        <div class="l1-section-title">面试模式</div>
        <div class="l1-mode-grid">
          <div
            v-for="m in modes" :key="m.value"
            class="l1-mode-card"
            :class="{ active: selectedMode === m.value }"
            @click="selectedMode = m.value"
          >
            <div class="mode-icon" :style="{ background: m.color }">{{ m.letter }}</div>
            <div class="mode-name">{{ m.label }}</div>
            <div class="mode-desc">{{ m.desc }}</div>
          </div>
        </div>
      </div>

      <!-- 难度等级 -->
      <div class="l1-section">
        <div class="l1-section-title">难度等级</div>
        <div class="l1-diff-row">
          <div
            v-for="d in difficulties" :key="d.value"
            class="l1-diff-chip"
            :class="{ active: selectedDifficulty === d.value }"
            @click="selectedDifficulty = d.value"
          >
            <span :style="{ color: selectedDifficulty === d.value ? 'var(--color-primary)' : d.color }">{{ d.label }}</span>
            <span class="sub">{{ d.sub }}</span>
          </div>
        </div>
      </div>

      <!-- 技术方向 -->
      <div class="l1-section">
        <div class="l1-section-title">技术方向</div>
        <div class="l1-tags">
          <span
            v-for="t in techTags" :key="t"
            class="tag"
            :class="{ active: selectedTags.includes(t) }"
            @click="toggleTag(t)"
          >{{ t }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 右侧：预览面板 ===== -->
    <div class="l1-preview">
      <div class="l1-pv-title">配置摘要</div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">简历</div>
        <div class="l1-pv-value">{{ selectedResumeName }}</div>
      </div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">模式</div>
        <div class="l1-pv-value">{{ selectedModeItem?.label || '-' }}</div>
      </div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">难度</div>
        <div class="l1-pv-value" style="color:#818CF8;font-weight:700">
          {{ selectedDifficultyItem?.label || '-' }}
        </div>
      </div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">技术标签</div>
        <div class="l1-pv-tags">
          <template v-if="selectedTags.length > 0">
            <span v-for="t in selectedTags" :key="t" class="l1-pv-tag">{{ t }}</span>
          </template>
          <span v-else style="font-size:12px;opacity:.4">未选择</span>
        </div>
      </div>

      <div class="l1-pv-divider"></div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">题目数量</div>
        <div class="l1-pv-value">10 题</div>
      </div>

      <div class="l1-pv-item">
        <div class="l1-pv-label">预计时长</div>
        <div class="l1-pv-value">约 20-30 分钟</div>
      </div>

      <button class="l1-pv-start" :disabled="starting" @click="startInterview">
        {{ starting ? '创建中...' : '开始面试' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ===== 分栏布局 ===== */
.l1-page {
  display: flex;
  height: calc(100vh - 56px);
  background: var(--bg-page, #F1F5F9);
  overflow: hidden;
}
.l1-config {
  flex: 1;
  padding: 32px 40px;
  overflow-y: auto;
}
.l1-section {
  margin-bottom: 24px;
}
.l1-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary, #1E293B);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.l1-section-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: var(--color-primary, #4F46E5);
  border-radius: 2px;
  flex-shrink: 0;
}

/* 简历列表 */
.l1-resume-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #E2E8F0);
  border-radius: var(--radius-md, 12px);
  padding: 8px;
}
.empty-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  color: var(--text-secondary, #64748B);
  font-size: 13px;
}
.resume-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm, 8px);
  cursor: pointer;
  transition: all 0.15s;
  border: 1.5px solid transparent;
  font-size: 13px;
  color: var(--text-primary, #1E293B);
}
.resume-option:hover {
  background: var(--color-primary-bg, #EEF2FF);
}
.resume-option.selected {
  background: var(--color-primary-bg, #EEF2FF);
  border-color: var(--color-primary, #4F46E5);
  font-weight: 600;
}
.resume-radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--border-color, #E2E8F0);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.resume-option.selected .resume-radio {
  border-color: var(--color-primary, #4F46E5);
  background: var(--color-primary, #4F46E5);
}
.resume-option.selected .resume-radio::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
}

/* 模式卡片 */
.l1-mode-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.l1-mode-card {
  background: var(--bg-card, #fff);
  border: 2px solid var(--border-color, #E2E8F0);
  border-radius: var(--radius-md, 12px);
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}
.l1-mode-card:hover {
  border-color: var(--color-primary-light, #818CF8);
}
.l1-mode-card.active {
  border-color: var(--color-primary, #4F46E5);
  background: var(--color-primary-bg, #EEF2FF);
}
.l1-mode-card .mode-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
  font-size: 16px;
  color: #fff;
  font-weight: 700;
}
.l1-mode-card .mode-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text-primary, #1E293B);
}
.l1-mode-card .mode-desc {
  font-size: 11px;
  color: var(--text-secondary, #64748B);
}

/* 难度芯片 */
.l1-diff-row {
  display: flex;
  gap: 8px;
}
.l1-diff-chip {
  flex: 1;
  padding: 10px 0;
  text-align: center;
  border-radius: var(--radius-sm, 8px);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  border: 2px solid var(--border-color, #E2E8F0);
  transition: all 0.15s;
  background: var(--bg-card, #fff);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.l1-diff-chip:hover {
  border-color: var(--color-primary-light, #818CF8);
}
.l1-diff-chip.active {
  border-color: var(--color-primary, #4F46E5);
  background: var(--color-primary-bg, #EEF2FF);
}
.l1-diff-chip .sub {
  font-size: 10px;
  font-weight: 400;
  color: var(--text-muted, #94A3B8);
  display: block;
}

/* 标签 */
.l1-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid var(--border-color, #E2E8F0);
  color: var(--text-secondary, #64748B);
  transition: all 0.15s;
  user-select: none;
  background: var(--bg-card, #fff);
}
.tag:hover {
  border-color: var(--color-primary-light, #818CF8);
  color: var(--color-primary, #4F46E5);
}
.tag.active {
  background: var(--color-primary, #4F46E5);
  color: #fff;
  border-color: var(--color-primary, #4F46E5);
}

/* ===== 右侧预览面板 ===== */
.l1-preview {
  width: 320px;
  flex-shrink: 0;
  background: var(--sidebar-bg, #0F172A);
  color: #E2E8F0;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
}
.l1-pv-title {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: #94A3B8;
  margin-bottom: 24px;
  font-weight: 600;
}
.l1-pv-item {
  margin-bottom: 20px;
}
.l1-pv-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #94A3B8;
  margin-bottom: 4px;
}
.l1-pv-value {
  font-size: 14px;
  font-weight: 500;
  color: #F1F5F9;
}
.l1-pv-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.l1-pv-tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255,255,255,0.1);
  color: #CBD5E1;
}
.l1-pv-divider {
  height: 1px;
  background: rgba(255,255,255,0.08);
  margin: 20px 0;
}
.l1-pv-start {
  width: 100%;
  margin-top: auto;
  padding: 14px;
  border: none;
  background: var(--color-primary, #4F46E5);
  color: #fff;
  border-radius: var(--radius-sm, 8px);
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  font-family: inherit;
  transition: all 0.15s;
}
.l1-pv-start:hover:not(:disabled) {
  background: #4338CA;
}
.l1-pv-start:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 900px) {
  .l1-page { flex-direction: column; }
  .l1-config { padding: 20px; }
  .l1-preview { width: 100%; }
}
</style>
