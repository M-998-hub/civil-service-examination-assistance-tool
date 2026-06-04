<template>
  <div class="position-container">
    <h1>岗位列表</h1>
    <el-card class="filter-card">
      <el-form :model="filterForm" label-width="100px" inline>
        <el-form-item label="年份">
          <el-input v-model.number="filterForm.year" placeholder="如: 2026" style="width: 120px" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="filterForm.department" placeholder="部门名称" style="width: 150px" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="filterForm.educationLevel" placeholder="请选择" clearable style="width: 120px">
            <el-option label="本科" :value="2" />
            <el-option label="硕士" :value="3" />
            <el-option label="博士" :value="4" />
          </el-select>
          <span class="education-hint">该学历及以下可报</span>
        </el-form-item>
        <el-form-item label="政治面貌">
          <el-select v-model="filterForm.politicalStatusLevel" placeholder="请选择" clearable style="width: 120px">
            <el-option label="不限" :value="1" />
            <el-option label="共青团员" :value="2" />
            <el-option label="中共党员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="限应届">
          <el-select v-model="filterForm.isFreshOnly" placeholder="请选择" clearable style="width: 100px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">筛选</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <div class="table-toolbar">
        <el-button
          type="warning"
          :disabled="selectedRows.length < 2 || selectedRows.length > 3"
          @click="handleCompare"
        >
          对比 ({{ selectedRows.length }}/3)
        </el-button>
        <span v-if="selectedRows.length > 3" class="selection-hint">最多选择 3 个岗位</span>
      </div>
      <el-table
        ref="tableRef"
        :data="positionList"
        style="width: 100%"
        v-loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" :reserve-selection="true" />
        <el-table-column prop="positionName" label="岗位名称" min-width="150" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="year" label="年份" width="80" />
        <el-table-column prop="educationRequired" label="学历要求" width="100" />
        <el-table-column prop="politicalStatusRequired" label="政治面貌要求" width="120" />
        <el-table-column prop="majorRequired" label="专业要求" min-width="120" show-overflow-tooltip />
        <el-table-column prop="recruitmentNumber" label="招录人数" width="90" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="handleDetail(scope.row)">详情</el-button>
            <el-button size="small" link @click="handleFavorite(scope.row.id)">收藏</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <PositionDetailDialog ref="detailDialogRef" />
    <PositionCompareDialog ref="compareDialogRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { filterPositions } from '../api/position'
import { addFavorite } from '../api/favorite'
import PositionDetailDialog from './PositionDetailDialog.vue'
import PositionCompareDialog from './PositionCompareDialog.vue'

const positionList = ref([])
const loading = ref(false)
const selectedRows = ref([])
const tableRef = ref(null)
const detailDialogRef = ref(null)
const compareDialogRef = ref(null)

const filterForm = reactive({
  year: null,
  department: '',
  educationLevel: null,
  politicalStatusLevel: null,
  isFreshOnly: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadPositions = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (filterForm.year) params.year = filterForm.year
    if (filterForm.department) params.department = filterForm.department
    if (filterForm.educationLevel) params.educationLevel = filterForm.educationLevel
    if (filterForm.politicalStatusLevel) params.politicalStatusLevel = filterForm.politicalStatusLevel
    if (filterForm.isFreshOnly !== null) params.isFreshOnly = filterForm.isFreshOnly

    const res = await filterPositions(params)
    positionList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取岗位列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pagination.pageNum = 1
  loadPositions()
}

const handleReset = () => {
  filterForm.year = null
  filterForm.department = ''
  filterForm.educationLevel = null
  filterForm.politicalStatusLevel = null
  filterForm.isFreshOnly = null
  pagination.pageNum = 1
  loadPositions()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadPositions()
}

const handleCurrentChange = (current) => {
  pagination.pageNum = current
  loadPositions()
}

const handleFavorite = async (positionId) => {
  try {
    await addFavorite(positionId)
    ElMessage.success('收藏成功')
  } catch (error) {
    console.error('收藏失败:', error)
  }
}

const handleDetail = (row) => {
  detailDialogRef.value?.open(row.id)
}

const handleSelectionChange = (rows) => {
  if (rows.length > 3) {
    const trimmed = rows.slice(rows.length - 3)
    tableRef.value?.clearSelection()
    trimmed.forEach(r => tableRef.value?.toggleRowSelection(r, true))
    selectedRows.value = trimmed
  } else {
    selectedRows.value = rows
  }
}

const handleCompare = () => {
  if (selectedRows.value.length < 2) {
    ElMessage.warning('请至少选择 2 个岗位进行对比')
    return
  }
  if (selectedRows.value.length > 3) {
    ElMessage.warning('最多选择 3 个岗位进行对比')
    return
  }
  compareDialogRef.value?.open(selectedRows.value)
}

onMounted(() => {
  loadPositions()
})
</script>

<style scoped>
.position-container {
  padding: 20px;
}

h1 {
  color: #303133;
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.education-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.selection-hint {
  color: #F56C6C;
  font-size: 13px;
}
</style>
