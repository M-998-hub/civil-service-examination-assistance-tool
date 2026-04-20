<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <h1>考公选岗系统</h1>
      </div>
      <div class="header-right">
        <span class="username">{{ username }}</span>
        <el-button type="primary" plain size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          class="layout-menu"
          router
        >
          <el-menu-item index="/archive">
            <el-icon><User /></el-icon>
            <span>个人档案</span>
          </el-menu-item>
          <el-menu-item index="/position">
            <el-icon><Document /></el-icon>
            <span>岗位列表</span>
          </el-menu-item>
          <el-menu-item index="/match">
            <el-icon><MagicStick /></el-icon>
            <span>一键匹配</span>
          </el-menu-item>
          <el-menu-item index="/favorite">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Document, MagicStick, Star } from '@element-plus/icons-vue'
import { getAdminInfo, logout } from '../api/auth'

const router = useRouter()
const route = useRoute()
const username = ref('用户')

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const loadUserInfo = async () => {
  try {
    const res = await getAdminInfo()
    if (res.data && res.data.username) {
      username.value = res.data.username
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 如果获取用户信息失败，可能是token过期，跳转到登录页
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #409EFF;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
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

.layout-aside {
  background-color: #fff;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
}

.layout-menu {
  height: 100%;
  border-right: none;
}

.layout-menu .el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409EFF;
}

.layout-main {
  padding: 20px;
  background-color: #f0f2f5;
}
</style>
