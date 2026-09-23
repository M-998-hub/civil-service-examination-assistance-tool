<template>
  <div class="home-container">
    <!-- 欢迎区 -->
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ username }}</h2>
          <p class="welcome-subtitle">欢迎使用考公选岗系统，为您智能匹配最适合的岗位</p>
          <div class="archive-progress" v-if="archiveLoaded">
            <span class="progress-label">档案完整度</span>
            <el-progress
              :percentage="archiveCompleteness"
              :color="archiveCompleteness === 100 ? '#67C23A' : '#E6A23C'"
              :stroke-width="8"
              style="width: 260px"
            />
            <span class="progress-text">{{ completenessText }}</span>
            <el-button
              v-if="archiveCompleteness < 100"
              type="warning"
              size="small"
              link
              @click="goArchive"
            >
              去完善 &rarr;
            </el-button>
          </div>
        </div>
        <div class="welcome-icon">
          <el-icon :size="72" color="#409EFF"><HomeFilled /></el-icon>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片行 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card" @click="goPosition">
          <div class="stat-inner">
            <div class="stat-icon" style="background-color: #ecf5ff">
              <el-icon :size="32" color="#409EFF"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <template v-if="statsLoading.position">&mdash;</template>
                <template v-else>{{ positionTotal }}</template>
              </div>
              <div class="stat-label">可选岗位总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card" @click="goFavorite">
          <div class="stat-inner">
            <div class="stat-icon" style="background-color: #fdf6ec">
              <el-icon :size="32" color="#E6A23C"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <template v-if="statsLoading.favorite">&mdash;</template>
                <template v-else>{{ favoriteTotal }}</template>
              </div>
              <div class="stat-label">我的收藏</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card" @click="goMatch">
          <div class="stat-inner">
            <div class="stat-icon" style="background-color: #f0f9eb">
              <el-icon :size="32" color="#67C23A"><MagicStick /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <template v-if="statsLoading.match">&mdash;</template>
                <template v-else>{{ matchTotal }}</template>
              </div>
              <div class="stat-label">匹配岗位数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口卡片区 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <span class="section-title">快捷功能</span>
      </template>
      <el-row :gutter="16">
        <el-col
          v-for="entry in quickEntries"
          :key="entry.route"
          :xs="12"
          :sm="8"
          :md="6"
          :lg="entry.span || 4"
          class="entry-col"
        >
          <div
            class="entry-card"
            :class="{ 'entry-highlight': entry.highlight }"
            @click="goRoute(entry.route)"
          >
            <div class="entry-icon" :style="{ backgroundColor: entry.bgColor }">
              <el-icon :size="28" :color="entry.color">
                <component :is="entry.icon" />
              </el-icon>
            </div>
            <div class="entry-info">
              <div class="entry-title">
                {{ entry.title }}
                <el-badge v-if="entry.highlight" value="待完善" class="entry-badge" />
              </div>
              <div class="entry-desc">{{ entry.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- AI 选岗助理 -->
    <el-card class="section-card ai-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span class="section-title">AI 选岗助理</span>
          <el-button link type="primary" size="small" @click="apiKeyDialog.open('view')">
            <el-icon><Setting /></el-icon>
          </el-button>
        </div>
      </template>
      <div class="ai-input-row">
        <el-input
          v-model="aiQuestion"
          placeholder="输入问题，例如：计算机专业硕士有什么推荐岗位？"
          @keyup.enter="handleAiAsk"
          :disabled="aiAsking"
        />
        <el-button type="primary" :loading="aiAsking" @click="handleAiAsk">发送</el-button>
      </div>
      <div v-if="aiAnswer" class="ai-answer">
        <div class="ai-answer-header">
          <el-icon color="#409EFF"><MagicStick /></el-icon>
          <span>AI 回复</span>
          <el-button link size="small" type="danger" @click="aiAnswer = ''">清除</el-button>
        </div>
        <div class="ai-answer-content">{{ aiAnswer }}</div>
      </div>
    </el-card>

    <!-- 档案摘要 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span class="section-title">档案摘要</span>
          <el-button type="primary" link size="small" @click="goArchive">
            编辑档案 &rarr;
          </el-button>
        </div>
      </template>
      <template v-if="hasArchive">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="专业">
            <template v-if="archive.major">
              <el-tag size="small">{{ archive.major }}</el-tag>
            </template>
            <template v-else>
              <span class="empty-field">未填写</span>
            </template>
          </el-descriptions-item>
          <el-descriptions-item label="学历">
            <template v-if="archive.education">
              <el-tag size="small" type="success">{{ archive.education }}</el-tag>
            </template>
            <template v-else>
              <span class="empty-field">未填写</span>
            </template>
          </el-descriptions-item>
          <el-descriptions-item label="政治面貌">
            <template v-if="archive.politicalStatus">
              <el-tag size="small" type="warning">{{ archive.politicalStatus }}</el-tag>
            </template>
            <template v-else>
              <span class="empty-field">未填写</span>
            </template>
          </el-descriptions-item>
          <el-descriptions-item label="是否应届">
            <el-tag size="small" :type="archive.isFreshGraduate ? '' : 'info'">
              {{ archive.isFreshGraduate ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="尚未填写个人档案，完善后可获得精准岗位推荐">
        <el-button type="primary" @click="goArchive">去完善档案</el-button>
      </el-empty>
    </el-card>
  </div>

  <ApiKeyDialog ref="apiKeyDialog" />
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Star,
  MagicStick,
  HomeFilled,
  Search,
  DataAnalysis,
  Setting,
} from '@element-plus/icons-vue'
import { getAdminInfo } from '../api/auth'
import { getMyArchive } from '../api/archive'
import { filterPositions } from '../api/position'
import { getFavoriteList } from '../api/favorite'
import { recommend } from '../api/match'
import { aiAsk, hasApiKey } from '../api/ai'
import ApiKeyDialog from '../components/ApiKeyDialog.vue'

const router = useRouter()

const username = ref('用户')
const archiveLoaded = ref(false)
const archive = reactive({
  major: '',
  education: '',
  politicalStatus: '',
  isFreshGraduate: false,
})

const positionTotal = ref(0)
const favoriteTotal = ref(0)
const matchTotal = ref('--')

const apiKeyDialog = ref(null)
const aiQuestion = ref('')
const aiAsking = ref(false)
const aiAnswer = ref('')

const statsLoading = reactive({
  position: true,
  favorite: true,
  match: true,
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const archiveCompleteness = computed(() => {
  if (!archiveLoaded.value) return 0
  let filled = 0
  if (archive.major) filled++
  if (archive.education) filled++
  if (archive.politicalStatus) filled++
  if (archive.isFreshGraduate !== null && archive.isFreshGraduate !== undefined) filled++
  return Math.round((filled / 4) * 100)
})

const completenessText = computed(() => {
  if (archiveCompleteness.value === 100) return '已完成'
  if (archiveCompleteness.value >= 50) return '待完善'
  return '未填写'
})

const hasArchive = computed(() => {
  return archive.major || archive.education || archive.politicalStatus
})

const quickEntries = computed(() => [
  {
    title: '个人档案',
    desc: '完善专业、学历等基本信息',
    route: '/archive',
    icon: HomeFilled,
    color: '#409EFF',
    bgColor: '#ecf5ff',
    highlight: archiveLoaded.value && archiveCompleteness.value < 100,
  },
  {
    title: '岗位列表',
    desc: '浏览所有岗位，支持筛选对比',
    route: '/position',
    icon: Search,
    color: '#67C23A',
    bgColor: '#f0f9eb',
    highlight: false,
  },
  {
    title: '一键匹配',
    desc: '基于档案智能推荐最适配岗位',
    route: '/match',
    icon: MagicStick,
    color: '#E6A23C',
    bgColor: '#fdf6ec',
    highlight: false,
    span: 6,
  },
  {
    title: '我的收藏',
    desc: '管理已收藏的心仪岗位',
    route: '/favorite',
    icon: Star,
    color: '#F56C6C',
    bgColor: '#fef0f0',
    highlight: false,
  },
  {
    title: '数据看板',
    desc: '查看历年报录比趋势分析',
    route: '/dashboard',
    icon: DataAnalysis,
    color: '#909399',
    bgColor: '#f4f4f5',
    highlight: false,
    span: 6,
  },
])

const goRoute = (route) => {
  router.push(route)
}

const goArchive = () => {
  router.push('/archive')
}

const goPosition = () => {
  router.push('/position')
}

const goFavorite = () => {
  router.push('/favorite')
}

const goMatch = () => {
  router.push('/match')
}

const loadUserInfo = async () => {
  try {
    const res = await getAdminInfo()
    if (res.data && res.data.username) {
      username.value = res.data.username
    }
  } catch {
    // 静默失败
  }
}

const loadArchive = async () => {
  try {
    const res = await getMyArchive()
    if (res.data) {
      archive.major = res.data.major || ''
      archive.education = res.data.education || ''
      archive.politicalStatus = res.data.politicalStatus || ''
      archive.isFreshGraduate = res.data.isFreshGraduate ?? false
    }
  } catch {
    // 静默失败
  } finally {
    archiveLoaded.value = true
  }
}

const loadStats = async () => {
  try {
    const posRes = await filterPositions({ pageNum: 1, pageSize: 1 })
    positionTotal.value = posRes.data?.total ?? 0
  } catch {
    positionTotal.value = 0
  } finally {
    statsLoading.position = false
  }

  try {
    const favRes = await getFavoriteList({ pageNum: 1, pageSize: 1 })
    favoriteTotal.value = favRes.data?.total ?? 0
  } catch {
    favoriteTotal.value = 0
  } finally {
    statsLoading.favorite = false
  }

  try {
    const matchRes = await recommend()
    matchTotal.value = matchRes.data?.length ?? 0
  } catch {
    matchTotal.value = '--'
  } finally {
    statsLoading.match = false
  }
}

const handleAiAsk = async () => {
  const q = aiQuestion.value.trim()
  if (!q) return
  if (!hasApiKey()) {
    apiKeyDialog.value.open('config')
    return
  }
  aiAsking.value = true
  aiAnswer.value = ''
  try {
    const res = await aiAsk({ question: q })
    aiAnswer.value = res.data?.answer || 'AI not return valid reply'
  } catch (error) {
    if (error.code === 'NO_API_KEY') {
      apiKeyDialog.value.open('config')
    } else {
      ElMessage.error('AI not available，please retry later')
    }
  } finally {
    aiAsking.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  loadArchive()
  loadStats()
})
</script>

<style scoped>
.home-container {
  padding-bottom: 20px;
}

.welcome-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9ff 100%);
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.welcome-subtitle {
  margin: 0 0 16px 0;
  color: #606266;
  font-size: 14px;
}

.archive-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.progress-text {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.welcome-icon {
  opacity: 0.6;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.section-card {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.entry-col {
  margin-bottom: 16px;
}

.entry-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
}

.entry-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.entry-highlight {
  border-color: #e6a23c;
  background-color: #fdf6ec;
}

.entry-highlight:hover {
  border-color: #e6a23c;
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.2);
}

.entry-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.entry-info {
  flex: 1;
  min-width: 0;
}

.entry-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.entry-badge {
  margin-left: 4px;
}

.entry-desc {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-field {
  color: #c0c4cc;
  font-size: 13px;
}

/* AI 助理卡片 */
.ai-card {
  border: 1px solid #d9ecff;
}
.ai-input-row {
  display: flex;
  gap: 10px;
}
.ai-input-row .el-input {
  flex: 1;
}
.ai-answer {
  margin-top: 16px;
}
.ai-answer-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}
.ai-answer-header .el-button {
  margin-left: auto;
}
.ai-answer-content {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
</style>
