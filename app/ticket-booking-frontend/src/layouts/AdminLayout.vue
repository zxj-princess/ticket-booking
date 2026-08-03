<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="admin-aside">
      <div class="aside-header">
        <div class="logo-box">
          <span class="logo-circle">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
          </span>
        </div>
        <span v-show="!isCollapse" class="aside-title">智行票务 · 管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="rgba(255,255,255,0.88)"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-menu-item index="/admin/trains">
          <el-icon><Tickets /></el-icon>
          <template #title>车次管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/stations">
          <el-icon><Location /></el-icon>
          <template #title>站点管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/chat-records">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>AI对话管理</template>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer" v-show="!isCollapse">
        <el-icon :size="14"><Monitor /></el-icon>
        <span>v1.0.0 · 管理系统</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleCollapse">
            <component :is="isCollapse ? Expand : Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="sys-tip">
            <el-icon :size="14" color="#2563eb"><Bell /></el-icon>
            欢迎使用智行票务管理系统
          </span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="admin-user">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="admin-username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回前台</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  DataAnalysis,
  Tickets,
  Location,
  List,
  User,
  Fold,
  Expand,
  UserFilled,
  ArrowDown,
  ChatDotRound,
  Bell,
  Monitor
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const titleMap = {
  '/admin/dashboard': '数据看板',
  '/admin/trains': '车次管理',
  '/admin/stations': '站点管理',
  '/admin/orders': '订单管理',
  '/admin/users': '用户管理',
  '/admin/chat-records': 'AI对话管理'
}

const currentTitle = computed(() => titleMap[route.path] || '管理后台')

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

async function handleCommand(command) {
  switch (command) {
    case 'home':
      router.push('/home')
      break
    case 'logout':
      await handleLogout()
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
.admin-layout {
  height: 100vh;
}

/* ========== 侧边栏 - 12306蓝色渐变 ========== */
.admin-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #2563eb 55%, #3b82f6 100%);
  transition: width 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;

  .aside-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.15);
    white-space: nowrap;
    background: rgba(0, 0, 0, 0.12);
  }

  .logo-box {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: var(--brand-red);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  }

  .el-menu {
    border-right: none;
    padding: 12px 8px;
    flex: 1;

    :deep(.el-menu-item) {
      border-radius: 6px;
      margin-bottom: 4px;
      height: 44px;
      line-height: 44px;
      transition: all 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.14) !important;
      }

      &.is-active {
        background: linear-gradient(90deg, rgba(249, 115, 22, 0.85) 0%, rgba(251, 146, 60, 0.85) 100%) !important;
        font-weight: 600;
        box-shadow: 0 4px 10px rgba(249, 115, 22, 0.3);
      }
    }
  }

  .aside-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 14px 12px;
    border-top: 1px solid rgba(255, 255, 255, 0.15);
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
  }
}

/* ========== 顶部栏 ========== */
.admin-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  border-bottom: 1px solid var(--border-light);
  height: 60px !important;

  .header-left {
    display: flex;
    align-items: center;
    gap: 18px;
  }

  .collapse-btn {
    cursor: pointer;
    color: var(--text-secondary);
    padding: 6px;
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
      color: var(--primary-blue);
      background: var(--primary-blue-bg);
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 18px;

    .sys-tip {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--text-secondary);
      padding: 6px 14px;
      background: var(--primary-blue-bg);
      border-radius: 20px;
      border: 1px solid var(--border-blue);
    }
  }

  .admin-user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 12px;
    border-radius: 20px;
    transition: all 0.2s;

    &:hover {
      background: var(--primary-blue-bg);
    }

    .admin-username {
      font-size: 14px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    :deep(.el-avatar) {
      background: var(--primary-blue) !important;
    }
  }
}

.admin-main {
  background: var(--bg-page);
  padding: 20px;
  overflow-y: auto;
}

/* ========== 过渡 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
