<template>
  <div class="role-manage">
    <h2>角色权限管理</h2>
    <el-card class="role-list">
      <div class="card-header">
        <el-button type="primary" @click="handleAddRole">新增角色</el-button>
        <el-button
          type="danger"
          :disabled="selectedRoles.length === 0"
          @click="batchDelete"
          style="margin-left: 10px"
        >
          批量删除 ({{ selectedRoles.length }})
        </el-button>
      </div>
      <el-table
        :data="roleList"
        v-loading="loading"
        style="width: 100%"
        @selection-change="onSelChange"
        ref="roleTableRef"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="handleAssign(scope.row)"
              >权限</el-button
            >
            <el-button type="success" link size="small" @click="handleAssignUser(scope.row)"
              >用户</el-button
            >
            <el-button type="primary" link size="small" @click="handleEditRole(scope.row)"
              >编辑</el-button
            >
            <el-popconfirm title="确定删除该角色吗？" @confirm="handleDeleteRole(scope.row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="dialogVisible" title="分配权限" width="800px" :close-on-click-modal="false">
      <div class="resource-tree-container">
        <div class="tree-header">
          <el-button type="success" size="small" @click="handleCheckAll">全选</el-button>
          <el-button type="warning" size="small" @click="handleUncheckAll">全不选</el-button>
        </div>
        <div class="tree-content">
          <el-tree
            ref="treeRef"
            :data="resourceTree"
            show-checkbox
            node-key="id"
            :props="treeProps"
            :default-expand-all="true"
            :default-checked-keys="checkedKeys"
          >
            <template #default="{ data }">
              <span class="custom-tree-node">
                <span v-if="data.categoryName" class="category-name">{{ data.categoryName }}</span>
                <span v-else class="resource-name">{{ data.name }}</span>
                <span v-if="data.url" class="resource-url">({{ data.url }})</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="formTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveForm" :loading="formSaving">保存</el-button>
      </template>
    </el-dialog>

    <RoleUserDialog ref="roleUserDialogRef" @saved="loadRoles" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRole,
  getRoleList,
  getRoleResources,
  assignResources,
  updateRole,
  deleteRole,
} from '../../api/role'
import { getResourceTree } from '../../api/resource'
import RoleUserDialog from './RoleUserDialog.vue'

const roleList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saveLoading = ref(false)
const currentRole = ref(null)
const resourceTree = ref([])
const checkedKeys = ref([])
const treeRef = ref(null)
const roleUserDialogRef = ref(null)
const roleTableRef = ref(null)
const selectedRoles = ref([])

const formDialogVisible = ref(false)
const formSaving = ref(false)
const formRef = ref(null)
const formData = ref({ name: '', description: '' })
const editingId = ref(null)

const formTitle = computed(() => (editingId.value ? '编辑角色' : '新增角色'))
const formRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在2到20个字符', trigger: 'blur' },
  ],
  description: [{ max: 100, message: '最多100个字符', trigger: 'blur' }],
}

const treeProps = { label: 'name', children: 'resources' }

const onSelChange = (rows) => {
  selectedRoles.value = rows
}

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    roleList.value = res.data || []
  } catch {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const loadResourceTree = async () => {
  try {
    const res = await getResourceTree()
    resourceTree.value = res.data || []
  } catch {
    ElMessage.error('获取资源树失败')
  }
}

const loadRoleResources = async (roleId) => {
  try {
    const res = await getRoleResources(roleId)
    checkedKeys.value = res.data || []
  } catch {
    checkedKeys.value = []
  }
}

const handleAssign = async (role) => {
  currentRole.value = role
  dialogVisible.value = true
  await Promise.all([loadResourceTree(), loadRoleResources(role.id)])
  if (treeRef.value) treeRef.value.setCheckedKeys(checkedKeys.value)
}

const handleSave = async () => {
  if (!treeRef.value) return
  const checkedIds = treeRef.value.getCheckedKeys()
  saveLoading.value = true
  try {
    await assignResources({ roleId: currentRole.value.id, resourceIds: checkedIds })
    ElMessage.success('权限分配成功')
    dialogVisible.value = false
    await loadRoles()
  } catch {
    ElMessage.error('权限分配失败')
  } finally {
    saveLoading.value = false
  }
}

const handleCheckAll = () => {
  if (treeRef.value) {
    const ids = []
    resourceTree.value.forEach((c) => {
      if (c.resources) c.resources.forEach((r) => ids.push(r.id))
    })
    treeRef.value.setCheckedKeys(ids)
  }
}

const handleUncheckAll = () => {
  if (treeRef.value) treeRef.value.setCheckedKeys([])
}

const handleAssignUser = (role) => {
  roleUserDialogRef.value?.open(role)
}

const handleAddRole = () => {
  editingId.value = null
  formData.value = { name: '', description: '' }
  formDialogVisible.value = true
}

const handleEditRole = (role) => {
  editingId.value = role.id
  formData.value = { name: role.name, description: role.description || '' }
  formDialogVisible.value = true
}

const handleSaveForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    formSaving.value = true
    try {
      if (editingId.value) {
        await updateRole(editingId.value, formData.value)
        ElMessage.success('角色已更新')
      } else {
        await createRole(formData.value)
        ElMessage.success('角色已创建')
      }
      formDialogVisible.value = false
      await loadRoles()
    } catch {
      ElMessage.error('保存失败')
    } finally {
      formSaving.value = false
    }
  })
}

const handleDeleteRole = async (id) => {
  try {
    await deleteRole(id)
    ElMessage.success('角色已删除')
    await loadRoles()
  } catch {
    ElMessage.error('删除失败')
  }
}

const batchDelete = () => {
  ElMessageBox.confirm('确定删除选中的 ' + selectedRoles.value.length + ' 个角色吗？', '批量删除', {
    type: 'warning',
  })
    .then(async () => {
      try {
        for (const r of selectedRoles.value) await deleteRole(r.id)
        ElMessage.success('已删除')
        roleTableRef.value?.clearSelection()
        await loadRoles()
      } catch {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.role-manage {
  padding: 20px;
}
.role-list {
  margin-top: 20px;
}
.card-header {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}
.resource-tree-container {
  max-height: 500px;
  overflow-y: auto;
}
.tree-header {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}
.tree-content {
  padding: 10px;
}
.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
.category-name {
  font-weight: bold;
  color: #303133;
}
.resource-name {
  color: #606266;
}
.resource-url {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
