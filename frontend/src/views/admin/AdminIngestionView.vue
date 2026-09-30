<template>
  <div class="ingestion-page">
    <h2>官方岗位自动采集</h2>
    <el-alert type="warning" :closable="false" show-icon>
      <template #title
        >仅支持配置的国考官方域名；采集结果须人工核对后发布。原有 Excel 手动导入仍可使用。</template
      >
    </el-alert>
    <el-card class="panel">
      <el-form :inline="true" :model="form">
        <el-form-item label="招录年份"
          ><el-input-number v-model="form.year" :min="2020" :max="2100"
        /></el-form-item>
        <el-form-item label="官方附件直链（可选）">
          <el-input
            v-model="form.officialUrl"
            placeholder="留空则从官方入口自动发现"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item
          ><el-button type="primary" :loading="starting" @click="start"
            >开始采集</el-button
          ></el-form-item
        >
      </el-form>
      <p class="hint">
        自动发现受官网页面结构影响；发现失败时，可从官网复制 .xls/.xlsx/.zip 附件地址重试。
      </p>
    </el-card>

    <el-card class="panel">
      <template #header
        ><div class="card-head">
          <span>采集批次</span><el-button @click="loadRuns">刷新</el-button>
        </div></template
      >
      <el-table :data="runs" v-loading="loading" stripe>
        <el-table-column prop="id" label="批次" width="80" />
        <el-table-column prop="year" label="年份" width="85" />
        <el-table-column prop="state" label="状态" width="120" />
        <el-table-column prop="candidate_count" label="差异数" width="85" />
        <el-table-column prop="source_url" label="来源" min-width="280" show-overflow-tooltip />
        <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="scope"
            ><el-button link type="primary" @click="view(scope.row)">查看差异</el-button></template
          >
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selected" class="panel">
      <template #header>
        <div class="card-head">
          <span>批次 #{{ selected.id }}：{{ selected.state }}（{{ candidateTotal }} 条差异）</span>
          <div v-if="selected.state === 'READY'">
            <el-button type="danger" plain @click="reject">驳回</el-button>
            <el-button type="success" :loading="publishing" @click="publish"
              >审核并发布整批</el-button
            >
          </div>
        </div>
      </template>
      <el-table :data="candidates" height="500" stripe>
        <el-table-column type="expand" width="45">
          <template #default="scope">
            <div class="diff-details">
              <div v-for="field in diffFields" :key="field.key">
                <strong>{{ field.label }}：</strong>
                <span>{{ scope.row.previous?.[field.key] ?? '—' }}</span>
                <span class="diff-arrow">→</span>
                <span>{{
                  scope.row.change_type === 'WITHDRAW'
                    ? '已撤销'
                    : (scope.row.data[field.key] ?? '—')
                }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="change_type" label="变更" width="100" />
        <el-table-column prop="position_code" label="职位代码" width="160" />
        <el-table-column label="部门" min-width="180"
          ><template #default="scope">{{ scope.row.data.department }}</template></el-table-column
        >
        <el-table-column label="职位" min-width="180"
          ><template #default="scope">{{ scope.row.data.positionName }}</template></el-table-column
        >
        <el-table-column label="人数" width="75"
          ><template #default="scope">{{
            scope.row.data.recruitmentNumber
          }}</template></el-table-column
        >
        <el-table-column label="专业 / 学历" min-width="240">
          <template #default="scope"
            >{{ scope.row.data.majorRequired }} / {{ scope.row.data.educationRequired }}</template
          >
        </el-table-column>
        <el-table-column label="原记录" min-width="240" show-overflow-tooltip>
          <template #default="scope">{{
            scope.row.previous
              ? `${scope.row.previous.positionName} · ${scope.row.previous.recruitmentNumber} 人`
              : '新增'
          }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="candidateTotal > pageSize"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="candidateTotal"
        layout="prev, pager, next, total"
        @current-change="loadCandidates"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listIngestionRuns,
  listIngestionCandidates,
  startIngestion,
  publishIngestion,
  rejectIngestion,
} from '../../api/ingestion'

const form = reactive({ year: new Date().getFullYear(), officialUrl: '' })
const runs = ref([])
const candidates = ref([])
const candidateTotal = ref(0)
const withdrawalCount = ref(0)
const pageNum = ref(1)
const pageSize = 100
const selected = ref(null)
const loading = ref(false)
const starting = ref(false)
const publishing = ref(false)
const diffFields = [
  { key: 'department', label: '部门' },
  { key: 'positionName', label: '职位' },
  { key: 'majorRequired', label: '专业' },
  { key: 'educationRequired', label: '学历' },
  { key: 'politicalStatusRequired', label: '政治面貌' },
  { key: 'isFreshOnly', label: '仅限应届' },
  { key: 'recruitmentNumber', label: '招录人数' },
]

async function loadRuns() {
  loading.value = true
  try {
    runs.value = (await listIngestionRuns()).data || []
  } finally {
    loading.value = false
  }
}

async function start() {
  starting.value = true
  try {
    const res = await startIngestion({
      year: form.year,
      officialUrl: form.officialUrl.trim() || null,
    })
    ElMessage.success(`采集任务 #${res.data} 已创建，请稍后刷新查看结果`)
    await loadRuns()
  } finally {
    starting.value = false
  }
}

async function view(row) {
  selected.value = row
  pageNum.value = 1
  await loadCandidates()
}

async function loadCandidates() {
  const res = await listIngestionCandidates(selected.value.id, pageNum.value, pageSize)
  candidateTotal.value = res.data.total || 0
  withdrawalCount.value = res.data.withdrawalCount || 0
  candidates.value = (res.data.records || []).map((item) => ({
    ...item,
    data: JSON.parse(item.data_json),
    previous: item.previous_json ? JSON.parse(item.previous_json) : null,
  }))
}

async function publish() {
  await ElMessageBox.confirm(
    `确认发布批次 #${selected.value.id} 的 ${candidateTotal.value} 条差异？其中 ${withdrawalCount.value} 条将被标记撤销。请先核对所有差异页。`,
    '人工审核确认',
    { type: 'warning' },
  )
  publishing.value = true
  try {
    await publishIngestion(selected.value.id)
    ElMessage.success('已提交后台发布任务，请刷新批次状态查看结果')
    selected.value = null
    await loadRuns()
  } finally {
    publishing.value = false
  }
}

async function reject() {
  await ElMessageBox.confirm(`确认驳回批次 #${selected.value.id}？`, '驳回批次', {
    type: 'warning',
  })
  await rejectIngestion(selected.value.id)
  ElMessage.success('批次已驳回')
  selected.value = null
  await loadRuns()
}

onMounted(loadRuns)
</script>

<style scoped>
.ingestion-page {
  display: grid;
  gap: 16px;
}
.ingestion-page h2 {
  margin: 0;
}
.panel {
  margin: 0;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  color: #777;
  margin: 0;
  font-size: 13px;
}
.diff-details {
  display: grid;
  gap: 8px;
  padding: 12px 40px;
}
.diff-arrow {
  padding: 0 12px;
  color: #888;
}
</style>
