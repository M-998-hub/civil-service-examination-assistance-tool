<template>
  <div class="archive-container">
    <h1>个人档案</h1>
    <el-card>
      <el-form ref="formRef" :model="archiveForm" :rules="rules" label-width="120px">
        <el-form-item label="专业" prop="major">
          <el-input v-model="archiveForm.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="学历" prop="education">
          <el-select v-model="archiveForm.education" placeholder="请选择学历" style="width: 100%">
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="政治面貌" prop="politicalStatus">
          <el-select
            v-model="archiveForm.politicalStatus"
            placeholder="请选择政治面貌"
            style="width: 100%"
          >
            <el-option label="群众" value="群众" />
            <el-option label="共青团员" value="共青团员" />
            <el-option label="中共党员" value="中共党员" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否应届">
          <el-switch v-model="archiveForm.isFreshGraduate" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="loading">保存档案</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyArchive, saveMyArchive } from '../api/archive'

const formRef = ref(null)
const loading = ref(false)

const archiveForm = reactive({
  major: '',
  education: '',
  politicalStatus: '',
  isFreshGraduate: false,
})

const rules = {
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  education: [{ required: true, message: '请选择学历', trigger: 'change' }],
  politicalStatus: [{ required: true, message: '请选择政治面貌', trigger: 'change' }],
}

const loadArchiveData = async () => {
  try {
    const res = await getMyArchive()
    if (res.data) {
      archiveForm.major = res.data.major || ''
      archiveForm.education = res.data.education || ''
      archiveForm.politicalStatus = res.data.politicalStatus || ''
      archiveForm.isFreshGraduate = res.data.isFreshGraduate || false
    }
  } catch (error) {
    console.error('获取档案失败:', error)
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await saveMyArchive(archiveForm)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadArchiveData()
})
</script>

<style scoped>
.archive-container {
  padding: 20px;
}

h1 {
  color: #303133;
  margin-bottom: 20px;
}

.el-card {
  max-width: 600px;
}
</style>
