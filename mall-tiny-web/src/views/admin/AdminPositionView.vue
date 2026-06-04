<template>
  <div class="position-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="部门">
          <el-input v-model="searchForm.department" placeholder="请输入部门" clearable @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="年份">
          <el-input-number v-model="searchForm.year" :min="2020" :max="2030" placeholder="请选择年份" clearable controls-position="right" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">新增岗位</el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
        stripe
        border
      >
        <el-table-column type="selection" width="50" :reserve-selection="true" />
        <el-table-column prop="department" label="部门" min-width="150" show-overflow-tooltip />
        <el-table-column prop="positionName" label="岗位名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="year" label="年份" width="80" />
        <el-table-column prop="educationRequired" label="学历要求" width="100" />
        <el-table-column prop="majorRequired" label="专业要求" min-width="150" show-overflow-tooltip />
        <el-table-column prop="recruitmentNumber" label="招录人数" width="90" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该岗位吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="部门" prop="department">
          <el-input v-model="formData.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="positionName">
          <el-input v-model="formData.positionName" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-input-number v-model="formData.year" :min="2020" :max="2030" controls-position="right" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="formData.educationRequired" placeholder="请选择" clearable>
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士研究生" value="硕士研究生" />
            <el-option label="博士研究生" value="博士研究生" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业要求">
          <el-input v-model="formData.majorRequired" placeholder="请输入专业要求" />
        </el-form-item>
        <el-form-item label="政治面貌要求">
          <el-select v-model="formData.politicalStatusRequired" placeholder="请选择" clearable>
            <el-option label="不限" value="不限" />
            <el-option label="共青团员" value="共青团员" />
            <el-option label="中共党员" value="中共党员" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否限应届">
          <el-switch v-model="formData.isFreshOnly" :active-value="true" :inactive-value="false" />
        </el-form-item>
        <el-form-item label="招录人数">
          <el-input-number v-model="formData.recruitmentNumber" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="报名截止时间">
          <el-date-picker
            v-model="formData.registrationDeadline"
            type="datetime"
            placeholder="请选择截止时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPositionPage,
  createPosition,
  updatePosition,
  deletePosition,
  batchDeletePositions
} from '../../api/positionManage'

// 搜索表单
const searchForm = reactive({
  department: '',
  year: null
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const selectedIds = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const formRef = ref(null)
const submitLoading = ref(false)
const editingId = ref(null)

const formData = reactive({
  department: '',
  positionName: '',
  year: new Date().getFullYear(),
  educationRequired: '',
  majorRequired: '',
  politicalStatusRequired: '',
  isFreshOnly: false,
  recruitmentNumber: 1,
  registrationDeadline: ''
})

const formRules = {
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  year: [{ required: true, message: '请选择年份', trigger: 'change' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (searchForm.department) params.department = searchForm.department
    if (searchForm.year) params.year = searchForm.year

    const res = await getPositionPage(params)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载岗位列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const resetSearch = () => {
  searchForm.department = ''
  searchForm.year = null
  pagination.pageNum = 1
  loadData()
}

// 分页
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

const handleCurrentChange = (page) => {
  pagination.pageNum = page
  loadData()
}

// 多选
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 新增
const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增岗位'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  editingId.value = row.id
  dialogTitle.value = '编辑岗位'
  Object.assign(formData, {
    department: row.department || '',
    positionName: row.positionName || '',
    year: row.year || new Date().getFullYear(),
    educationRequired: row.educationRequired || '',
    majorRequired: row.majorRequired || '',
    politicalStatusRequired: row.politicalStatusRequired || '',
    isFreshOnly: row.isFreshOnly || false,
    recruitmentNumber: row.recruitmentNumber || 1,
    registrationDeadline: row.registrationDeadline || ''
  })
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updatePosition(editingId.value, { ...formData })
      ElMessage.success('编辑成功')
    } else {
      await createPosition({ ...formData })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await deletePosition(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条记录吗？`,
      '批量删除',
      { type: 'warning' }
    )
    await batchDeletePositions(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    department: '',
    positionName: '',
    year: new Date().getFullYear(),
    educationRequired: '',
    majorRequired: '',
    politicalStatusRequired: '',
    isFreshOnly: false,
    recruitmentNumber: 1,
    registrationDeadline: ''
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.position-manage {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.table-toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
