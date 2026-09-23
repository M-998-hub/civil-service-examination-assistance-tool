<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-bg"></div>
      <div class="brand-content">
        <div class="brand-title">考公选岗系统</div>
        <div class="brand-slogan">智能匹配 &middot; 精准选岗</div>
        <div class="brand-illustration">
          <div class="illus-shield"><div class="illus-check"></div></div>
          <div class="illus-book">
            <div class="illus-book-line"></div>
            <div class="illus-book-line"></div>
            <div class="illus-book-line"></div>
          </div>
          <div class="illus-glass"><div class="illus-glass-handle"></div></div>
          <div class="illus-dot illus-dot-1"></div>
          <div class="illus-dot illus-dot-2"></div>
          <div class="illus-dot illus-dot-3"></div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-card">
        <div class="mobile-header">
          <h1>考公选岗系统</h1>
          <p>智能匹配 &middot; 精准选岗</p>
        </div>
        <h2 class="form-welcome">欢迎回来</h2>
        <p class="form-subtitle">登录您的账号以继续</p>

        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="密码登录" name="password">
            <el-form
              ref="pwdFormRef"
              :model="pwdForm"
              :rules="pwdRules"
              @keyup.enter="handlePwdLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="pwdForm.username"
                  placeholder="请输入用户名"
                  :prefix-icon="User"
                  size="large"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="pwdForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  size="large"
                />
              </el-form-item>
              <el-form-item prop="captchaCode">
                <div class="captcha-row">
                  <el-input
                    v-model="pwdForm.captchaCode"
                    placeholder="图片验证码"
                    size="large"
                    style="flex: 1"
                  />
                  <img
                    :src="captchaImage"
                    class="captcha-img"
                    @click="refreshCaptcha"
                    title="点击刷新验证码"
                  />
                </div>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="pwdLoading"
                  @click="handlePwdLogin"
                  class="login-btn"
                  >登 录</el-button
                >
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="验证码登录" name="code">
            <el-form
              ref="codeFormRef"
              :model="codeForm"
              :rules="codeRules"
              @keyup.enter="handleCodeLogin"
            >
              <el-form-item prop="email">
                <el-input
                  v-model="codeForm.email"
                  placeholder="请输入邮箱"
                  :prefix-icon="Message"
                  size="large"
                />
              </el-form-item>
              <el-form-item prop="captchaCode">
                <div class="captcha-row">
                  <el-input
                    v-model="codeForm.captchaCode"
                    placeholder="图片验证码"
                    size="large"
                    style="flex: 1"
                  />
                  <img
                    :src="captchaImage"
                    class="captcha-img"
                    @click="refreshCaptcha"
                    title="点击刷新验证码"
                  />
                </div>
              </el-form-item>
              <el-form-item prop="emailCode">
                <div class="captcha-row">
                  <el-input
                    v-model="codeForm.emailCode"
                    placeholder="邮箱验证码"
                    size="large"
                    style="flex: 1"
                  />
                  <el-button
                    type="primary"
                    plain
                    :disabled="codeCountdown > 0"
                    :loading="codeSending"
                    @click="handleSendLoginCode"
                    class="send-code-btn"
                    >{{ codeCountdown > 0 ? codeCountdown + 's' : '发送验证码' }}</el-button
                  >
                </div>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="codeLoading"
                  @click="handleCodeLogin"
                  class="login-btn"
                  >登 录</el-button
                >
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="form-links">
          <el-button type="primary" link @click="registerDialogVisible = true">注册账号</el-button>
          <el-button type="primary" link @click="forgotDialogVisible = true">忘记密码</el-button>
        </div>

        <div class="third-party">
          <div class="third-party-divider"><span>其他方式登录</span></div>
          <div class="third-party-icons">
            <el-tooltip content="暂未开放" placement="top">
              <div class="third-icon wechat">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="#909399">
                  <path
                    d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm3.276 3.857c-2.627 0-4.755 2.132-4.755 4.763 0 2.63 2.128 4.762 4.755 4.762.34 0 .677-.036 1.006-.106a.46.46 0 0 1 .38.05l1.058.617a.175.175 0 0 0 .09.029c.087 0 .157-.07.157-.157a.286.286 0 0 0-.026-.115l-.218-.825a.337.337 0 0 1 .118-.367A4.742 4.742 0 0 0 19.624 14.61c0-2.631-2.128-4.762-4.75-4.762zm-1.522 2.453c.353 0 .64.29.64.646a.643.643 0 0 1-.64.646.643.643 0 0 1-.64-.646c0-.356.287-.646.64-.646zm3.074 0c.353 0 .64.29.64.646a.643.643 0 0 1-.64.646.643.643 0 0 1-.64-.646c0-.356.287-.646.64-.646z"
                  />
                </svg>
              </div>
            </el-tooltip>
            <el-tooltip content="暂未开放" placement="top">
              <div class="third-icon alipay">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="#909399">
                  <path
                    d="M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2zm3.844 14.281c-1.357.642-2.948.944-4.572.944-1.623 0-3.214-.302-4.57-.944-.52-.247-.501-.88-.315-1.142.185-.261.626-.55.961-.39 1.098.524 2.459.822 3.924.822 1.464 0 2.826-.298 3.924-.822.335-.16.776.129.961.39.186.261.205.895-.313 1.142zM18 10.719H6v-1.5h12v1.5z"
                  />
                </svg>
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div class="page-footer">
        <a href="#" class="footer-link">服务协议</a>
        <span class="footer-divider">|</span>
        <a href="#" class="footer-link">隐私政策</a>
      </div>
    </div>

    <el-dialog v-model="registerDialogVisible" title="注册账号" width="440px" destroy-on-close>
      <el-form
        ref="regFormRef"
        :model="regForm"
        :rules="regRules"
        label-width="0"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="regForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="regForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="regForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="图片验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="regForm.captchaCode" placeholder="图片验证码" style="flex: 1" />
            <img
              :src="captchaImage"
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新验证码"
            />
          </div>
        </el-form-item>
        <el-form-item label="邮箱验证码" prop="emailCode">
          <div class="captcha-row">
            <el-input v-model="regForm.emailCode" placeholder="邮箱验证码" style="flex: 1" />
            <el-button
              :disabled="regCountdown > 0"
              :loading="regSending"
              @click="handleSendRegCode"
              class="send-code-btn"
              >{{ regCountdown > 0 ? regCountdown + 's' : '发送验证码' }}</el-button
            >
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="forgotDialogVisible" title="忘记密码" width="420px" destroy-on-close>
      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-width="0"
        label-position="top"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="forgotForm.email"
            placeholder="请输入注册时的邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="captcha-row">
            <el-input v-model="forgotForm.code" placeholder="请输入验证码" style="flex: 1" />
            <el-button
              :disabled="forgotCountdown > 0"
              :loading="forgotSending"
              @click="handleSendForgotCode"
              class="send-code-btn"
              >{{ forgotCountdown > 0 ? forgotCountdown + 's' : '发送验证码' }}</el-button
            >
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="forgotForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotResetLoading" @click="handleResetPassword"
          >确认重置</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock, User } from '@element-plus/icons-vue'
