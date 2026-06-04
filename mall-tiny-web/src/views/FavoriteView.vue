<template>
  <div class="favorite-container">
    <h1>我的收藏</h1>
    <el-card class="favorite-card">
      <el-table :data="favoriteList" style="width: 100%" v-loading="loading">
        <el-table-column prop="positionName" label="岗位名称" min-width="150" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="year" label="年份" width="80" />
        <el-table-column prop="educationRequired" label="学历要求" width="100" />
        <el-table-column prop="recruitmentNumber" label="招录人数" width="100" />
        <el-table-column prop="favoriteTime" label="收藏时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.favoriteTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button size="small" type="danger" @click="handleRemove(scope.row.positionId)">
              取消收藏
            </el-button>
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
          @size-change="loadFavorites"
          @current-change="loadFavorites"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavoriteList, removeFavorite } from '../api/favorite'

const favoriteList = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavoriteList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    favoriteList.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleRemove = async (positionId) => {
  try {
    await removeFavorite(positionId)
    ElMessage.success('取消收藏成功')
    loadFavorites()
  } catch (error) {
    console.error('取消收藏失败:', error)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorite-container {
  padding: 20px;
}

h1 {
  color: #303133;
  margin-bottom: 20px;
}

.favorite-card {
  margin-top: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
