<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { streamQuestion, sendAnswerSSE, skipQuestionSSE, interviewApi } from '@/api/interview'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const sessionId = Number(route.params.sessionId)

// 用户头像 src
const userAvatarSrc = computed(() => {
  const uid = userStore.profile?.id
  if (!uid || !userStore.profile?.avatarUrl) return undefined
  return `/api/v1/user/avatar/${uid}`
})

interface Message { id: number; role: 'AI' | 'USER' | 'SYSTEM'; content: string; streaming: boolean }

const messages = ref<Message[]>([])
const inputText = ref('')
const sending = ref(false)
const active = ref(true)
const waiting = ref(true) // 等待AI第一句话
const chatRef = ref<HTMLElement>()
let currentCtrl: AbortController | null = null

// 进入房间立刻拉取第一题
onMounted(() => {
  currentCtrl = streamQuestion(sessionId,
    (token) => {
      waiting.value = false
      const last = messages.value[messages.value.length - 1]
      if (!last || last.role !== 'AI' || !last.streaming) {
        messages.value.push({ id: Date.now(), role: 'AI', content: token, streaming: true })
      } else {
        last.content += token
      }
      scrollToBottom()
    },
    (done) => {
      const last = messages.value[messages.value.length - 1]
      if (last) last.streaming = false
      if (done.finished) {
        active.value = false
        messages.value.push({ id: Date.now(), role: 'SYSTEM', content: '🎉 面试已结束，报告生成中…', streaming: false })
        ElMessage.success('面试结束，报告生成中...')
        setTimeout(() => router.push(`/interview/${sessionId}/report`), 2000)
      }
    },
    () => {
      waiting.value = false
      messages.value.push({ id: Date.now(), role: 'AI', content: '连接失败，请刷新重试', streaming: false })
    }
  )
})

onBeforeUnmount(() => currentCtrl?.abort())

// 用户发送回答
async function sendAnswer() {
  const text = inputText.value.trim()
  if (!text || sending.value || !active.value) return
  messages.value.push({ id: Date.now(), role: 'USER', content: text, streaming: false })
  inputText.value = ''
  sending.value = true
  scrollToBottom()

  currentCtrl?.abort()
  currentCtrl = sendAnswerSSE(sessionId, text,
    (token) => {
      const last = messages.value[messages.value.length - 1]
      if (!last || last.role !== 'AI' || !last.streaming) {
        messages.value.push({ id: Date.now(), role: 'AI', content: token, streaming: true })
      } else {
        last.content += token
      }
      scrollToBottom()
    },
    (done) => {
      const last = messages.value[messages.value.length - 1]
      if (last) last.streaming = false
      sending.value = false
      if (done.finished) {
        active.value = false
        messages.value.push({ id: Date.now(), role: 'SYSTEM', content: '🎉 面试已结束，报告生成中…', streaming: false })
        ElMessage.success('面试结束！')
        setTimeout(() => router.push(`/interview/${sessionId}/report`), 2000)
      }
    },
    (err) => {
      messages.value.push({ id: Date.now(), role: 'AI', content: '网络错误: ' + err, streaming: false })
      sending.value = false
    }
  )
}

async function skipQuestion() {
  if (!active.value) return
  currentCtrl?.abort()
  sending.value = true
  currentCtrl = skipQuestionSSE(sessionId,
    (token) => {
      const last = messages.value[messages.value.length - 1]
      if (!last || last.role !== 'AI' || !last.streaming) {
        messages.value.push({ id: Date.now(), role: 'AI', content: token, streaming: true })
      } else {
        last.content += token
      }
      scrollToBottom()
    },
    (done) => {
      const last = messages.value[messages.value.length - 1]
      if (last) last.streaming = false
      sending.value = false
      if (done.finished) {
        active.value = false
        messages.value.push({ id: Date.now(), role: 'SYSTEM', content: '🎉 面试已结束，报告生成中…', streaming: false })
        setTimeout(() => router.push(`/interview/${sessionId}/report`), 2000)
      }
    },
    () => { sending.value = false }
  )
}

async function endInterview() {
  currentCtrl?.abort()
  active.value = false
  try { await interviewApi.end(sessionId) } catch { /* */ }
  ElMessage.success('面试已结束')
  router.push(`/interview/${sessionId}/report`)
}

