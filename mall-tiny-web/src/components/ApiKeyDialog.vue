<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="440px" destroy-on-close @closed="onClosed">
    <template v-if="mode === 'config'">
      <p class="dialog-desc">请输入你的 DeepSeek API Key，仅保存在浏览器本地，不会上传到服务器。</p>
      <el-input v-model="inputKey" placeholder="sk-xxxxxxxxxxxx" clearable @keyup.enter="handleSave" />
      <p class="dialog-hint">在 <a href="https://platform.deepseek.com/api_keys" target="_blank">DeepSeek control panel</a> get API Key</p>
    </template>

    <template v-else-if="mode === 'view'">
      <p class="dialog-desc">current API Key：</p>
      <div class="key-display">{{ maskedKey }}</div>
      <div class="view-actions">
        <el-button type="primary" @click="switchMode('config')">edit</el-button>
        <el-button type="danger" @click="switchMode('delete')">delete</el-button>
      </div>
    </template>

    <template v-else-if="mode === 'delete'">
      <el-alert title="delete API Key？" type="warning" :closable="false" show-icon />
      <p class="dialog-desc delete-warn">After deletion all AI features will be unavailable.</p>
    </template>

    <template #footer>
      <template v-if="mode === 'config'">
        <el-button @click="visible = false">cancel</el-button>
        <el-button type="primary" @click="handleSave">save</el-button>
      </template>
      <template v-else-if="mode === 'view'">
        <el-button @click="visible = false">close</el-button>
      </template>
      <template v-else-if="mode === 'delete'">
        <el-button @click="switchMode('view')">cancel</el-button>
        <el-button type="danger" @click="handleDelete">confirm delete</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getApiKey, setApiKey, removeApiKey, maskApiKey } from '../api/ai'

const emit = defineEmits(['saved', 'deleted'])
const visible = ref(false)
const mode = ref('config')
const inputKey = ref('')

const maskedKey = computed(() => {
  const key = getApiKey()
  return key ? maskApiKey(key) : 'not configured'
})

const dialogTitle = computed(() => {
  if (mode.value === 'config') return maskedKey.value !== 'not configured' ? 'edit DeepSeek API Key' : 'config DeepSeek API Key'
  if (mode.value === 'view') return 'API Key info'
  return 'delete API Key'
})

function open(m) {
  mode.value = m || 'config'
  inputKey.value = ''
  visible.value = true
}

function switchMode(m) { mode.value = m }

function handleSave() {
  const key = inputKey.value.trim()
  if (!key) { ElMessage.warning('please input API Key'); return }
  setApiKey(key)
  ElMessage.success('API Key saved')
  visible.value = false
  emit('saved')
}

function handleDelete() {
  removeApiKey()
  ElMessage.success('API Key deleted')
  visible.value = false
  emit('deleted')
}

function onClosed() { inputKey.value = '' }

defineExpose({ open })
</script>

<style scoped>
.dialog-desc { color: #606266; font-size: 14px; margin-bottom: 12px; line-height: 1.6; }
.dialog-hint { color: #909399; font-size: 12px; margin-top: 8px; }
.dialog-hint a { color: #409EFF; }
.key-display { font-family: monospace; font-size: 16px; padding: 12px; background: #f5f7fa; border-radius: 6px; text-align: center; letter-spacing: 1px; margin-bottom: 16px; }
.view-actions { display: flex; gap: 12px; justify-content: center; }
.delete-warn { color: #E6A23C; }
</style>
