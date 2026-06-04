<template>
  <el-dialog
    v-model="visible"
    title="岗位详情"
    width="680px"
    destroy-on-close
    @close="handleClose"
  >
    <div v-loading="loading">
      <!-- 基础信息 -->
      <el-descriptions :column="2" border v-if="position">
        <el-descriptions-item label="部门">{{ position.department }}</el-descriptions-item>
        <el-descriptions-item label="职位名称">{{ position.positionName }}</el-descriptions-item>
        <el-descriptions-item label="年份">{{ position.year }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ position.educationRequired || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="专业要求">{{ position.majorRequired || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="政治面貌要求">{{ position.politicalStatusRequired || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="招录人数">{{ position.recruitmentNumber ?? '—' }} 人</el-descriptions-item>
        <el-descriptions-item label="报名截止时间">{{ formatDate(position.registrationDeadline) }}</el-descriptions-item>
        <el-descriptions-item label="限应届">{{ position.isFreshOnly ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 报录比趋势 -->
      <div style="margin-top: 20px">
        <h4 style="margin: 0 0 8px 0; color: #303133">报录比趋势</h4>
        <div v-if="statsList.length === 0" class="empty-chart">
          <el-empty description="暂无报录比数据" :image-size="60" />
        </div>
        <div v-else ref="chartRef" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { getPositionDetail, getPositionStats } from '../api/position'
import { createStatsTrendChart } from '../utils/statsChart'

const props = defineProps({
  positionId: { type: Number, default: null }
})

const visible = ref(false)
const loading = ref(false)
const position = ref(null)
const statsList = ref([])
const chartRef = ref(null)

let chartInstance = null

async function open(id) {
  visible.value = true
  loading.value = true
  try {
    const [detailRes, statsRes] = await Promise.all([
      getPositionDetail(id),
      getPositionStats() // 全量加载，后续按部门/职位过滤
    ])
    position.value = detailRes.data
    const allStats = statsRes.data || []
    statsList.value = allStats
      .filter(s => s.department === position.value.department && s.positionName === position.value.positionName)
      .sort((a, b) => a.year - b.year)

    if (statsList.value.length > 0) {
      await nextTick()
      chartInstance = createStatsTrendChart(chartRef.value, statsList.value, 200)
    }
  } catch (e) {
    console.error('加载岗位详情失败:', e)
  } finally {
    loading.value = false
  }
}

function handleClose() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  position.value = null
  statsList.value = []
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

defineExpose({ open })
</script>

<style scoped>
.empty-chart {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
