<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'

const router = useRouter()
const uploading = ref(false)
const fileList = ref<any[]>([])

async function handleUpload() {
  if (fileList.value.length === 0) return
  uploading.value = true
  try {
    const file = fileList.value[0].raw
    await resumeApi.upload(file)
    ElMessage.success('简历上传成功，AI 正在后台分析，请稍后刷新查看结果')
    router.push('/resumes')
  } catch {
    // 错误已由拦截器处理
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>上传简历</h2>
        <p>支持 PDF 格式，上传后 AI 将自动分析并给出优化建议</p>
      </div>
    </div>

    <el-card>
      <div class="upload-area">
        <el-upload
          v-model:file-list="fileList"
          drag
          :auto-upload="false"
          accept=".pdf"
          :limit="1"
        >
          <el-icon :size="64" color="#4F46E5"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="upload-title">将简历文件拖到此处，或<em>点击上传</em></p>
            <p class="upload-hint">仅支持 PDF 格式，文件大小不超过 10MB</p>
          </div>
        </el-upload>

        <div class="upload-tips">
          <h4>💡 上传建议</h4>
          <ul>
            <li>确保简历为单栏排版，AI 对多栏布局的解析效果较差</li>
            <li>项目经历尽量包含技术栈关键词（如 Spring Boot、Redis、MySQL）</li>
            <li>文件名建议包含你的姓名和求职方向</li>
          </ul>
        </div>

        <div class="upload-actions">
          <el-button @click="router.back()">取消</el-button>
          <el-button type="primary" :loading="uploading" :disabled="fileList.length === 0"
            @click="handleUpload">
            {{ uploading ? '上传中...' : '开始分析' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.upload-area { padding: 8px 0; }
.upload-text { margin-top: 12px; }
.upload-title { font-size: 15px; color: var(--text-primary); }
.upload-title em { color: var(--color-primary); font-style: normal; font-weight: 600; }
.upload-hint { font-size: 12px; color: var(--text-muted); margin-top: 6px; }
.upload-tips {
  margin-top: 28px; padding: 16px 20px; background: var(--color-primary-bg);
  border-radius: var(--radius-md); border: 1px solid #C7D2FE;
}
.upload-tips h4 { font-size: 13px; margin-bottom: 8px; color: var(--color-primary-dark); }
.upload-tips ul { font-size: 13px; color: var(--text-secondary); padding-left: 18px; }
.upload-tips li { margin-bottom: 4px; }
.upload-actions { margin-top: 24px; display: flex; justify-content: flex-end; gap: 12px; }
</style>
