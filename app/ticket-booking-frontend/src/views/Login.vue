<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand-area">
          <div class="brand-logo">
            <span class="logo-circle">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
            </span>
          </div>
          <h1 class="brand-title">智行票务</h1>
          <p class="brand-subtitle">AI 驱动 · 智慧出行 · 一句话搞定购票</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon :size="20"><ChatDotRound /></el-icon>
              <span>AI 智能客服对话</span>
            </div>
            <div class="feature-item">
              <el-icon :size="20"><Search /></el-icon>
              <span>自然语言查车票</span>
            </div>
            <div class="feature-item">
              <el-icon :size="20"><Tickets /></el-icon>
              <span>在线选座一键购票</span>
            </div>
          </div>
        </div>
        <div class="deco-train">
          <svg viewBox="0 0 200 80" width="280" height="112" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g opacity="0.25" fill="#fff">
              <rect x="10" y="20" width="120" height="38" rx="6"/>
              <rect x="135" y="28" width="50" height="30" rx="4"/>
              <circle cx="40" cy="64" r="6"/>
              <circle cx="80" cy="64" r="6"/>
              <circle cx="160" cy="64" r="5"/>
              <rect x="22" y="30" width="20" height="14" rx="2" fill="#2563eb" opacity="0.5"/>
              <rect x="48" y="30" width="20" height="14" rx="2" fill="#2563eb" opacity="0.5"/>
              <rect x="74" y="30" width="20" height="14" rx="2" fill="#2563eb" opacity="0.5"/>
              <rect x="100" y="30" width="20" height="14" rx="2" fill="#2563eb" opacity="0.5"/>
            </g>
          </svg>
        </div>
      </div>
      <div class="login-right">
        <div class="login-card">
          <h2 class="login-title">欢迎登录</h2>
          <p class="login-subtitle">登录智行票务，开启智慧出行</p>
          <el-form ref="formRef" :model="form" :rules="rules" class="login-form" size="default">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
            </el-form-item>
            <div class="form-options">
              <el-checkbox v-model="form.remember">记住我</el-checkbox>
              <router-link to="/register" class="register-link">没有账号？去注册</router-link>
            </div>
            <el-form-item>
              <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">登 录</el-button>
            </el-form-item>
            <div class="quick-tips">
              <span class="tip-title">测试账号：</span>
              <span class="tip-text">admin / admin123</span>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Tickets, Search, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  const saved = localStorage.getItem('rememberedUser')
  if (saved) {
    form.value.username = saved
    form.value.remember = true
  }
})

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.value.username, password: form.value.password })
    if (form.value.remember) {
      localStorage.setItem('rememberedUser', form.value.username)
    } else {
      localStorage.removeItem('rememberedUser')
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/home'
    router.push(redirect)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
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
    right: -8%;
    width: 420px;
    height: 420px;
    background: rgba(249, 115, 22, 0.18);
    border-radius: 50%;
    filter: blur(40px);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -12%;
    left: -6%;
    width: 380px;
    height: 380px;
    background: rgba(147, 197, 253, 0.2);
    border-radius: 50%;
    filter: blur(40px);
  }
}

.login-container {
  display: flex;
  width: 920px;
  max-width: 100%;
  min-height: 540px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.35);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.login-left {
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
    bottom: -20%;
    left: -10%;
    width: 200px;
    height: 200px;
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

.deco-train {
  position: relative;
  z-index: 1;
  margin-top: 30px;
}

.login-right {
  flex: 1;
  padding: 50px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: var(--bg-card);
}

.login-card {
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
}

.login-title {
  font-size: 26px;
  color: var(--primary-blue-deep);
  margin: 0 0 6px;
  text-align: center;
  font-weight: 700;
}

.login-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 30px;
  text-align: center;
}

.login-form {
  .el-form-item {
    margin-bottom: 18px;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  font-size: 13px;

  :deep(.el-checkbox__label) {
    color: var(--text-secondary);
  }
}

.register-link {
  color: var(--primary-blue);
  text-decoration: none;
  font-weight: 500;

  &:hover {
    color: var(--primary-blue-deep);
    text-decoration: underline;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 8px;
  padding-left: 8px;
}

.quick-tips {
  margin-top: 16px;
  padding: 10px 14px;
  background: var(--primary-blue-bg);
  border: 1px solid var(--border-blue);
  border-radius: var(--radius-sm);
  font-size: 12px;
  text-align: center;

  .tip-title {
    color: var(--text-secondary);
  }
  .tip-text {
    color: var(--primary-blue-deep);
    font-weight: 600;
    font-family: 'Consolas', monospace;
  }
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    min-height: auto;
  }

  .login-left {
    padding: 36px 30px;

    .deco-train { display: none; }
  }

  .login-right {
    padding: 36px 30px;
  }
}
</style>
