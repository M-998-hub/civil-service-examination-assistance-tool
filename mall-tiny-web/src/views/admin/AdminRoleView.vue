<template>
  <div class="role-manage">
    <h2>角色权限管理</h2>
    <el-card class="role-list">
      <div class="card-header">
        <el-button type="primary" @click="handleAddRole">新增角色</el-button>
      </div>
      <el-table :data="roleList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleAssign(scope.row)">分配权限</el-button>
            <el-button type="success" size="small" @click="handleAssignUser(scope.row)">分配用户</el-button>
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
          <el-tree ref="treeRef" :data="resourceTree" show-checkbox node-key="id" :props="treeProps" :default-expand-all="true" :default-checked-keys="checkedKeys">
            <template #default="{ node, data }">
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

    <!-- 新增角色弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增角色" width="500px" :close-on-click-modal="false">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole" :loading="saveRoleLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配用户弹窗 -->
    <RoleUserDialog ref="roleUserDialogRef" @saved="loadRoles" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createRole, getRoleList, getRoleResources, assignResources } from '../../api/role'
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

const addDialogVisible = ref(false)
const saveRoleLoading = ref(false)
const addFormRef = ref(null)
const addForm = ref({ name: '', description: '' })

const addRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  description: [{ max: 100, message: '最多 100 个字符', trigger: 'blur' }]
}

const treeProps = { label: 'name', children: 'resources' }

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    roleList.value = res.data || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败')
  } finally { loading.value = false }
}

const loadResourceTree = async () => {
  try {
    const res = await getResourceTree()
    resourceTree.value = res.data || []
  } catch (error) {
    console.error('获取资源树失败:', error)
    ElMessage.error('获取资源树失败')
  }
}

const loadRoleResources = async (roleId) => {
  try {
    const res = await getRoleResources(roleId)
    checkedKeys.value = res.data || []
  } catch (error) {
    console.error('获取角色资源失败:', error)
    checkedKeys.value = []
  }
}

const handleAssign = async (role) => {
  currentRole.value = role
  dialogVisible.value = true
  await Promise.all([loadResourceTree(), loadRoleResources(role.id)])
  if (treeRef.value) treeRef.value.setCheckedKeys(checkedKeys.value)
}

const handleCheckAll = () => {
  if (treeRef.value) {
    const ids = []
    resourceTree.value.forEach(c => { if (c.resources) c.resources.forEach(r => ids.push(r.id)) })
    treeRef.value.setCheckedKeys(ids)
  }
}

const handleUncheckAll = () => { if (treeRef.value) treeRef.value.setCheckedKeys([]) }

const handleAssignUser = (role) => { roleUserDialogRef.value?.open(role) }

const handleAddRole = () => {
  addForm.value = { name: '', description: '' }
  addDialogVisible.value = true
}

const handleSaveRole = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    saveRoleLoading.value = true
    try {
      await createRole(addForm.value)
      ElMessage.success('角色创建成功')
      addDialogVisible.value = false
      await loadRoles()
    } catch (error) {
      console.error('创建角色失败:', error)
      ElMessage.error('创建角色失败')
    } finally { saveRoleLoading.value = false }
  })
}

const handleSave = async () => {
  if (!currentRole.value) return
  saveLoading.value = true
  try {
    const checkedNodes = treeRef.value.getCheckedNodes(true)
    await assignResources({ roleId: currentRole.value.id, resourceIds: checkedNodes.map(n => n.id) })
    ElMessage.success('权限分配成功')
    dialogVisible.value = false
  } catch (error) {
    console.error('权限分配失败:', error)
    ElMessage.error('权限分配失败')
  } finally { saveLoading.value = false }
}

onMounted(() => { loadRoles() })
</script>

<style scoped>
.role-manage { padding: 20px; }
.role-list { margin-top: 20px; }
.card-header { margin-bottom: 15px; }
.resource-tree-container { max-height: 500px; overflow-y: auto; }
.tree-header { margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #ebeef5; }
.tree-content { padding: 10px; }
.custom-tree-node { display: flex; align-items: center; font-size: 14px; }
.category-name { font-weight: bold; color: #303133; }
.resource-name { color: #606266; }
.resource-url { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
