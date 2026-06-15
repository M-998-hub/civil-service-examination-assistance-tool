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
      <div class="result-header">
        <h2>匹配结果（共 {{ matchList.length }} 个岗位）</h2>
        <el-button link type="primary" @click="apiKeyDialog.open('view')">
          <el-icon><Setting /></el-icon> API Key
        </el-button>
      </div>
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

            <!-- AI 分析区域 -->
            <div v-if="aiResults[item.position.id]" class="ai-analysis">
              <el-divider />
              <div class="ai-header">
                <el-icon color="#409EFF"><MagicStick /></el-icon>
                <strong>AI 分析</strong>
              </div>
              <div class="ai-content">{{ aiResults[item.position.id] }}</div>
            </div>
          </div>
          <div class="card-footer">
            <el-button
              v-if="!aiResults[item.position.id]"
              type="success"
              link
              size="small"
              :loading="aiLoading[item.position.id]"
              @click="handleAiAnalyze(item)"
            >
              <el-icon><MagicStick /></el-icon> AI 分析
            </el-button>
            <el-button
              v-else
              type="warning"
              link
              size="small"
              @click="handleReAnalyze(item)"
            >
              重新分析
            </el-button>
            <el-button type="primary" size="small" @click="handleFavorite(item.position.id)">
              收藏岗位
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-empty v-else-if="!loading && hasMatched" description="暂无匹配的岗位，请完善您的档案信息" />

    <ApiKeyDialog ref="apiKeyDialog" @saved="onKeySaved" />
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Setting } from '@element-plus/icons-vue'
import { recommend } from '../api/match'
import { addFavorite } from '../api/favorite'
import { analyzeMatch, hasApiKey } from '../api/ai'
import ApiKeyDialog from '../components/ApiKeyDialog.vue'

const loading = ref(false)
const hasMatched = ref(false)
const matchList = ref([])
const apiKeyDialog = ref(null)

const aiResults = reactive({})
const aiLoading = reactive({})

const handleMatch = async () => {
  loading.value = true
  hasMatched.value = false
  try {
    const res = await recommend()
    matchList.value = res.data || []
    hasMatched.value = true
    // 清空之前的 AI 分析缓存
    Object.keys(aiResults).forEach(k => delete aiResults[k])
    Object.keys(aiLoading).forEach(k => delete aiLoading[k])
    if (matchList.value.length === 0) {
      ElMessage.info('暂无匹配的岗位，请完善您的档案信息')
    } else {
      ElMessage.success('为您匹配到 ' + matchList.value.length + ' 个岗位')
    }
  } catch (error) {
    console.error('匹配失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAiAnalyze = async (item) => {
  if (!hasApiKey()) {
    apiKeyDialog.value.open('config')
    return
  }
  aiLoading[item.position.id] = true
  try {
    const res = await analyzeMatch({
      positionId: item.position.id,
      matchScore: item.matchScore,
      matchDetails: item.matchDetails,
    })
    aiResults[item.position.id] = res.data?.answer || 'AI 未返回有效分析'
  } catch (error) {
    if (error.code === 'NO_API_KEY') {
      apiKeyDialog.value.open('config')
    } else {
      ElMessage.error('AI 分析失败，请稍后重试')
    }
  } finally {
    aiLoading[item.position.id] = false
  }
}

const handleReAnalyze = (item) => {
  delete aiResults[item.position.id]
  nextTick(() => handleAiAnalyze(item))
}

const onKeySaved = () => {
  ElMessage.success('API Key 已配置，可以尝试 AI 分析了')
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
.match-container { padding: 20px; }
h1 { color: #303133; margin-bottom: 20px; }
.match-card { margin-bottom: 30px; text-align: center; }
.match-header { padding: 40px 0; }
.match-header h2 { margin: 20px 0 10px; color: #303133; }
.match-header p { margin-bottom: 30px; color: #606266; }

.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.result-header h2 { margin: 0; color: #303133; }

.match-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 20px; }
.result-card { transition: all 0.3s ease; }
.result-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; color: #303133; }
.card-content { margin: 15px 0; }
.match-details { margin-top: 15px; }
.detail-tag { margin: 5px 5px 5px 0; }

.ai-analysis { margin-top: 12px; }
.ai-header { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #303133; margin-bottom: 8px; }
.ai-content { background: #f0f9ff; border-left: 3px solid #409EFF; padding: 12px 16px; border-radius: 4px; font-size: 14px; color: #303133; line-height: 1.8; white-space: pre-wrap; }

.card-footer { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 15px; }
</style>
