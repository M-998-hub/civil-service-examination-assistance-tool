import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

// 在挂载应用之前注册全局守卫，确保拦截首次导航
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    console.warn('暂未登录或 token 已经过期');
    return { name: 'Login' }; // 重定向到登录页
  }
  return true;
});

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