import {
  login,
  loginByCode,
  getCaptcha,
  register,
  sendRegisterCode,
  sendLoginCode,
  sendVerifyCode,
  resetPassword,
} from '../api/auth'

const router = useRouter()

const captchaImage = ref('')
const captchaUuid = ref('')

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.data) {
      captchaImage.value = res.data.imageBase64
      captchaUuid.value = res.data.uuid
    }
  } catch {
    // 请求层已统一展示错误信息。
    // The shared request interceptor displays the error message.
  }
}

const activeTab = ref('password')

// ========== 密码登录 ==========
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = reactive({ username: '', password: '', captchaCode: '' })
const pwdRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }],
}

const handlePwdLogin = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    // 请求层已统一展示错误信息。
    // The shared request interceptor displays the error message.
    return
  }
  pwdLoading.value = true
  try {
    const res = await login({
      username: pwdForm.username,
      password: pwdForm.password,
      captchaUuid: captchaUuid.value,
      captchaCode: pwdForm.captchaCode,
    })
    afterLogin(res)
  } catch {
    // The shared request interceptor displays the error message.
    refreshCaptcha()
  } finally {
    pwdLoading.value = false
  }
}

// ========== 验证码登录 ==========
const codeFormRef = ref(null)
const codeLoading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer = null
const codeForm = reactive({ email: '', captchaCode: '', emailCode: '' })
const codeRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