function scrollToBottom() {
  nextTick(() => { if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendAnswer()
  }
}
</script>

<template>
  <div class="interview-room">
    <div class="topbar">
      <el-tag type="primary" effect="dark" size="small">AI 面试官</el-tag>
      <div class="actions">
        <el-button text @click="skipQuestion" :disabled="!active || sending"><el-icon><DArrowRight /></el-icon>跳过</el-button>
        <el-button text type="danger" @click="endInterview"><el-icon><Close /></el-icon>结束</el-button>
      </div>
    </div>

    <div class="chat" ref="chatRef">
      <!-- 等待AI首次回复 -->
      <div v-if="waiting" class="waiting-hint">
        <el-icon :size="28" class="pulse"><ChatDotRound /></el-icon>
        <span>AI 面试官正在出题...</span>
      </div>

      <!-- AI消息：头像左，气泡右 -->
      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role">
        <template v-if="msg.role === 'SYSTEM'">
          <div class="msg-system">{{ msg.content }}</div>
        </template>
        <template v-else-if="msg.role === 'AI'">
          <div class="ai-avatar">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="ai-avatar-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#4F46E5"/>
                  <stop offset="100%" stop-color="#7C3AED"/>
                </linearGradient>
              </defs>
              <circle cx="20" cy="20" r="20" fill="url(#ai-avatar-grad)"/>
              <!-- 人物剪影：头+肩 -->
              <circle cx="20" cy="14" r="6.5" fill="white" fill-opacity="0.95"/>
              <path d="M8 36C8 28 13 22 20 22C27 22 32 28 32 36" fill="white" fill-opacity="0.95"/>
              <!-- 领带 -->
              <path d="M18 20L20 26L22 20" fill="#4F46E5" fill-opacity="0.85"/>
            </svg>
          </div>
          <div class="msg-bubble AI">{{ msg.content }}<span v-if="msg.streaming" class="cursor">|</span></div>
        </template>
        <template v-else>
          <div class="msg-bubble USER">{{ msg.content }}</div>
          <el-avatar :size="32" :src="userAvatarSrc" style="flex-shrink:0">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
        </template>
      </div>
    </div>

    <div class="input-area" v-if="active">
      <el-input
        v-model="inputText" type="textarea" :rows="3"
        placeholder="输入你的回答... (Enter 发送，Shift+Enter 换行)"
        resize="none" :disabled="sending"
        @keydown="onKeydown"
      />
      <div class="input-act">
        <span class="hint">{{ sending ? 'AI 回复中...' : 'Enter 发送' }}</span>
        <el-button type="primary" :loading="sending" :disabled="!inputText.trim()" @click="sendAnswer">发送</el-button>
      </div>
    </div>
    <div class="input-area ended" v-else>
      <div class="ended-note"><el-icon :size="24"><CircleCheckFilled /></el-icon>面试结束</div>
    </div>
  </div>
</template>

<style scoped>
.interview-room{display:flex;flex-direction:column;height:calc(100vh - 56px);background:#F8FAFC}
.topbar{height:48px;background:var(--bg-card);border-bottom:1px solid var(--border-color);display:flex;align-items:center;justify-content:space-between;padding:0 20px;flex-shrink:0}
.actions{display:flex;gap:8px}
.chat{flex:1;overflow-y:auto;padding:20px 24px;display:flex;flex-direction:column;gap:16px}
.waiting-hint{display:flex;align-items:center;gap:10px;color:var(--text-secondary);font-size:14px;padding:20px 0}
.pulse{animation:pulse 1.5s infinite;color:var(--color-primary)}@keyframes pulse{0%,100%{opacity:1}50%{opacity:.3}}
.msg-row{display:flex;gap:10px;align-items:flex-start;max-width:85%}
.ai-avatar{width:36px;height:36px;flex-shrink:0;border-radius:50%;overflow:hidden;box-shadow:0 2px 8px rgba(79,70,229,.25)}
.msg-row.AI{align-self:flex-start}
.msg-row.USER{align-self:flex-end}
.msg-row.SYSTEM{align-self:center;max-width:100%}
.msg-system{text-align:center;padding:10px 24px;font-size:13px;color:var(--color-primary);background:var(--color-primary-bg);border-radius:20px;font-weight:600}
.msg-bubble{padding:10px 14px;border-radius:14px;font-size:14px;line-height:1.7;white-space:pre-wrap;word-break:break-word}
.msg-bubble.AI{background:var(--bg-card);border:1px solid var(--border-color);border-radius:4px 14px 14px 14px;box-shadow:var(--shadow-sm)}
.msg-bubble.USER{background:var(--color-primary);color:#fff;border-radius:14px 4px 14px 14px}
.cursor{animation:blink .8s infinite;font-weight:700;color:var(--color-primary)}@keyframes blink{0%,100%{opacity:1}50%{opacity:0}}
.input-area{flex-shrink:0;padding:12px 24px;background:var(--bg-card);border-top:1px solid var(--border-color)}
.input-act{display:flex;align-items:center;justify-content:space-between;margin-top:8px}
.hint{font-size:12px;color:var(--text-muted)}
.ended{text-align:center;padding:20px}
.ended-note{display:flex;align-items:center;justify-content:center;gap:8px;color:var(--color-success);font-size:15px;font-weight:500}
</style>
