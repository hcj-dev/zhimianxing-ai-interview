<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, type QuestionItem } from '@/api/question'

const questions = ref<QuestionItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(true)
const showDialog = ref(false)
const editing = ref<QuestionItem | null>(null)
const form = ref({ title: '', content: '', answer: '', tags: '[]', difficulty: 1 })

async function fetchQuestions() {
  loading.value = true
  try { const r = await questionApi.list({ page: page.value, pageSize: pageSize.value }); questions.value = r.data.data.records; total.value = r.data.data.total }
  catch { questions.value = [] }
  finally { loading.value = false }
}

onMounted(fetchQuestions)

function openCreate() {
  editing.value = null
  form.value = { title: '', content: '', answer: '', tags: '[]', difficulty: 1 }
  showDialog.value = true
}

function openEdit(q: QuestionItem) {
  editing.value = q
  form.value = { title: q.title, content: q.content, answer: q.answer || '', tags: q.tags || '[]', difficulty: q.difficulty }
  showDialog.value = true
}

async function save() {
  try {
    const data: any = {
      title: form.value.title, content: form.value.content,
      answer: form.value.answer, tags: form.value.tags, difficulty: form.value.difficulty,
    }
    if (editing.value) {
      await questionApi.update(editing.value.id, data)
      ElMessage.success('已更新')
    } else {
      await questionApi.create(data)
      ElMessage.success('已创建')
    }
    showDialog.value = false
    fetchQuestions()
  } catch { /* ignore */ }
}

async function deleteQuestion(q: QuestionItem) {
  await ElMessageBox.confirm(`确定删除 "${q.title}"？`, '确认删除', { type: 'warning' })
  try { await questionApi.delete(q.id); ElMessage.success('已删除'); fetchQuestions() }
  catch { /* ignore */ }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div><h2>题库管理</h2><p>新增、编辑、删除面试题目</p></div>
      <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增题目</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="questions" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="标签" width="200">
          <template #default="{ row }">
            <el-tag v-for="t in (()=>{try{return JSON.parse(row.tags||'[]')}catch{return[]}})()" :key="t" size="small" effect="plain" style="margin-right:4px">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.difficulty===1?'success':row.difficulty===2?'warning':'danger'" size="small" effect="light">{{ row.difficulty===1?'简单':row.difficulty===2?'中等':'困难' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="deleteQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          background
          @current-change="fetchQuestions"
          @size-change="fetchQuestions"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" :title="editing?'编辑题目':'新增题目'" width="640px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="题目内容" required><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="参考答案"><el-input v-model="form.answer" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="标签 (JSON数组)"><el-input v-model="form.tags" placeholder='["Java","Spring"]' /></el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">简单</el-radio><el-radio :value="2">中等</el-radio><el-radio :value="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.pagination-wrap :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--color-primary, #4F46E5);
}
.pagination-wrap :deep(.el-pagination.is-background .el-pager li:hover) {
  color: var(--color-primary, #4F46E5);
}
.pagination-wrap :deep(.el-pagination .el-select .el-input) {
  width: 110px !important;
}

/* 消除固定列的阴影分隔，使表头一致 */
:deep(.el-table__fixed-right-patch) {
  background: var(--el-table-header-bg-color, #F5F7FA);
}
:deep(.el-table__fixed-right::before) {
  display: none;
}
</style>
