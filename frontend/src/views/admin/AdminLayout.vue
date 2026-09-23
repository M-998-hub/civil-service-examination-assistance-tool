<template>
  <el-container class="admin-layout-container">
    <el-header class="admin-layout-header">
      <div class="header-left">
        <h1>考公选岗系统 - 管理端</h1>
      </div>
      <div class="header-right">
        <span class="username">{{ username }}</span>
        <el-button type="primary" plain size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="admin-layout-aside">
        <el-menu :default-active="activeMenu" class="admin-layout-menu" router>
          <el-menu-item index="/admin/import">
            <el-icon><Upload /></el-icon>
            <span>数据导入</span>
          </el-menu-item>
          <el-menu-item index="/admin/position">
            <el-icon><Document /></el-icon>
            <span>岗位管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/role">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/user">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="admin-layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, Document, UserFilled, User } from '@element-plus/icons-vue'
import { getAdminInfo, logout } from '../../api/auth'

const router = useRouter()
const route = useRoute()
const username = ref('管理员')

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  logout()
  localStorage.removeItem('token')
  localStorage.removeItem('roles')
  localStorage.removeItem('resources')
  ElMessage.success('已退出登录')
  router.push('/login')
}

const loadUserInfo = async () => {
  try {
    const res = await getAdminInfo()
    if (res.data && res.data.username) {
      username.value = res.data.username
    }
    // 同步资源权限列表到 localStorage
    if (res.data && res.data.resources) {
      localStorage.setItem('resources', JSON.stringify(res.data.resources))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('roles')
      localStorage.removeItem('resources')
      router.push('/login')
    }
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.admin-layout-container {
  height: 100vh;
}

.admin-layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #001529;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.admin-layout-aside {
  background-color: #fff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.admin-layout-menu {
  height: 100%;
  border-right: none;
}

.admin-layout-menu .el-menu-item.is-active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.admin-layout-main {
  padding: 20px;
  background-color: #f0f2f5;
}
</style>
