<template>
  <div class="dashboard-container">
    <h1>数据看板</h1>

    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" label-width="80px">
        <el-form-item label="部门">
          <el-select
            v-model="selectedDepartment"
            filterable
            clearable
            placeholder="请选择部门"
            style="width: 220px"
            @change="onDepartmentChange"
          >
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位名称">
          <el-select
            v-model="selectedPositionName"
            filterable
            clearable
            placeholder="请先选择部门"
            style="width: 220px"
            :disabled="!selectedDepartment"
          >
            <el-option v-for="name in positionNames" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="querying"
            :disabled="!selectedDepartment || !selectedPositionName"
            @click="handleQuery"
          >
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 竞争热度预警（仅查询后显示） -->
    <el-alert
      v-if="warningInfo"
      :type="warningInfo.type"
      :title="warningInfo.title"
      :description="warningInfo.description"
      class="warning-alert"
      show-icon
      :closable="false"
    />

    <!-- 趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <span class="card-title">历年报录比趋势</span>
      </template>
      <div v-if="queryStats.length === 0" class="empty-area">
        <el-empty
          :description="hasQueried ? '暂无该岗位的报录比数据' : '请选择部门和职位名称后查询'"
        />
      </div>
      <div v-else ref="chartRef" style="height: 420px" />
    </el-card>

    <!-- 近3年数据卡片 -->
    <div v-if="recentStats.length > 0" class="stats-row">
      <el-card v-for="stat in recentStats" :key="stat.year" class="stat-card" shadow="hover">
        <div class="stat-year">{{ stat.year }} 年</div>
        <el-divider />
        <div class="stat-item">
          <span class="stat-label">报名人数</span>
          <span class="stat-value primary">{{ stat.registrationCount ?? '—' }} 人</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">进面分区间</span>
          <span class="stat-value">
            {{ stat.minEntryScore ?? '—' }} ~ {{ stat.maxEntryScore ?? '—' }}
          </span>
        </div>
        <div class="stat-item">
          <span class="stat-label">报录比</span>
          <span class="stat-value" :class="getRatioClass(stat)">
            {{ formatRatio(stat) }}
          </span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getPositionStats, filterPositions } from '../api/position'

// ─── 状态 ───────────────────────────────────────────────
const allStats = ref([]) // 全量 stats（用于下拉选项）
const queryStats = ref([]) // 当前查询结果
const recruitmentMap = ref({}) // year → recruitmentNumber
const selectedDepartment = ref('')
const selectedPositionName = ref('')
const querying = ref(false)
const hasQueried = ref(false)
const loading = ref(false)

let chartInstance = null
const chartRef = ref(null)

// ─── 下拉选项（从全量数据前端提取） ──────────────────────
const departments = computed(() => {
  const set = new Set(allStats.value.map((s) => s.department).filter(Boolean))
  return [...set].sort()
})

const positionNames = computed(() => {
  if (!selectedDepartment.value) return []
  const set = new Set(
    allStats.value
      .filter((s) => s.department === selectedDepartment.value)
      .map((s) => s.positionName)
      .filter(Boolean),
  )
  return [...set].sort()
})

// ─── 近3年数据 ────────────────────────────────────────────
const recentStats = computed(() => {
  return [...queryStats.value].sort((a, b) => b.year - a.year).slice(0, 3)
})

// ─── 竞争热度预警 ──────────────────────────────────────────
const warningInfo = computed(() => {
  if (queryStats.value.length === 0) return null
  const latest = [...queryStats.value].sort((a, b) => b.year - a.year)[0]
  const recruitment = recruitmentMap.value[latest.year]
  if (!recruitment || !latest.registrationCount) {
    return {
      type: 'info',
      title: `${latest.year} 年报名人数：${latest.registrationCount ?? '暂无'} 人`,
      description: '暂无招录人数数据，无法计算精确报录比',
    }
  }
  const ratio = Math.round(latest.registrationCount / recruitment)
  if (ratio >= 100) {
    return {
      type: 'error',
      title: `极高竞争预警：报录比约 ${ratio}:1，请谨慎报考`,
      description: `${latest.year} 年报名 ${latest.registrationCount} 人，招录 ${recruitment} 人`,
    }
  } else if (ratio >= 30) {
    return {
      type: 'warning',
      title: `竞争激烈：报录比约 ${ratio}:1，建议充分备考`,
      description: `${latest.year} 年报名 ${latest.registrationCount} 人，招录 ${recruitment} 人`,
    }
  } else {
    return {
      type: 'success',
      title: `竞争适中：报录比约 ${ratio}:1，机会相对较好`,
      description: `${latest.year} 年报名 ${latest.registrationCount} 人，招录 ${recruitment} 人`,
    }
  }
})

