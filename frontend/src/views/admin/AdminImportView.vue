<template>
  <div class="import-container">
    <!-- 步骤条 -->
    <el-steps :active="currentStep" align-center class="steps">
      <el-step title="选择类型" />
      <el-step title="上传文件" />
      <el-step title="配置映射" />
      <el-step title="执行导入" />
    </el-steps>

    <!-- 步骤0: 选择导入类型 -->
    <div v-if="currentStep === 0" class="step-content">
      <div class="type-selection">
        <h3>请选择导入数据类型</h3>
        <el-radio-group v-model="importType" size="large" class="type-radio-group">
          <el-radio-button v-for="t in importTypes" :key="t.code" :label="t.code">
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>
        <div class="step-actions">
          <el-button type="primary" @click="handleTypeConfirm" :disabled="!importType">
            下一步
          </el-button>
        </div>
      </div>
    </div>

    <!-- 步骤1: 上传文件 -->
    <div v-if="currentStep === 1" class="step-content">
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
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
        </template>
      </el-upload>

      <div v-if="selectedFile" class="file-info">
        <el-icon><Document /></el-icon>
        <span>{{ selectedFile.name }}</span>
        <el-button type="primary" @click="handleUpload" :loading="uploading"> 开始预览 </el-button>
      </div>

      <div class="step-actions">
        <el-button @click="currentStep = 0">上一步</el-button>
      </div>
    </div>

    <!-- 步骤2: 配置映射 -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="mapping-header">
        <h3>列映射配置</h3>
        <div class="template-actions">
          <el-select
            v-model="selectedTemplateId"
            placeholder="选择已有模板"
            clearable
            @change="applyTemplate"
            style="width: 200px; margin-right: 10px"
          >
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
          style="width: 150px"
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
              <div v-if="row.description" class="field-desc">{{ row.description }}</div>
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
              <span
                v-if="row.excelColumn !== null && row.excelColumn !== undefined && previewData[0]"
              >
                {{ previewData[0][row.excelColumn] || '-' }}
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="step-actions">
        <el-button @click="currentStep = 1">上一步</el-button>
        <el-button type="primary" @click="handleNextStep" :disabled="!isMappingValid">
          下一步
        </el-button>
      </div>
    </div>

    <!-- 步骤3: 执行导入 -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="import-summary">
        <h3>导入确认</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="导入类型">
            <el-tag>{{ currentTypeLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总行数">{{ uploadResult.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="导入年份">
            <el-tag type="primary">{{ importYear }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="mapping-summary">
          <h4>映射配置</h4>
          <el-tag
            v-for="item in mappingTable.filter(
              (m) => m.excelColumn !== null && m.excelColumn !== undefined,
            )"
            :key="item.fieldName"
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

        <div
          v-if="importResult.failDetails && importResult.failDetails.length > 0"
          class="fail-details"
        >
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import {
  uploadExcel,
  executeImport,
  getTemplateList,
  saveTemplate as saveTemplateApi,
  getImportTypes,
  getFieldMetas,
} from '../../api/import'

// 步骤控制
const currentStep = ref(0)

// 导入类型
const importType = ref('')
const importTypes = ref([])

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

// 动态字段列表（从后端获取）
const systemFields = ref([])

// 映射表格数据
const mappingTable = ref([])

// 当前类型的中文标签
const currentTypeLabel = computed(() => {
  const found = importTypes.value.find((t) => t.code === importType.value)
  return found ? found.label : ''
})

// 计算映射是否有效（需要检查必填字段和年份）
const isMappingValid = computed(() => {
  if (!importYear.value) return false
  const requiredFields = mappingTable.value.filter((m) => m.required)
  return requiredFields.every((m) => m.excelColumn !== null && m.excelColumn !== undefined)
})

// 加载导入类型列表
onMounted(async () => {
  try {
    const res = await getImportTypes()
    importTypes.value = res.data || []
    // 默认选第一个
    if (importTypes.value.length > 0) {
      importType.value = importTypes.value[0].code
    }
  } catch (error) {
    console.error('加载导入类型失败', error)
  }
})

// 切换类型时加载字段元数据和模板列表
watch(importType, async (type) => {
  if (!type) return
  try {
    // 加载字段
    const fieldRes = await getFieldMetas(type)
    systemFields.value = fieldRes.data || []
    mappingTable.value = systemFields.value.map((f) => ({
      ...f,
      excelColumn: null,
    }))
    // 加载该类型的模板
    const tplRes = await getTemplateList(type)
    templateList.value = tplRes.data || []
    selectedTemplateId.value = null
  } catch (error) {
    console.error('加载字段元数据失败', error)
  }
})

// 确认类型后进入下一步
const handleTypeConfirm = async () => {
  if (!importType.value) {
    ElMessage.warning('请选择导入类型')
    return
  }
  // 确保字段已加载
  if (systemFields.value.length === 0) {
    try {
      const fieldRes = await getFieldMetas(importType.value)
      systemFields.value = fieldRes.data || []
      mappingTable.value = systemFields.value.map((f) => ({
        ...f,
        excelColumn: null,
      }))
    } catch (e) {
      console.error('加载字段配置失败', e)
      ElMessage.error('加载字段配置失败')
      return
    }
  }
  currentStep.value = 1
}

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
    previewData.value = (res.data.previewData || []).map((row) => {
      if (Array.isArray(row)) return row
      return excelColumns.value.map((_, i) => row[i] || '')
    })

    // 自动匹配列名
    autoMatchColumns()

    currentStep.value = 2
    ElMessage.success('文件解析成功')
  } catch (error) {
    console.error('上传失败', error)
  } finally {
    uploading.value = false
  }
}

