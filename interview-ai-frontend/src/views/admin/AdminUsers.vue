<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminUser } from '@/api/admin'

const users = ref<AdminUser[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(true)

async function fetchUsers() {
  loading.value = true
  try {
    const r = await adminApi.getUsers({ page: page.value, pageSize: pageSize.value, keyword: keyword.value })
    users.value = r.data.data.records; total.value = r.data.data.total
  } catch { users.value = [] }
  finally { loading.value = false }
}

onMounted(fetchUsers)

async function toggleStatus(user: AdminUser) {
  const action = user.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '确认操作', { type: 'warning' })
  try {
    await adminApi.toggleUserStatus(user.id)
    ElMessage.success(`已${action}`)
    fetchUsers()
  } catch { /* ignore */ }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>用户管理</h2><p>查看所有用户，禁用/启用账号</p></div>

    <el-card style="margin-bottom:16px">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width:240px" @keyup.enter="fetchUsers" />
      <el-button style="margin-left:8px" @click="fetchUsers">搜索</el-button>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role==='ADMIN'?'danger':'info'" size="small" effect="light">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'danger'" size="small" effect="light">{{ row.status===1?'正常':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text :type="row.status===1?'danger':'success'" size="small" @click="toggleStatus(row)">
              {{ row.status===1?'禁用':'启用' }}
            </el-button>
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
          @current-change="fetchUsers"
          @size-change="fetchUsers"
        />
      </div>
    </el-card>
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
</style>
