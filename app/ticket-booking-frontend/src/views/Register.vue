<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-left">
        <div class="brand-area">
          <div class="brand-logo">
            <span class="logo-circle">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
            </span>
          </div>
          <h1 class="brand-title">加入智行</h1>
          <p class="brand-subtitle">注册账户，开启 AI 智慧出行之旅</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon :size="20"><Tickets /></el-icon>
              <span>AI 一句话购票</span>
            </div>
            <div class="feature-item">
              <el-icon :size="20"><Lock /></el-icon>
              <span>账户安全保障</span>
            </div>
            <div class="feature-item">
              <el-icon :size="20"><Bell /></el-icon>
              <span>订单消息通知</span>
            </div>
          </div>
        </div>
        <div class="deco-clouds">
          <svg viewBox="0 0 300 120" width="320" height="128" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g opacity="0.18" fill="#fff">
              <ellipse cx="70" cy="80" rx="50" ry="20"/>
              <ellipse cx="90" cy="60" rx="40" ry="18"/>
              <ellipse cx="130" cy="70" rx="45" ry="22"/>
              <ellipse cx="200" cy="90" rx="55" ry="18"/>
              <ellipse cx="230" cy="68" rx="35" ry="15"/>
            </g>
          </svg>
        </div>
      </div>
      <div class="register-right">
        <div class="register-card">
          <h2 class="register-title">创建账户</h2>
          <p class="register-subtitle">填写以下信息完成注册</p>
          <el-form ref="formRef" :model="form" :rules="rules" class="register-form" size="default">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名 (3-16位)" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码 (至少6位)" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Iphone" maxlength="11" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading">注 册</el-button>
            </el-form-item>
            <div class="form-footer">
              <span>已有账号？</span>
              <router-link to="/login" class="login-link">立即登录</router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Tickets, Bell, Iphone } from '@element-plus/icons-vue'

const router = useRouter()

const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 16, message: '用户名长度为 3-16 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ]
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await register({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 40%, #3b82f6 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -15%;
    left: -8%;
    width: 420px;
    height: 420px;
    background: rgba(249, 115, 22, 0.16);
    border-radius: 50%;
    filter: blur(40px);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -12%;
    right: -6%;
    width: 380px;
    height: 380px;
    background: rgba(147, 197, 253, 0.2);
    border-radius: 50%;
    filter: blur(40px);
  }
}

.register-container {
  display: flex;
  width: 940px;
  max-width: 100%;
  min-height: 600px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.35);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.register-left {
  flex: 1.1;
  background: linear-gradient(160deg, #1e40af 0%, #2563eb 60%, #3b82f6 100%);
  color: #fff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40%;
    right: -20%;
    width: 320px;
    height: 320px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 50%;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -10%;
    width: 240px;
    height: 240px;
    background: rgba(249, 115, 22, 0.15);
    border-radius: 50%;
  }
}

.brand-area {
  position: relative;
  z-index: 1;
}

.brand-logo {
  margin-bottom: 18px;

  .logo-circle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 68px;
    height: 68px;
    border-radius: 50%;
    background: var(--brand-red);
    border: 3px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  }
}

.brand-title {
  font-size: 30px;
  margin: 0 0 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  font-size: 14px;
  margin: 0 0 36px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.95;
  background: rgba(255, 255, 255, 0.12);
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  width: fit-content;
}

.deco-clouds {
  position: relative;
  z-index: 1;
  margin-top: 30px;
}

.register-right {
  flex: 1;
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: var(--bg-card);
}

.register-card {
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
}

.register-title {
  font-size: 26px;
  color: var(--primary-blue-deep);
  margin: 0 0 6px;
  text-align: center;
  font-weight: 700;
}

.register-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 24px;
  text-align: center;
}

.register-form {
  .el-form-item {
    margin-bottom: 16px;
  }
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 8px;
  padding-left: 8px;
}

.form-footer {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 6px;
}

.login-link {
  color: var(--primary-blue);
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;

  &:hover {
    color: var(--primary-blue-deep);
    text-decoration: underline;
  }
}

@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
    min-height: auto;
  }

  .register-left {
    padding: 32px 30px;

    .deco-clouds { display: none; }
  }

  .register-right {
    padding: 32px 30px;
  }
}
</style>