const handleSendLoginCode = async () => {
  if (!codeForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!captchaUuid.value) {
    ElMessage.warning('请先加载图片验证码')
    return
  }
  codeSending.value = true
  try {
    await sendLoginCode({
      email: codeForm.email,
      captchaUuid: captchaUuid.value,
      captchaCode: codeForm.captchaCode,
    })
    ElMessage.success('验证码已发送')
    refreshCaptcha()
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(codeTimer)
    }, 1000)
  } catch {
    // 请求层已统一展示错误信息。
    refreshCaptcha()
  } finally {
    codeSending.value = false
  }
}

const handleCodeLogin = async () => {
  if (!codeFormRef.value) return
  try {
    await codeFormRef.value.validate()
  } catch {
    return
  }
  codeLoading.value = true
  try {
    const res = await loginByCode({ email: codeForm.email, code: codeForm.emailCode })
    afterLogin(res)
  } catch {
    refreshCaptcha()
  } finally {
    codeLoading.value = false
  }
}

// ========== 注册 ==========
const registerDialogVisible = ref(false)
const regFormRef = ref(null)
const regLoading = ref(false)
const regSending = ref(false)
const regCountdown = ref(0)
let regTimer = null

const regForm = reactive({
  email: '',
  username: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  emailCode: '',
})
const validateRegConfirm = (rule, value, callback) => {
  if (value !== regForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}
const regRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateRegConfirm, trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

const handleSendRegCode = async () => {
  if (!regForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!captchaUuid.value) {
    ElMessage.warning('请先加载图片验证码')
    return
  }
  regSending.value = true
  try {
    await sendRegisterCode({
      email: regForm.email,
      captchaUuid: captchaUuid.value,
      captchaCode: regForm.captchaCode,
    })
    ElMessage.success('验证码已发送')
    refreshCaptcha()
    regCountdown.value = 60
    regTimer = setInterval(() => {
      regCountdown.value--
      if (regCountdown.value <= 0) clearInterval(regTimer)
    }, 1000)
  } catch {
    refreshCaptcha()
  } finally {
    regSending.value = false
  }
}

const handleRegister = async () => {
  if (!regFormRef.value) return
  try {
    await regFormRef.value.validate()
  } catch {
    return
  }
  regLoading.value = true
  try {
    await register({
      email: regForm.email,
      username: regForm.username,
      password: regForm.password,
      confirmPassword: regForm.confirmPassword,
      captchaUuid: captchaUuid.value,
      captchaCode: regForm.captchaCode,
      emailCode: regForm.emailCode,
    })
    ElMessage.success('注册成功，请登录')
    registerDialogVisible.value = false
    pwdForm.username = regForm.username
    activeTab.value = 'password'
    refreshCaptcha()
  } catch {
    refreshCaptcha()
  } finally {
    regLoading.value = false
  }
}

// ========== 忘记密码 ==========
const forgotDialogVisible = ref(false)
const forgotFormRef = ref(null)
const forgotSending = ref(false)
const forgotResetLoading = ref(false)
const forgotCountdown = ref(0)
let forgotTimer = null

const forgotForm = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })
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
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateForgotConfirm, trigger: 'blur' },
  ],
}

