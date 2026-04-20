import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../views/LayoutView.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      redirect: '/archive',
    },
    // 用户端路由
    {
      path: '/',
      component: LayoutView,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'archive',
          name: 'Archive',
          component: () => import('../views/ArchiveView.vue'),
        },
        {
          path: 'position',
          name: 'Position',
          component: () => import('../views/PositionView.vue'),
        },
        {
          path: 'match',
          name: 'Match',
          component: () => import('../views/MatchView.vue'),
        },
        {
          path: 'favorite',
          name: 'Favorite',
          component: () => import('../views/FavoriteView.vue'),
        },
      ],
    },
    // 管理端路由
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: '/admin/import',
        },
        {
          path: 'import',
          name: 'AdminImport',
          component: () => import('../views/admin/AdminImportView.vue'),
        },
        {
          path: 'position',
          name: 'AdminPosition',
          component: () => import('../views/admin/AdminPositionView.vue'),
        },
        {
          path: 'role',
          name: 'AdminRole',
          component: () => import('../views/admin/AdminRoleView.vue'),
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const roles = JSON.parse(localStorage.getItem('roles') || '[]')
  
  // 需要登录的页面
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }
  
  // 需要管理员权限的页面
  if (to.meta.requiresAdmin) {
    if (!roles.includes('管理员')) {
      return '/archive'
    }
  }
  
  // 已登录用户访问登录页，跳转到对应首页
  if (to.path === '/login' && token) {
    if (roles.includes('管理员')) {
      return '/admin/import'
    } else {
      return '/archive'
    }
  }
  
  // 默认允许导航
  return true
})

export default router