// ─── 数据卡片辅助函数 ──────────────────────────────────────
function formatRatio(stat) {
  const recruitment = recruitmentMap.value[stat.year]
  if (!recruitment || !stat.registrationCount) return '—'
  const ratio = Math.round(stat.registrationCount / recruitment)
  return `${ratio}:1`
}

function getRatioClass(stat) {
  const recruitment = recruitmentMap.value[stat.year]
  if (!recruitment || !stat.registrationCount) return ''
  const ratio = Math.round(stat.registrationCount / recruitment)
  if (ratio >= 100) return 'ratio-high'
  if (ratio >= 30) return 'ratio-mid'
  return 'ratio-low'
}

// ─── 事件处理 ──────────────────────────────────────────────
function onDepartmentChange() {
  selectedPositionName.value = ''
}

function handleReset() {
  selectedDepartment.value = ''
  selectedPositionName.value = ''
  queryStats.value = []
  recruitmentMap.value = {}
  hasQueried.value = false
  if (chartInstance) {
    chartInstance.clear()
  }
}

async function handleQuery() {
  if (!selectedDepartment.value || !selectedPositionName.value) {
    ElMessage.warning('请选择部门和职位名称')
    return
  }
  querying.value = true
  try {
    const [statsRes, posRes] = await Promise.all([
      getPositionStats({
        department: selectedDepartment.value,
        positionName: selectedPositionName.value,
      }),
      filterPositions({
        department: selectedDepartment.value,
        positionName: selectedPositionName.value,
        pageSize: 100,
        pageNum: 1,
      }),
    ])

    queryStats.value = (statsRes.data || []).sort((a, b) => a.year - b.year)

    // 构建 year → recruitmentNumber 映射
    const map = {}
    const positions = posRes.data?.records || posRes.data || []
    positions.forEach((p) => {
      if (p.year && p.recruitmentNumber) {
        map[p.year] = p.recruitmentNumber
      }
    })
    recruitmentMap.value = map

    hasQueried.value = true

    if (queryStats.value.length > 0) {
      await nextTick()
      renderChart()
    }
  } catch (e) {
    console.error('查询失败:', e)
    ElMessage.error('查询失败，请稍后再试')
  } finally {
    querying.value = false
  }
}

// ─── ECharts 渲染 ──────────────────────────────────────────
function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const stats = queryStats.value
  const years = stats.map((s) => String(s.year))
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['报名人数', '最低进面分', '最高进面分'],
      bottom: 0,
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '12%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: years,
      axisLabel: { formatter: '{value} 年' },
    },
    yAxis: [
      {
        type: 'value',
        name: '报名人数（人）',
        nameTextStyle: { color: '#409EFF' },
        axisLabel: { color: '#409EFF' },
      },
      {
        type: 'value',
        name: '进面分数',
        nameTextStyle: { color: '#E6A23C' },
        axisLabel: { color: '#E6A23C' },
        min: (val) => Math.max(0, Math.floor(val.min * 0.95)),
      },
    ],
    series: [
      {
        name: '报名人数',
        type: 'bar',
        yAxisIndex: 0,
        data: stats.map((s) => s.registrationCount ?? null),
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 60,
      },
      {
        name: '最低进面分',
        type: 'line',
        yAxisIndex: 1,
        data: stats.map((s) => s.minEntryScore ?? null),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#E6A23C', width: 2 },
        itemStyle: { color: '#E6A23C' },
      },
      {
        name: '最高进面分',
        type: 'line',
        yAxisIndex: 1,
        data: stats.map((s) => s.maxEntryScore ?? null),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#F56C6C', width: 2 },
        itemStyle: { color: '#F56C6C' },
      },
    ],
  }
  chartInstance.setOption(option, true)
}

// ─── 窗口 resize ────────────────────────────────────────────
function handleResize() {
  chartInstance?.resize()
}

// ─── 生命周期 ──────────────────────────────────────────────
onMounted(async () => {
  loading.value = true
  try {
    const res = await getPositionStats()
    allStats.value = res.data || []
  } catch (e) {
    console.error('加载下拉数据失败:', e)
    ElMessage.error('加载部门数据失败，请刷新重试')
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.dashboard-container {
  padding-bottom: 20px;
}

.dashboard-container h1 {
  margin: 0 0 16px 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.filter-card {
  margin-bottom: 16px;
}

.warning-alert {
  margin-bottom: 16px;
}

.chart-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.empty-area {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 近3年数据卡片 */
.stats-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 200px;
}

.stat-year {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  text-align: center;
  padding: 4px 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stat-value.primary {
  color: #409eff;
}

.ratio-high {
  color: #f56c6c;
}

.ratio-mid {
  color: #e6a23c;
}

.ratio-low {
  color: #67c23a;
}
</style>
