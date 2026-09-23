<template>
  <el-dialog
    v-model="visible"
    :title="'分配用户 - ' + (role?.name || '')"
    width="650px"
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名" clearable style="width: 200px" />
      <el-button type="primary" size="small" @click="checkAll">全选</el-button>
      <el-button size="small" @click="uncheckAll">全不选</el-button>
    </div>
    <el-table ref="tableRef" :data="filtered" height="350" @selection-change="onSelChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickName" label="昵称" />
    </el-table>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, getAdminRoles } from '../../api/user'
import { grantRole, revokeUserRole } from '../../api/role'

const emit = defineEmits(['saved'])
const visible = ref(false)
const role = ref(null)
const allUsers = ref([])
const originalIds = ref([])
const selectedIds = ref([])
const keyword = ref('')
const saving = ref(false)
const tableRef = ref(null)

const filtered = computed(() => {
  if (!keyword.value) return allUsers.value
  const kw = keyword.value.toLowerCase()
  return allUsers.value.filter(
    (u) =>
      (u.username || '').toLowerCase().includes(kw) ||
      (u.nickName || '').toLowerCase().includes(kw),
  )
})

const onSelChange = (rows) => {
  selectedIds.value = rows.map((r) => r.id)
}
const checkAll = () => {
  tableRef.value?.toggleAllSelection()
}
const uncheckAll = () => {
  tableRef.value?.clearSelection()
}

const open = async (r) => {
  role.value = r
  keyword.value = ''
  selectedIds.value = []
  visible.value = true
  try {
    const userRes = await getUserList()
    allUsers.value = userRes.data?.list || []
    // 构建角色-用户映射：遍历每个用户获取其角色
    const ids = []
    for (const u of allUsers.value) {
      try {
        const res = await getAdminRoles(u.id)
        const roles = res.data || []
        if (roles.some((ro) => ro.id === r.id)) ids.push(u.id)
      } catch {
        /* ignore */
      }
    }
    originalIds.value = [...ids]
    // 设置默认勾选
    await nextTick()
    allUsers.value.forEach((u) => {
      if (ids.includes(u.id)) tableRef.value?.toggleRowSelection(u, true)
    })
  } catch (e) {
    console.error('加载失败', e)
    ElMessage.error('加载数据失败')
  }
}

const handleSave = async () => {
  if (!role.value) return
  saving.value = true
  try {
    const toAdd = selectedIds.value.filter((id) => !originalIds.value.includes(id))
    const toRemove = originalIds.value.filter((id) => !selectedIds.value.includes(id))
    for (const adminId of toAdd) await grantRole({ roleId: role.value.id, adminId })
    for (const adminId of toRemove) await revokeUserRole({ roleId: role.value.id, adminId })
    originalIds.value = [...selectedIds.value]
    ElMessage.success('保存成功')
    visible.value = false
    emit('saved')
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const onClosed = () => {
  allUsers.value = []
  originalIds.value = []
  selectedIds.value = []
}

defineExpose({ open })
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  align-items: center;
}
</style>
