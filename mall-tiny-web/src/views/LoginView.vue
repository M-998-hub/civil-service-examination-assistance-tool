<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>考公选岗系统</h2>
        </div>
      </template>
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <div class="login-links">
            <el-button type="primary" link @click="registerDialogVisible = true">注册账号</el-button>
            <el-button type="primary" link @click="forgotDialogVisible = true">忘记密码</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 注册弹窗 -->
    <el-dialog v-model="registerDialogVisible" title="注册账号" width="420px" destroy-on-close>
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱（用于找回密码）" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="registerForm.nickName" placeholder="请输入昵称（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>

    <!-- 忘记密码弹窗 -->
    <el-dialog v-model="forgotDialogVisible" title="忘记密码" width="420px" destroy-on-close>
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="100px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="forgotForm.email" placeholder="请输入注册时的邮箱">
            <template #append>
              <el-button :disabled="countdown > 0" :loading="sendingCode" @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="forgotForm.code" placeholder="请输入验证码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, sendVerifyCode, resetPassword } from '../api/auth'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await login(loginForm)
    const token = res.data.token
    const roles = res.data.roles || []
    const resources = res.data.resources || []

    localStorage.setItem('token', token)
    localStorage.setItem('roles', JSON.stringify(roles))
    localStorage.setItem('resources', JSON.stringify(resources))

    ElMessage.success('登录成功')

    // 根据权限跳转到不同页面：拥有 /admin/** 权限即可进入管理端
    const hasAdminAccess = resources.some(url => url && url.startsWith('/admin'))
    if (hasAdminAccess) {
      router.push('/admin/import')
    } else {
      router.push('/home')
    }
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// ========== 注册 ==========
const registerDialogVisible = ref(false)
const registerFormRef = ref(null)
const registerLoading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickName: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  registerLoading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    registerDialogVisible.value = false
    // 自动填充用户名
    loginForm.username = registerForm.username
    // 重置注册表单
    Object.assign(registerForm, { username: '', password: '', confirmPassword: '', email: '', nickName: '' })
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    registerLoading.value = false
  }
}

// ========== 忘记密码 ==========
const forgotDialogVisible = ref(false)
const forgotFormRef = ref(null)
const sendingCode = ref(false)
const resetLoading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const validateForgotConfirm = (rule, value, callback) => {
  if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateForgotConfirm, trigger: 'blur' }
  ]
}

const handleSendCode = async () => {
  if (!forgotForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  sendingCode.value = true
  try {
    const res = await sendVerifyCode({ email: forgotForm.email })
    ElMessage.success('验证码已发送，请查看邮箱')
    // 演示模式：如果返回了验证码，自动填充
    if (res.data && res.data.code) {
      forgotForm.code = res.data.code
    }
    // 开始倒计时
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    sendingCode.value = false
  }
}

const handleResetPassword = async () => {
  if (!forgotFormRef.value) return
  try {
    await forgotFormRef.value.validate()
  } catch {
    return
  }

  resetLoading.value = true
  try {
    await resetPassword(forgotForm)
    ElMessage.success('密码重置成功，请使用新密码登录')
    forgotDialogVisible.value = false
    // 重置表单
    Object.assign(forgotForm, { email: '', code: '', newPassword: '', confirmPassword: '' })
  } catch (error) {
    console.error('重置密码失败:', error)
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
}

.login-header h2 {
  margin: 0;
  color: #303133;
}

.login-links {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
</style>
