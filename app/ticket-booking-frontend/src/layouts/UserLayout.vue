<template>
  <div class="user-layout">
    <!-- ===== 顶部工具栏（白色）：品牌Logo + 搜索 + 登录 ===== -->
    <header class="top-bar">
      <div class="top-inner">
        <router-link to="/home" class="logo">
          <span class="logo-circle">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
          </span>
          <span class="logo-text">
            <span class="logo-title">智行票务</span>
            <span class="logo-sub">AI · 智慧出行服务平台</span>
          </span>
        </router-link>
        <div class="search-wrapper">
          <el-input v-model="searchKeyword" placeholder="搜索车次、订单、出行攻略" size="default" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="default" class="search-btn">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </el-button>
        </div>
        <div class="top-right">
          <a class="top-link" href="#" @click.prevent="goTo('/ai-chat')">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 客服</span>
          </a>
          <span class="divider">|</span>
          <a class="top-link" href="#" @click.prevent>帮助中心</a>
          <div class="auth-area">
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="click" @command="handleCommand">
                <span class="user-info">
                  <el-avatar :size="28" :icon="UserFilled" />
                  <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="my-orders">我的订单</el-dropdown-item>
                    <el-dropdown-item v-if="userStore.isAdmin" command="admin">管理后台</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/login" class="login-btn">登录</router-link>
              <router-link to="/register" class="register-btn">注册</router-link>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- ===== 主导航栏（蓝色） ===== -->
    <nav class="main-nav">
      <div class="nav-inner">
        <router-link to="/home" class="nav-item" exact-active-class="nav-active">首页</router-link>
        <router-link to="/search" class="nav-item" active-class="nav-active">车票查询</router-link>
        <router-link to="/my-orders" class="nav-item" active-class="nav-active">我的订单</router-link>
        <router-link to="/ai-chat" class="nav-item nav-ai" active-class="nav-active">
          <el-icon class="ai-spark"><ChatDotRound /></el-icon>
          AI 智能助手
        </router-link>
        <div class="nav-item has-dropdown">
          出行服务
          <el-icon class="caret"><ArrowDown /></el-icon>
          <div class="nav-dropdown">
            <a class="drop-item" href="#" @click.prevent="goTo('/profile')">乘客信息管理</a>
            <a class="drop-item" href="#" @click.prevent="goTo('/my-orders')">订单改签</a>
            <a class="drop-item" href="#" @click.prevent="goTo('/my-orders')">车票退票</a>
          </div>
        </div>
      </div>
    </nav>

    <!-- ===== 内容区 ===== -->
    <main class="user-main">
      <router-view />
    </main>

    <!-- ===== 页脚 ===== -->
    <footer class="user-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">智行票务</span>
          <span class="footer-slogan">AI 驱动的智慧出行服务平台</span>
        </div>
        <div class="footer-links">
          <a href="#" @click.prevent>关于我们</a>
          <span class="divider">|</span>
          <a href="#" @click.prevent>服务条款</a>
          <span class="divider">|</span>
          <a href="#" @click.prevent>隐私政策</a>
          <span class="divider">|</span>
          <a href="#" @click.prevent="goTo('/ai-chat')">AI 客服</a>
          <span class="divider">|</span>
          <a href="#" @click.prevent>帮助中心</a>
        </div>
        <p>&copy; 2024 智行票务 · 基于大模型与向量检索的智能客服管理系统</p>
        <p class="footer-sub">本项目仅供学习展示使用 · 技术栈：SpringBoot + Vue3 + LangChain4j + Milvus</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, ArrowDown, UserFilled, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')

function goTo(path) {
  router.push(path)
}

function handleCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'my-orders':
      router.push('/my-orders')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      handleLogout()
      break
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  } catch {
    // cancelled
  }
}
</script>

<style lang="scss" scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* ========== 顶部工具栏 ========== */
.top-bar {
  background: #ffffff;
  border-bottom: 1px solid var(--border-light);

  .top-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 10px 24px;
    display: flex;
    align-items: center;
    gap: 24px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;

    .logo-circle {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
    }

    .logo-text {
      display: flex;
      flex-direction: column;
      line-height: 1.15;

      .logo-title {
        font-size: 19px;
        font-weight: 700;
        color: #1e293b;
        letter-spacing: 1px;
      }

      .logo-sub {
        font-size: 11px;
        color: var(--text-tertiary);
        letter-spacing: 0.5px;
        margin-top: 2px;
      }
    }
  }

  .search-wrapper {
    flex: 1;
    max-width: 420px;
    display: flex;
    align-items: center;
    gap: 8px;

    .search-btn {
      padding: 0 16px;
      height: 40px;
    }
  }

  .top-right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    .top-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--text-tertiary);
      font-size: 13px;
      transition: color 0.2s;

      &:hover {
        color: var(--primary-blue);
      }
    }

    .divider {
      color: var(--border-medium);
      font-size: 12px;
    }
  }

  .auth-area {
    display: flex;
    align-items: center;
    margin-left: 12px;
    padding-left: 12px;
    border-left: 1px solid var(--border-light);
    gap: 10px;

    .login-btn,
    .register-btn {
      padding: 6px 16px;
      border-radius: var(--radius-xs);
      font-size: 13px;
      transition: all 0.2s ease;
      font-weight: 500;
    }

    .login-btn {
      color: var(--primary-blue);
      border: 1px solid var(--border-blue);
      background: var(--primary-blue-bg);

      &:hover {
        background: var(--primary-blue-lighter);
        border-color: var(--primary-blue);
      }
    }

    .register-btn {
      background: var(--primary-blue);
      color: #fff;

      &:hover {
        background: var(--primary-blue-deep);
      }
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    padding: 4px 10px;
    border-radius: 4px;
    transition: background 0.2s;

    :deep(.el-avatar) {
      background: var(--primary-blue);
    }

    &:hover {
      background: var(--primary-blue-bg);
    }

    .username {
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 500;
    }
  }
}

