<template>
  <div class="match-container">
    <h1>一键匹配</h1>
    <el-card class="match-card">
      <div class="match-header">
        <el-icon :size="48" color="#409EFF"><MagicStick /></el-icon>
        <h2>智能岗位匹配</h2>
        <p>基于您的档案信息，为您推荐最适合的岗位</p>
        <el-button type="primary" size="large" @click="handleMatch" :loading="loading">
          开始匹配
        </el-button>
      </div>
    </el-card>

    <div v-if="matchList.length > 0" class="match-result">
      <h2>匹配结果（共 {{ matchList.length }} 个岗位）</h2>
      <div class="match-cards">
        <el-card
          v-for="item in matchList"
          :key="item.position.id"
          class="result-card"
          :body-style="{ padding: '20px' }"
        >
          <template #header>
            <div class="card-header">
              <h3>{{ item.position.positionName }}</h3>
              <el-tag :type="getScoreType(item.matchScore)" size="large">
                匹配度: {{ item.matchScore }}分
              </el-tag>
            </div>
          </template>
          <div class="card-content">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="部门">{{ item.position.department }}</el-descriptions-item>
              <el-descriptions-item label="年份">{{ item.position.year }}</el-descriptions-item>
              <el-descriptions-item label="学历要求">{{ item.position.educationRequired }}</el-descriptions-item>
              <el-descriptions-item label="政治面貌要求">{{ item.position.politicalStatusRequired }}</el-descriptions-item>
              <el-descriptions-item label="招录人数">{{ item.position.recruitmentNumber }}</el-descriptions-item>
              <el-descriptions-item label="专业要求" :span="2">{{ item.position.majorRequired || '不限' }}</el-descriptions-item>
            </el-descriptions>
            <div class="match-details">
              <strong>匹配详情：</strong>
              <el-tag v-for="(detail, index) in item.matchDetails" :key="index" class="detail-tag" type="success">
                {{ detail }}
              </el-tag>
            </div>
          </div>
          <div class="card-footer">
            <el-button type="primary" size="small" @click="handleFavorite(item.position.id)">
              收藏岗位
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-empty v-else-if="!loading && hasMatched" description="暂无匹配的岗位，请完善您的档案信息" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { recommend } from '../api/match'
import { addFavorite } from '../api/favorite'

const loading = ref(false)
const hasMatched = ref(false)
const matchList = ref([])

const handleMatch = async () => {
  loading.value = true
  hasMatched.value = false
  try {
    const res = await recommend()
    matchList.value = res.data || []
    hasMatched.value = true
    if (matchList.value.length === 0) {
      ElMessage.info('暂无匹配的岗位，请完善您的档案信息')
    } else {
      ElMessage.success(`为您匹配到 ${matchList.value.length} 个岗位`)
    }
  } catch (error) {
    console.error('匹配失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFavorite = async (positionId) => {
  try {
    await addFavorite(positionId)
    ElMessage.success('收藏成功')
  } catch (error) {
    console.error('收藏失败:', error)
  }
}

const getScoreType = (score) => {
  if (score >= 40) return 'success'
  if (score >= 25) return 'warning'
  return 'info'
}
</script>

<style scoped>
.match-container {
  padding: 20px;
}

h1 {
  color: #303133;
  margin-bottom: 20px;
}

.match-card {
  margin-bottom: 30px;
  text-align: center;
}

.match-header {
  padding: 40px 0;
}

.match-header h2 {
  margin: 20px 0 10px;
  color: #303133;
}

.match-header p {
  margin-bottom: 30px;
  color: #606266;
}

.match-result h2 {
  margin-bottom: 20px;
  color: #303133;
}

.match-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.result-card {
  transition: all 0.3s ease;
}

.result-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: #303133;
}

.card-content {
  margin: 15px 0;
}

.match-details {
  margin-top: 15px;
}

.detail-tag {
  margin: 5px 5px 5px 0;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
}
</style>