const handleSendForgotCode = async () => {
  if (!forgotForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  forgotSending.value = true
  try {
    const res = await sendVerifyCode({ email: forgotForm.email })
    ElMessage.success('验证码已发送')
    if (res.data && res.data.code) forgotForm.code = res.data.code
    forgotCountdown.value = 60
    forgotTimer = setInterval(() => {
      forgotCountdown.value--
      if (forgotCountdown.value <= 0) clearInterval(forgotTimer)
    }, 1000)
  } catch {
    // 请求层已统一展示错误信息。
  } finally {
    forgotSending.value = false
  }
}

const handleResetPassword = async () => {
  if (!forgotFormRef.value) return
  try {
    await forgotFormRef.value.validate()
  } catch {
    return
  }
  forgotResetLoading.value = true
  try {
    await resetPassword(forgotForm)
    ElMessage.success('密码重置成功，请使用新密码登录')
    forgotDialogVisible.value = false
    Object.assign(forgotForm, { email: '', code: '', newPassword: '', confirmPassword: '' })
  } catch {
    // 请求层已统一展示错误信息。
  } finally {
    forgotResetLoading.value = false
  }
}

const afterLogin = (res) => {
  const token = res.data?.token
  if (token) {
    localStorage.setItem('token', token)
    if (res.data?.refreshToken) localStorage.setItem('refreshToken', res.data.refreshToken)
    if (res.data?.roles) localStorage.setItem('roles', JSON.stringify(res.data.roles))
    if (res.data?.resources) localStorage.setItem('resources', JSON.stringify(res.data.resources))
    ElMessage.success('登录成功')
    const resources = res.data?.resources || []
    const hasAdmin = resources.some((u) => u && u.startsWith('/admin'))
    router.push(hasAdmin ? '/admin/import' : '/home')
  }
}

onMounted(() => {
  refreshCaptcha()
})
onBeforeUnmount(() => {
  if (codeTimer) clearInterval(codeTimer)
  if (regTimer) clearInterval(regTimer)
  if (forgotTimer) clearInterval(forgotTimer)
})
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.brand-panel {
  display: none;
  flex: 0 0 45%;
  position: relative;
  overflow: hidden;
}
.brand-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a56db 0%, #3b82f6 40%, #60a5fa 100%);
}
.brand-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.06) 0%, transparent 50%);
}
.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 60px 40px;
  color: #fff;
}
.brand-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 4px;
  margin-bottom: 12px;
}
.brand-slogan {
  font-size: 16px;
  opacity: 0.85;
  letter-spacing: 2px;
  margin-bottom: 60px;
}
.brand-illustration {
  position: relative;
  width: 220px;
  height: 220px;
}
.illus-shield {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 70px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50% 50% 50% 50%/12% 12% 88% 88%;
}
.illus-check {
  position: absolute;
  top: 25px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 10px;
  height: 20px;
  border-right: 3px solid #fff;
  border-bottom: 3px solid #fff;
}
.illus-book {
  position: absolute;
  bottom: 40px;
  left: 30px;
  width: 50px;
  height: 60px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 3px 8px 8px 3px;
}
.illus-book-line {
  height: 2px;
  background: rgba(255, 255, 255, 0.4);
  margin: 10px 8px 0;
  border-radius: 1px;
}
.illus-glass {
  position: absolute;
  bottom: 50px;
  right: 30px;
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.25);
  border-radius: 50%;
}
.illus-glass-handle {
  position: absolute;
  bottom: -8px;
  right: -12px;
  width: 20px;
  height: 5px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 3px;
  transform: rotate(-45deg);
  transform-origin: left center;
}
.illus-dot {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
}
.illus-dot-1 {
  width: 10px;
  height: 10px;
  top: 10px;
  left: 40px;
}
.illus-dot-2 {
  width: 6px;
  height: 6px;
  top: 60px;
  right: 50px;
}
.illus-dot-3 {
  width: 8px;
  height: 8px;
  bottom: 90px;
  left: 50%;
}
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 20px;
}
.form-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}
.mobile-header {
  display: block;
  text-align: center;
  margin-bottom: 28px;
}
.mobile-header h1 {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a56db;
}
.mobile-header p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}
.form-welcome {
  margin: 0 0 4px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}
.form-subtitle {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #6b7280;
}
.login-tabs {
  margin-bottom: 8px;
}
.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}
.captcha-img {
  height: 40px;
  width: 130px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  flex-shrink: 0;
}
.send-code-btn {
  flex-shrink: 0;
  width: 110px;
}
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}
.form-links {
  display: flex;
  justify-content: space-between;
  margin: 12px 0 20px;
}
.third-party-divider {
  display: flex;
  align-items: center;
  color: #9ca3af;
  font-size: 12px;
  margin-bottom: 14px;
}
.third-party-divider::before,
.third-party-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}
.third-party-divider span {
  padding: 0 12px;
}
.third-party-icons {
  display: flex;
  justify-content: center;
  gap: 32px;
}
.third-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: not-allowed;
  background: #f3f4f6;
  transition: opacity 0.2s;
  opacity: 0.5;
}
.third-icon:hover {
  opacity: 0.8;
}
.page-footer {
  margin-top: 24px;
  text-align: center;
}
.footer-link {
  color: #9ca3af;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}
.footer-link:hover {
  color: #3b82f6;
}
.footer-divider {
  margin: 0 10px;
  color: #d1d5db;
}
@media (min-width: 768px) {
  .brand-panel {
    display: flex;
  }
  .mobile-header {
    display: none;
  }
  .form-panel {
    background: #fff;
  }
  .form-card {
    box-shadow: none;
    padding: 40px 20px;
  }
}
@media (max-width: 767px) {
  .form-welcome {
    display: none;
  }
  .form-subtitle {
    display: none;
  }
  .form-card {
    padding: 28px 24px;
  }
}
</style>
