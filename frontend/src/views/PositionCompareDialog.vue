<template>
  <el-dialog v-model="visible" title="岗位对比" width="900px" destroy-on-close @close="handleClose">
    <div v-loading="loading">
      <el-table :data="compareRows" border style="width: 100%">
        <el-table-column prop="label" label="对比维度" width="130" fixed />
        <el-table-column
          v-for="(pos, idx) in positions"
          :key="pos.id"
          :label="pos.positionName"
          min-width="180"
        >
          <template #header>
            <span>{{ pos.department }} - {{ pos.positionName }}</span>
            <span style="color: #909399; font-size: 12px; margin-left: 4px">{{ pos.year }}年</span>
          </template>
          <template #default="{ row }">
            <span :class="{ 'diff-cell': row.isDiff && row.isDiff[idx] }">
              <el-icon
                v-if="row.isDiff && row.isDiff[idx]"
                style="vertical-align: middle; margin-right: 2px"
                ><Warning
              /></el-icon>
              {{ row.values[idx] }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="对比结论" min-width="180">
          <template #default="{ row }">
            <span v-if="row.conclusion" class="conclusion-text">{{ row.conclusion }}</span>
            <span v-else class="same-text">相同</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import { getPositionDetail, getPositionStats } from '../api/position'

const visible = ref(false)
const loading = ref(false)
const positions = ref([])
const compareRows = ref([])

const DIMENSIONS = [
  { key: 'department', label: '部门' },
  { key: 'positionName', label: '职位名称' },
  { key: 'educationRequired', label: '学历要求', emptyText: '不限' },
  { key: 'majorRequired', label: '专业要求', emptyText: '不限' },
  { key: 'politicalStatusRequired', label: '政治面貌要求', emptyText: '不限' },
  { key: 'recruitmentNumber', label: '招录人数', suffix: ' 人' },
  { key: 'isFreshOnly', label: '限应届', format: (v) => (v ? '是' : '否') },
  { key: '_latestRatio', label: '最新年报录比' },
]

async function open(selectedPositions) {
  visible.value = true
  loading.value = true
  try {
    // 并行加载每个岗位的详情和报录比
    const detailPromises = selectedPositions.map((p) => getPositionDetail(p.id))
    const detailResults = await Promise.all(detailPromises)
    const detailList = detailResults.map((r) => r.data)

    // 获取全量报录比数据
    const statsRes = await getPositionStats()
    const allStats = statsRes.data || []

    // 为每个岗位计算最新年报录比
    detailList.forEach((pos) => {
      const stats = allStats
        .filter((s) => s.department === pos.department && s.positionName === pos.positionName)
        .sort((a, b) => b.year - a.year)
      if (stats.length > 0 && stats[0].registrationCount && pos.recruitmentNumber) {
        pos._latestRatio = Math.round(stats[0].registrationCount / pos.recruitmentNumber) + ':1'
      } else {
        pos._latestRatio = '—'
      }
    })

    positions.value = detailList

    // 构建对比行
    compareRows.value = DIMENSIONS.map((dim) => {
      const values = detailList.map((pos) => {
        let raw = pos[dim.key]
        if (dim.format) raw = dim.format(raw)
        if ((raw === null || raw === undefined || raw === '') && dim.emptyText) raw = dim.emptyText
        const suffix = dim.suffix || ''
        return raw !== null && raw !== undefined ? String(raw) + suffix : '—'
      })

      const uniqueValues = [...new Set(values)]
      const isDiff = values.length > 1 && uniqueValues.length > 1

      // 生成对比结论
      let conclusion = ''
      if (isDiff) {
        if (dim.key === '_latestRatio') {
          // 找出报录比最低的（竞争最不激烈的）
          const ratios = detailList.map((pos) => {
            const stats = allStats
              .filter((s) => s.department === pos.department && s.positionName === pos.positionName)
              .sort((a, b) => b.year - a.year)
            if (stats.length > 0 && stats[0].registrationCount && pos.recruitmentNumber) {
              return Math.round(stats[0].registrationCount / pos.recruitmentNumber)
            }
            return Infinity
          })
          const minIdx = ratios.indexOf(Math.min(...ratios))
          if (minIdx >= 0 && ratios[minIdx] !== Infinity) {
            conclusion = `${detailList[minIdx].department} 竞争相对较小`
          }
        } else if (dim.key === 'recruitmentNumber') {
          const nums = detailList.map((pos) => pos.recruitmentNumber || 0)
          const maxIdx = nums.indexOf(Math.max(...nums))
          conclusion = `${detailList[maxIdx].department} 招录人数更多`
        } else {
          conclusion = `${dim.label}不同`
        }
      }

      return {
        label: dim.label,
        values,
        isDiff: isDiff
          ? values.map((v) => !values.every((val) => val === v))
          : values.map(() => false),
        conclusion,
      }
    })
  } catch (e) {
    console.error('加载对比数据失败:', e)
  } finally {
    loading.value = false
  }
}

function handleClose() {
  positions.value = []
  compareRows.value = []
}

defineExpose({ open })
</script>

<style scoped>
.diff-cell {
  color: #f56c6c;
  font-weight: 600;
}

.conclusion-text {
  color: #e6a23c;
  font-size: 13px;
}

.same-text {
  color: #67c23a;
  font-size: 13px;
}
</style>
