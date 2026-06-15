<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    <el-card class="user-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="danger" :disabled="selectedUsers.length === 0" @click="batchDelete" style="margin-left:10px">
          批量删除 ({{ selectedUsers.length }})
        </el-button>
      </div>
      <el-table :data="userList" v-loading="loading" style="width: 100%" @selection-change="onSelChange" ref="userTableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickName" label="昵称" width="130">
          <template #default="scope">{{ scope.row.nickName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="scope">{{ scope.row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status !== 1"
              @change="(val) => handleToggleStatus(scope.row, !val)"
            />
            <span style="margin-left:6px;font-size:13px;color:#909399">{{ scope.row.status === 1 ? '启用' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="scope">
            <el-button type="success" link size="small" @click="handleViewRoles(scope.row)">角色</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="420px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="editForm.nickName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看角色弹窗 -->
    <el-dialog v-model="rolesVisible" title="用户角色" width="400px">
      <div v-loading="rolesLoading">
        <el-tag v-for="role in rolesList" :key="role.id" style="margin: 4px" type="primary">
          {{ role.name }}
        </el-tag>
        <el-empty v-if="!rolesLoading && rolesList.length === 0" description="暂无角色" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, getAdminRoles, updateUser, updateUserStatus, deleteUser } from '../../api/user'

const userList = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userTableRef = ref(null)
const selectedUsers = ref([])

const editVisible = ref(false)
const editFormRef = ref(null)
const editSaving = ref(false)
const editForm = reactive({ nickName: '', email: '' })
const editUserId = ref(null)
const editRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const rolesVisible = ref(false)
const rolesLoading = ref(false)
const rolesList = ref([])

const onSelChange = (rows) => { selectedUsers.value = rows }

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList(keyword.value, pageSize.value, pageNum.value)
    userList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally { loading.value = false }
}

const handleSearch = () => {
  pageNum.value = 1
  loadUsers()
}

const handleToggleStatus = async (row, enabled) => {
  try {
    await updateUserStatus(row.id, enabled ? 1 : 0)
    ElMessage.success(enabled ? '已启用' : '已禁用')
    row.status = enabled ? 1 : 0
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleEdit = (row) => {
  editUserId.value = row.id
  editForm.nickName = row.nickName || ''
  editForm.email = row.email || ''
  editVisible.value = true
}

const handleSaveEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSaving.value = true
    try {
      await updateUser(editUserId.value, { nickName: editForm.nickName, email: editForm.email })
      ElMessage.success('用户信息已更新')
      editVisible.value = false
      loadUsers()
    } catch (e) {
      ElMessage.error('更新失败')
    } finally { editSaving.value = false }
  })
}

const handleViewRoles = async (row) => {
  rolesVisible.value = true
  rolesLoading.value = true
  rolesList.value = []
  try {
    const res = await getAdminRoles(row.id)
    rolesList.value = res.data || []
  } catch (e) {
    ElMessage.error('获取角色失败')
  } finally { rolesLoading.value = false }
}

const handleDelete = async (id) => {
  try {
    await deleteUser(id)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const batchDelete = () => {
  ElMessageBox.confirm('确定删除选中的 ' + selectedUsers.value.length + ' 个用户吗？', '批量删除', { type: 'warning' }).then(async () => {
    try {
      for (const u of selectedUsers.value) await deleteUser(u.id)
      ElMessage.success('已删除')
      userTableRef.value?.clearSelection()
      await loadUsers()
    } catch (e) { ElMessage.error('删除失败') }
  }).catch(() => {})
}

onMounted(() => { loadUsers() })
</script>

<style scoped>
.user-manage { padding: 20px; }
h2 { color: #303133; margin-bottom: 20px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.pagination-box { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
