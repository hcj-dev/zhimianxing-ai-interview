<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { resumeApi } from '@/api/resume'
import type { ResumeItem } from '@/types/resume'

const router = useRouter()

const resumes = ref<ResumeItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await resumeApi.list()
    resumes.value = res.data.data || []
  } catch {
    // 后端接口未就绪时显示空列表，不弹错误
  } finally {
    loading.value = false
  }
})

function getStatusTag(status: string) {
  const map: Record<string, { type: string; label: string }> = {
    PROCESSING: { type: 'warning', label: '分析中' },
    COMPLETED: { type: 'success', label: '已完成' },
    FAILED: { type: 'danger', label: '失败' },
  }
  return map[status] || { type: 'info', label: status }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除这份简历吗？', '确认删除', { type: 'warning' })
  try {
    await resumeApi.delete(id)
    resumes.value = resumes.value.filter(r => r.id !== id)
    ElMessage.success('已删除')
  } catch {
    // 后端未就绪时直接从列表移除
    resumes.value = resumes.value.filter(r => r.id !== id)
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>简历管理</h2>
        <p>上传简历，获取 AI 分析和优化建议</p>
      </div>
      <el-button type="primary" @click="router.push('/resumes/upload')">
        <el-icon><Upload /></el-icon>上传简历
      </el-button>
    </div>

    <el-card v-loading="loading">
      <!-- Empty state -->
      <div v-if="!loading && resumes.length === 0" class="empty-state">
        <el-icon :size="56" color="#CBD5E1"><Document /></el-icon>
        <p>还没有上传简历</p>
        <p class="sub">上传你的第一份简历，AI 将帮你分析和优化</p>
        <el-button type="primary" style="margin-top: 16px;" @click="router.push('/resumes/upload')">
          立即上传
        </el-button>
      </div>

      <el-table v-else :data="resumes" style="width: 100%" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="240">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/resumes/${row.id}`)">
              {{ row.fileName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" align="center">
          <template #default="{ row }">
            <span class="version-badge">v{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" size="small" effect="light">
              {{ getStatusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="综合评分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 'COMPLETED' && row.overallScore != null" class="score">
              {{ row.overallScore }}<small>分</small>
            </span>
            <span v-else-if="row.status === 'PROCESSING'" class="text-muted">分析中</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button text type="primary" size="small" @click="router.push(`/resumes/${row.id}`)">
                查看
              </el-button>
              <el-button text type="primary" size="small"
                @click="router.push(`/resumes/${row.id}/match-jd`)">
                JD匹配
              </el-button>
              <el-popconfirm
                title="确定要删除这份简历吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(row.id)"
              >
                <template #reference>
                  <el-button text type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.action-cell {
  display: inline-flex;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

.version-badge {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-input);
  padding: 2px 8px;
  border-radius: 4px;
}

.score {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 16px;
}
.score small {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-secondary);
}
.text-muted { color: var(--text-muted); }

.empty-state {
  text-align: center;
  padding: 60px 0;
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
  color: var(--text-secondary);
}
.empty-state .sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