/* ========== 主导航栏（蓝色） ========== */
.main-nav {
  background: var(--gradient-nav);
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.25);

  .nav-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    height: 42px;
    align-items: stretch;
  }

  .nav-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 0 22px;
    color: rgba(255, 255, 255, 0.92);
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
    white-space: nowrap;
    text-decoration: none;

    .caret {
      font-size: 11px;
      opacity: 0.75;
    }

    &:hover,
    &.nav-active {
      background: rgba(255, 255, 255, 0.12);
      color: #fff;
    }

    &.nav-active {
      box-shadow: inset 0 -3px 0 var(--primary-orange);
      font-weight: 600;
    }

    /* AI 智能助手 特殊高亮 */
    &.nav-ai {
      background: linear-gradient(135deg, rgba(249, 115, 22, 0.85) 0%, rgba(251, 146, 60, 0.85) 100%);
      font-weight: 600;
      gap: 6px;

      .ai-spark {
        font-size: 16px;
        animation: spark 2s ease-in-out infinite;
      }

      &:hover {
        background: linear-gradient(135deg, var(--primary-orange) 0%, #fb923c 100%);
        color: #fff;
      }

      &.nav-active {
        background: linear-gradient(135deg, var(--primary-orange-deep) 0%, var(--primary-orange) 100%);
        box-shadow: inset 0 -3px 0 #fff;
      }
    }

    .nav-dropdown {
      position: absolute;
      top: 100%;
      left: 0;
      min-width: 160px;
      background: #fff;
      border: 1px solid var(--border-light);
      border-top: none;
      border-radius: 0 0 var(--radius-sm) var(--radius-sm);
      box-shadow: 0 6px 16px rgba(15, 23, 42, 0.1);
      display: none;
      z-index: 200;
      padding: 4px 0;

      .drop-item {
        display: block;
        padding: 10px 18px;
        color: var(--text-secondary);
        font-size: 14px;
        font-weight: 400;
        transition: all 0.15s;

        &:hover {
          background: var(--primary-blue-bg);
          color: var(--primary-blue-deep);
        }
      }
    }

    &.has-dropdown:hover .nav-dropdown {
      display: block;
    }
  }
}

@keyframes spark {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.85; }
}

/* ========== 内容区 ========== */
.user-main {
  flex: 1;
  width: 100%;
}

/* ========== 页脚 ========== */
.user-footer {
  background: #1e293b;
  color: #cbd5e1;
  margin-top: 40px;

  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 32px 24px 24px;
    text-align: center;
  }

  .footer-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-bottom: 16px;

    .footer-logo {
      font-size: 18px;
      font-weight: 700;
      color: #fff;
      letter-spacing: 1px;
    }

    .footer-slogan {
      font-size: 12px;
      color: #94a3b8;
      padding: 4px 10px;
      border: 1px solid #334155;
      border-radius: 12px;
    }
  }

  .footer-links {
    margin-bottom: 14px;
    font-size: 13px;

    a {
      color: #cbd5e1;
      transition: color 0.2s;

      &:hover {
        color: #fff;
      }
    }

    .divider {
      margin: 0 10px;
      color: #475569;
    }
  }

  p {
    font-size: 12px;
    color: #94a3b8;
    margin: 0 0 6px;
  }

  .footer-sub {
    font-size: 11px;
    color: #64748b;
    margin: 0;
  }
}

/* Keep navigation and account controls usable on narrow screens. */
@media (max-width: 760px) {
  .top-bar .top-inner { flex-wrap: wrap; padding: 12px 16px; gap: 12px; }
  .top-bar .search-wrapper { order: 3; flex-basis: 100%; max-width: none; }
  .top-bar .top-right { margin-left: auto; }
  .top-bar .top-right .top-link, .top-bar .top-right .divider { display: none; }
  .main-nav .nav-inner { padding: 0 8px; flex-wrap: wrap; height: auto; min-height: 44px; }
  .main-nav .nav-item { padding: 12px 10px; font-size: 14px; }
  .user-footer .footer-brand { flex-wrap: wrap; }
}
</style>
