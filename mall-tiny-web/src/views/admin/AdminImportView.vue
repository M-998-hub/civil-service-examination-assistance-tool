<template>
  <div class="import-container">
    <!-- 步骤条 -->
    <el-steps :active="currentStep" align-center class="steps">
      <el-step title="上传文件" />
      <el-step title="配置映射" />
      <el-step title="执行导入" />
    </el-steps>

    <!-- 步骤1: 上传文件 -->
    <div v-if="currentStep === 0" class="step-content">
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将 Excel 文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
        </template>
      </el-upload>

      <div v-if="selectedFile" class="file-info">
        <el-icon><Document /></el-icon>
        <span>{{ selectedFile.name }}</span>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          开始预览
        </el-button>
      </div>
    </div>

    <!-- 步骤2: 配置映射 -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="mapping-header">
        <h3>列映射配置</h3>
        <div class="template-actions">
          <el-select v-model="selectedTemplateId" placeholder="选择已有模板" clearable @change="applyTemplate" style="width: 200px; margin-right: 10px;">
            <el-option
              v-for="tpl in templateList"
              :key="tpl.id"
              :label="tpl.templateName"
              :value="tpl.id"
            />
          </el-select>
          <el-button @click="showSaveTemplate = true">保存为模板</el-button>
        </div>
      </div>

      <!-- 年份输入 -->
      <div class="year-input-section">
        <span class="year-label">导入年份 <span class="required-mark">*</span>：</span>
        <el-date-picker
          v-model="importYear"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          style="width: 150px;"
        />
        <span class="year-hint">所有导入数据的年份将设置为该值</span>
      </div>

      <!-- 预览数据 -->
      <div class="preview-section">
        <h4>数据预览（前5行）</h4>
        <el-table :data="previewData" border size="small" max-height="200">
          <el-table-column
            v-for="(col, index) in excelColumns"
            :key="index"
            :prop="String(index)"
            :label="col"
          />
        </el-table>
      </div>

      <!-- 映射配置 -->
      <div class="mapping-section">
        <h4>字段映射</h4>
        <el-table :data="mappingTable" border>
          <el-table-column label="系统字段" prop="field" width="200">
            <template #default="{ row }">
              <span :class="{ required: row.required }">
                {{ row.label }}
                <span v-if="row.required" class="required-mark">*</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Excel列" prop="excelColumn">
            <template #default="{ row }">
              <el-select v-model="row.excelColumn" placeholder="请选择Excel列" clearable>
                <el-option
                  v-for="(col, index) in excelColumns"
                  :key="index"
                  :label="col"
                  :value="index"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="示例值" prop="sample">
            <template #default="{ row }">
              <span v-if="row.excelColumn !== null && row.excelColumn !== undefined && previewData[0]">
                {{ previewData[0][row.excelColumn] || '-' }}
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="step-actions">
        <el-button @click="currentStep = 0">上一步</el-button>
        <el-button type="primary" @click="handleNextStep" :disabled="!isMappingValid">
          下一步
        </el-button>
      </div>
    </div>

    <!-- 步骤3: 执行导入 -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="import-summary">
        <h3>导入确认</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总行数">{{ uploadResult.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="Excel列数">{{ excelColumns.length }}</el-descriptions-item>
          <el-descriptions-item label="导入年份">
            <el-tag type="primary">{{ importYear }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="mapping-summary">
          <h4>映射配置</h4>
          <el-tag
            v-for="item in mappingTable.filter(m => m.excelColumn !== null && m.excelColumn !== undefined)"
            :key="item.field"
            class="mapping-tag"
          >
            {{ item.label }} → {{ excelColumns[item.excelColumn] }}
          </el-tag>
        </div>

        <el-button type="primary" size="large" @click="handleExecute" :loading="importing">
          开始导入
        </el-button>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" class="import-result">
        <h3>导入结果</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="成功">
            <el-tag type="success">{{ importResult.successCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-tag type="danger">{{ importResult.failCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="跳过（重复）">
            <el-tag type="warning">{{ importResult.skipCount }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="importResult.failDetails && importResult.failDetails.length > 0" class="fail-details">
          <h4>失败详情</h4>
          <el-table :data="importResult.failDetails" border max-height="300">
            <el-table-column prop="rowNum" label="行号" width="80" />
            <el-table-column prop="data" label="数据" show-overflow-tooltip />
            <el-table-column prop="reason" label="失败原因" />
          </el-table>
        </div>
      </div>

      <div class="step-actions">
        <el-button @click="resetImport">重新导入</el-button>
      </div>
    </div>

    <!-- 保存模板对话框 -->
    <el-dialog v-model="showSaveTemplate" title="保存为模板" width="400px">
      <el-form>
        <el-form-item label="模板名称">
          <el-input v-model="newTemplateName" placeholder="请输入模板名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveTemplate = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { uploadExcel, executeImport, getTemplateList } from '../../api/import'

// 步骤控制
const currentStep = ref(0)

// 文件上传
const uploadRef = ref()
const selectedFile = ref(null)
const uploading = ref(false)
const uploadResult = ref(null)

// Excel数据
const excelColumns = ref([])
const previewData = ref([])

// 年份（手动输入）
const importYear = ref(new Date().getFullYear().toString())

// 模板
const templateList = ref([])
const selectedTemplateId = ref(null)
const showSaveTemplate = ref(false)
const newTemplateName = ref('')

// 导入
const importing = ref(false)
const importResult = ref(null)

// 系统字段定义（不包含year，year改为手动输入）
const systemFields = [
  { field: 'department', label: '部门', required: true },
  { field: 'positionName', label: '职位名称', required: true },
  { field: 'majorRequired', label: '专业要求', required: false },
  { field: 'educationRequired', label: '学历要求', required: false },
  { field: 'politicalStatusRequired', label: '政治面貌要求', required: false },
  { field: 'isFreshOnly', label: '是否限应届', required: false },
  { field: 'recruitmentNumber', label: '招录人数', required: false },
]

// 映射表格数据
const mappingTable = ref(systemFields.map(f => ({
  ...f,
  excelColumn: null
})))

// 计算映射是否有效（需要检查必填字段和年份）
const isMappingValid = computed(() => {
  // 检查年份是否已填写
  if (!importYear.value) return false
  
  // 检查必填字段是否已映射
  const requiredFields = mappingTable.value.filter(m => m.required)
  return requiredFields.every(m => m.excelColumn !== null && m.excelColumn !== undefined)
})

// 加载模板列表
onMounted(async () => {
  try {
    const res = await getTemplateList()
    templateList.value = res.data || []
  } catch (error) {
    console.error('加载模板列表失败', error)
  }
})

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

// 上传并预览
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  try {
    const res = await uploadExcel(selectedFile.value)
    uploadResult.value = res.data
    excelColumns.value = res.data.columns || []
    previewData.value = (res.data.previewData || []).map(row => {
      // 转换为数组格式
      if (Array.isArray(row)) return row
      return excelColumns.value.map((_, i) => row[i] || '')
    })
    
    // 自动匹配列名
    autoMatchColumns()
    
    currentStep.value = 1
    ElMessage.success('文件解析成功')
  } catch (error) {
    console.error('上传失败', error)
  } finally {
    uploading.value = false
  }
}

// 自动匹配列名
const autoMatchColumns = () => {
  const columnLower = excelColumns.value.map(c => c.toLowerCase())
  
  mappingTable.value.forEach(item => {
    const fieldLower = item.field.toLowerCase()
    const labelLower = item.label.toLowerCase()
    
    // 尝试匹配字段名或标签名
    const index = columnLower.findIndex(c => 
      c.includes(fieldLower) || 
      c.includes(labelLower) ||
      fieldLower.includes(c) ||
      labelLower.includes(c)
    )
    
    if (index !== -1) {
      item.excelColumn = index
    }
  })
}

// 应用模板
const applyTemplate = (templateId) => {
  if (!templateId) return
  
  const template = templateList.value.find(t => t.id === templateId)
  if (template && template.columnMapping) {
    try {
      const mapping = JSON.parse(template.columnMapping)
      mappingTable.value.forEach(item => {
        if (mapping[item.field] !== undefined) {
          item.excelColumn = mapping[item.field]
        }
      })
      // 应用年份（模板中单独保存的年份字段）
      if (template.year) {
        importYear.value = template.year
      }
      ElMessage.success('模板已应用')
    } catch (error) {
      console.error('解析模板失败', error)
    }
  }
}

// 保存模板
const saveTemplate = () => {
  if (!newTemplateName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  
  // 构建映射对象（不包含年份）
  const mapping = {}
  mappingTable.value.forEach(item => {
    if (item.excelColumn !== null && item.excelColumn !== undefined) {
      mapping[item.field] = item.excelColumn
    }
  })
  
  // 这里需要调用后端保存模板，暂时用本地存储
  const templates = JSON.parse(localStorage.getItem('importTemplates') || '[]')
  templates.push({
    id: Date.now(),
    templateName: newTemplateName.value,
    columnMapping: JSON.stringify(mapping),
    year: importYear.value,
    createTime: new Date().toISOString()
  })
  localStorage.setItem('importTemplates', JSON.stringify(templates))
  
  templateList.value = templates
  showSaveTemplate.value = false
  newTemplateName.value = ''
  ElMessage.success('模板保存成功')
}

// 下一步
const handleNextStep = () => {
  if (!importYear.value) {
    ElMessage.warning('请选择导入年份')
    return
  }
  if (!isMappingValid.value) {
    ElMessage.warning('请完成必填字段的映射')
    return
  }
  currentStep.value = 2
}

// 执行导入
const handleExecute = async () => {
  // 构建映射对象（不包含年份，年份单独传递）
  const mapping = {}
  mappingTable.value.forEach(item => {
    if (item.excelColumn !== null && item.excelColumn !== undefined) {
      mapping[item.field] = item.excelColumn
    }
  })
  
  importing.value = true
  try {
    const res = await executeImport({
      sessionId: uploadResult.value.sessionId,
      mapping: mapping,
      year: importYear.value,
      saveAsTemplate: false
    })
    importResult.value = res.data
    ElMessage.success('导入完成')
  } catch (error) {
    console.error('导入失败', error)
  } finally {
    importing.value = false
  }
}

// 重置导入
const resetImport = () => {
  currentStep.value = 0
  selectedFile.value = null
  uploadResult.value = null
  excelColumns.value = []
  previewData.value = []
  importResult.value = null
  importYear.value = new Date().getFullYear().toString()
  mappingTable.value = systemFields.map(f => ({
    ...f,
    excelColumn: null
  }))
}
</script>

<style scoped>
.import-container {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.steps {
  margin-bottom: 30px;
}

.step-content {
  min-height: 400px;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.mapping-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.mapping-header h3 {
  margin: 0;
}

.template-actions {
  display: flex;
  align-items: center;
}

.year-input-section {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: #fdf6ec;
  border-radius: 4px;
}

.year-label {
  font-weight: 500;
  margin-right: 10px;
}

.year-hint {
  margin-left: 15px;
  color: #909399;
  font-size: 13px;
}

.preview-section {
  margin-bottom: 20px;
}

.preview-section h4 {
  margin-bottom: 10px;
}

.mapping-section {
  margin-bottom: 20px;
}

.mapping-section h4 {
  margin-bottom: 10px;
}

.required {
  font-weight: 500;
}

.required-mark {
  color: #f56c6c;
}

.text-muted {
  color: #909399;
}

.step-actions {
  margin-top: 30px;
  text-align: center;
}

.step-actions .el-button {
  min-width: 100px;
  margin: 0 10px;
}

.import-summary {
  text-align: center;
}

.import-summary h3 {
  margin-bottom: 20px;
}

.mapping-summary {
  margin: 20px 0;
}

.mapping-summary h4 {
  margin-bottom: 10px;
}

.mapping-tag {
  margin: 5px;
}

.import-result {
  margin-top: 30px;
  text-align: left;
}

.import-result h3 {
  margin-bottom: 15px;
}

.fail-details {
  margin-top: 20px;
}

.fail-details h4 {
  margin-bottom: 10px;
}
</style>