// 自动匹配列名
const autoMatchColumns = () => {
  const columnLower = excelColumns.value.map((c) => c.toLowerCase())

  mappingTable.value.forEach((item) => {
    const fieldLower = item.fieldName.toLowerCase()
    const labelLower = item.label.toLowerCase()

    const index = columnLower.findIndex(
      (c) =>
        c.includes(fieldLower) ||
        c.includes(labelLower) ||
        fieldLower.includes(c) ||
        labelLower.includes(c),
    )

    if (index !== -1) {
      item.excelColumn = index
    }
  })
}

// 应用模板
const applyTemplate = (templateId) => {
  if (!templateId) return

  const template = templateList.value.find((t) => t.id === templateId)
  if (template && template.columnMapping) {
    try {
      const mapping = JSON.parse(template.columnMapping)
      mappingTable.value.forEach((item) => {
        if (mapping[item.fieldName] !== undefined) {
          item.excelColumn = mapping[item.fieldName]
        }
      })
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
const saveTemplate = async () => {
  if (!newTemplateName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }

  const mapping = {}
  mappingTable.value.forEach((item) => {
    if (item.excelColumn !== null && item.excelColumn !== undefined) {
      mapping[item.fieldName] = item.excelColumn
    }
  })

  try {
    await saveTemplateApi({
      templateName: newTemplateName.value,
      description: '',
      columnMapping: JSON.stringify(mapping),
      importType: importType.value,
    })

    // 重新获取模板列表
    const res = await getTemplateList(importType.value)
    templateList.value = res.data || []

    showSaveTemplate.value = false
    newTemplateName.value = ''
    ElMessage.success('模板保存成功')
  } catch (error) {
    console.error('保存模板失败', error)
    ElMessage.error('保存模板失败')
  }
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
  currentStep.value = 3
}

// 执行导入
const handleExecute = async () => {
  const mapping = {}
  mappingTable.value.forEach((item) => {
    if (item.excelColumn !== null && item.excelColumn !== undefined) {
      mapping[item.fieldName] = item.excelColumn
    }
  })

  importing.value = true
  try {
    const res = await executeImport({
      sessionId: uploadResult.value.sessionId,
      importType: importType.value,
      mapping: mapping,
      year: importYear.value,
      saveAsTemplate: false,
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
  mappingTable.value = systemFields.value.map((f) => ({
    ...f,
    excelColumn: null,
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

.type-selection {
  text-align: center;
  padding: 60px 20px;
}

.type-selection h3 {
  margin-bottom: 30px;
  color: #303133;
}

.type-radio-group {
  margin-bottom: 40px;
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

.field-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
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
