<template>
  <div class="not-found-page">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-icon">
        <el-icon :size="64" color="#fff"><WarningFilled /></el-icon>
      </div>
      <h1 class="error-title">抱歉，页面走丢了</h1>
      <p class="error-desc">您访问的页面不存在或已被移除，让我们回到首页继续探索吧</p>
      <div class="actions">
        <el-button type="primary" size="large" class="home-btn" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button size="large" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { WarningFilled, HomeFilled, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

function goHome() {
  router.push('/home')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}
</script>

<style lang="scss" scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f4a8b8 0%, #f0c0cb 50%, #ffd9e0 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 250px;
  height: 250px;
  bottom: -50px;
  left: -50px;
  animation: float 10s ease-in-out infinite reverse;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 40%;
  left: 10%;
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(30px, -30px);
  }
}

.content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  max-width: 500px;
}

.error-code {
  font-size: 160px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 8px;
  margin-bottom: 16px;
  background: linear-gradient(180deg, #fff 0%, rgba(255, 255, 255, 0.6) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.error-icon {
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  backdrop-filter: blur(8px);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.error-title {
  font-size: 28px;
  margin: 0 0 12px;
  font-weight: 600;
}

.error-desc {
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 40px;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.home-btn {
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  background: #fff;
  color: var(--pink-deep);
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 500;

  &:hover {
    opacity: 0.95;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(244, 168, 184, 0.3);
  }
}

.back-btn {
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: var(--radius-sm);
  font-weight: 500;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: translateY(-2px);
  }
}

@media (max-width: 480px) {
  .error-code {
    font-size: 120px;
  }

  .error-title {
    font-size: 22px;
  }

  .actions {
    flex-direction: column;
    align-items: center;
  }

  .home-btn,
  .back-btn {
    width: 200px;
  }
}
</style>
