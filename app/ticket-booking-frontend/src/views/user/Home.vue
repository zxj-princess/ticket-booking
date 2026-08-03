<template>
  <div class="home-page">
    <!-- ===== Banner 区：火车背景 + 左侧搜索卡 + 右侧APP推广 ===== -->
    <section class="banner-section">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <!-- 左侧：车票搜索卡片 -->
        <div class="ticket-search-card">
          <!-- 左侧功能图标区 -->
          <div class="search-sidebar">
            <div class="sidebar-item active">
              <el-icon><Tickets /></el-icon>
              <span>车票</span>
            </div>
            <div class="sidebar-item">
              <el-icon><Search /></el-icon>
              <span>常用查询</span>
            </div>
            <div class="sidebar-item">
              <el-icon><Dish /></el-icon>
              <span>订餐</span>
            </div>
          </div>
          <!-- 右侧搜索内容 -->
          <div class="search-main">
            <!-- 标签：单程 / 往返 / 中转换乘 / 适次改签 -->
            <div class="search-tabs">
              <div
                v-for="tab in ticketTabs"
                :key="tab.value"
                class="tab-item"
                :class="{ active: activeTab === tab.value }"
                @click="activeTab = tab.value"
              >
                <el-radio v-model="activeTab" :label="tab.value">{{ tab.label }}</el-radio>
              </div>
            </div>
            <!-- 搜索表单 -->
            <el-form :model="searchForm" class="search-form" @submit.prevent="handleSearch">
              <div class="form-row-1">
                <div class="form-field">
                  <label>出发地</label>
                  <div class="field-input">
                    <el-select
                      v-model="searchForm.fromStation"
                      filterable
                      placeholder="简拼/全拼汉字"
                      class="station-select"
                      value-key="value"
                      clearable
                    >
                      <el-option
                        v-for="station in stationOptions"
                        :key="station.id"
                        :label="station.label"
                        :value="station.value"
                      >
                        <div class="station-option">
                          <span class="station-name">{{ station.label }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                </div>
                <el-button class="swap-btn" :icon="Sort" circle @click="swapStations" />
                <div class="form-field">
                  <label>到达地</label>
                  <div class="field-input">
                    <el-select
                      v-model="searchForm.toStation"
                      filterable
                      placeholder="请输入到达站"
                      class="station-select"
                      value-key="value"
                      clearable
                    >
                      <el-option
                        v-for="station in stationOptions"
                        :key="station.id"
                        :label="station.label"
                        :value="station.value"
                      >
                        <div class="station-option">
                          <span class="station-name">{{ station.label }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                </div>
                <div class="form-field date-field">
                  <label>出发日期</label>
                  <div class="field-input">
                    <el-date-picker
                      v-model="searchForm.date"
                      type="date"
                      placeholder="2026-08-01"
                      value-format="YYYY-MM-DD"
                      :disabled-date="disabledDate"
                      class="date-picker"
                    />
                  </div>
                </div>
              </div>
              <div class="form-row-2">
                <div class="checkboxes">
                  <el-checkbox v-model="searchForm.student">学生</el-checkbox>
                  <el-checkbox v-model="searchForm.highSpeed">高铁动车</el-checkbox>
                </div>
                <el-button type="primary" size="large" class="query-btn" @click="handleSearch">
                  查 询
                </el-button>
              </div>
            </el-form>
          </div>
        </div>
        <!-- 右侧：AI 智能助手推广 -->
        <div class="ai-assistant-card">
          <div class="ai-header">
            <div class="ai-avatar">
              <el-icon :size="28"><ChatDotRound /></el-icon>
              <span class="ai-pulse"></span>
            </div>
            <div class="ai-title-box">
              <h3>AI 智能助手</h3>
              <span class="ai-online">● 在线</span>
            </div>
          </div>
          <div class="ai-body">
            <p class="ai-desc">基于大语言模型，一句话搞定查票、购票、退改签</p>
            <ul class="ai-features">
              <li><el-icon><Tickets /></el-icon>自然语言查车票</li>
              <li><el-icon><Search /></el-icon>智能意图识别</li>
              <li><el-icon><ChatDotRound /></el-icon>多轮对话记忆</li>
            </ul>
            <el-button type="primary" class="ai-cta" @click="goToAiChat">
              立即体验 AI 对话
              <el-icon class="cta-arrow"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <!-- 轮播指示点 -->
      <div class="banner-dots">
        <span class="dot active"></span>
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
    </section>

    <!-- ===== 功能图标行：核心服务入口 ===== -->
    <section class="feature-icons-section">
      <div class="icon-item" v-for="item in featureIcons" :key="item.label" @click="item.action && item.action()">
        <div class="icon-box" :class="item.theme">
          <el-icon :size="28">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <span class="icon-label">{{ item.label }}</span>
      </div>
    </section>

    <!-- ===== 双卡片区：AI 智能客服 + 便捷服务 ===== -->
    <section class="service-cards-section">
      <!-- AI 智能客服 -->
      <div class="service-card ai-card" @click="goToAiChat">
        <div class="card-left-bar"></div>
        <div class="card-body">
          <div class="card-header">
            <h3 class="card-title">AI 智能客服</h3>
            <p class="card-subtitle">基于大模型 + 向量知识库<br/>7×24 小时智能问答，秒级响应</p>
          </div>
          <div class="card-footer">
            <span class="more-link">立即对话 &rsaquo;</span>
          </div>
        </div>
        <div class="card-decoration">
          <el-icon :size="72"><ChatDotRound /></el-icon>
        </div>
      </div>
      <!-- 便捷出行服务 -->
      <div class="service-card service-card-orange">
        <div class="card-body">
          <div class="card-header">
            <h3 class="card-title">便捷出行服务</h3>
            <p class="card-subtitle">在线选座 · 一键购票<br/>订单管理 · 退改无忧</p>
          </div>
          <div class="card-footer">
            <span class="more-link" @click="goToSearch">去购票 &rsaquo;</span>
          </div>
        </div>
        <div class="card-decoration">
          <el-icon :size="68"><Tickets /></el-icon>
        </div>
      </div>
    </section>

    <!-- ===== 我的最近订单 ===== -->
    <section class="section" v-if="userStore.isLoggedIn">
      <div class="section-header">
        <h2>我的最近订单</h2>
        <router-link to="/my-orders" class="view-all">查看全部 &rsaquo;</router-link>
      </div>
      <div class="recent-orders" v-loading="loadingOrders">
        <div v-if="recentOrders.length === 0 && !loadingOrders" class="empty-state">
          <el-empty description="暂无订单记录，快去预订一张车票吧！" />
        </div>
        <div v-else class="order-list">
          <div
            v-for="order in recentOrders"
            :key="order.id"
            class="order-item"
            @click="goOrderDetail(order)"
          >
            <div class="order-main">
              <div class="order-train">
                <span class="train-no">{{ order.trainNo }}</span>
                <el-tag :type="orderStatusTag(order.statusStr)" size="small">
                  {{ orderStatusText(order.statusStr) }}
                </el-tag>
              </div>
              <div class="order-route">
                <div class="route-station">
                  <span class="time">{{ order.departTimeStr || order.departTime }}</span>
                  <span class="station">{{ order.fromStation }}</span>
                </div>
                <div class="route-line">
                  <span class="line"></span>
                  <span class="duration">{{ order.durationText || '--' }}</span>
                  <el-icon class="arrow-icon"><ArrowRight /></el-icon>
                  <span class="line"></span>
                </div>
                <div class="route-station">
                  <span class="time">{{ order.arriveTimeStr || order.arriveTime }}</span>
                  <span class="station">{{ order.toStation }}</span>
                </div>
              </div>
              <div class="order-seat">{{ order.seatLabel || order.seatType }} · {{ order.passengerName }}</div>
            </div>
            <div class="order-price">
              <span class="currency">¥</span>
              <span class="amount">{{ order.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 热门线路 ===== -->
    <section class="section">
      <div class="section-header">
        <h2>热门线路</h2>
        <span class="section-desc">精选热门线路，便捷出行</span>
      </div>
      <div class="route-grid">
        <div
          v-for="route in popularRoutes"
          :key="route.from + route.to"
          class="route-card"
          @click="goSearch(route)"
        >
          <div class="route-header">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="train-icon"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
            <el-tag size="small">{{ route.type }}字头</el-tag>
          </div>
          <div class="route-cities">
            <span class="city from">{{ route.from }}</span>
            <div class="arrow-line">
              <span class="dash"></span>
              <el-icon><ArrowRight /></el-icon>
              <span class="dash"></span>
            </div>
            <span class="city to">{{ route.to }}</span>
          </div>
          <div class="route-footer">
            <span class="route-time">{{ route.time }}</span>
            <span class="route-price">¥{{ route.price }}<small>起</small></span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Search, ArrowRight, Sort, Tickets,
  ChatDotRound, Promotion, Document, Wallet, Service, Reading
} from '@element-plus/icons-vue'
import { listStations } from '@/api/station'
import { getMyOrders } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const ticketTabs = ref([
  { label: '单程', value: 'one-way' },
  { label: '往返', value: 'round-trip' },
  { label: '中转换乘', value: 'transfer' }
])
const activeTab = ref('one-way')

const searchForm = ref({
  fromStation: '',
  toStation: '',
  date: '',
  student: false,
  highSpeed: true
})

const stationOptions = ref([])
const loadingOrders = ref(false)
const recentOrders = ref([])

const featureIcons = ref([
  { icon: 'ChatDotRound', label: 'AI 客服', theme: 'theme-ai', action: () => router.push('/ai-chat') },
  { icon: 'Search', label: '车票查询', theme: 'theme-blue', action: () => router.push('/search') },
  { icon: 'Tickets', label: '我的订单', theme: 'theme-orange', action: () => router.push('/my-orders') },
  { icon: 'Promotion', label: '订单改签', theme: 'theme-blue', action: () => router.push('/my-orders') },
  { icon: 'Wallet', label: '车票退票', theme: 'theme-orange', action: () => router.push('/my-orders') },
  { icon: 'Document', label: '乘客管理', theme: 'theme-blue', action: () => router.push('/profile') },
  { icon: 'Reading', label: '出行指南', theme: 'theme-blue' }
])

const popularRoutes = ref([
  { from: '北京', to: '上海', time: '约4小时30分', price: 553, type: 'G' },
  { from: '广州', to: '深圳', time: '约30分钟', price: 74.5, type: 'G' },
  { from: '上海', to: '杭州', time: '约1小时', price: 73, type: 'G' },
  { from: '北京', to: '广州', time: '约8小时', price: 862, type: 'G' },
  { from: '成都', to: '重庆', time: '约1小时30分', price: 154, type: 'G' },
  { from: '西安', to: '郑州', time: '约2小时', price: 229, type: 'G' }
])

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date()
  maxDate.setDate(maxDate.getDate() + 15)
  return date < today || date > maxDate
}

function swapStations() {
  const temp = searchForm.value.fromStation
  searchForm.value.fromStation = searchForm.value.toStation
  searchForm.value.toStation = temp
}

function handleSearch() {
  if (!searchForm.value.fromStation || !searchForm.value.toStation) {
    ElMessage.warning('请选择出发站和到达站')
    return
  }
  if (!searchForm.value.date) {
    ElMessage.warning('请选择出发日期')
    return
  }
  router.push({
    path: '/search',
    query: {
      from: searchForm.value.fromStation,
      to: searchForm.value.toStation,
      date: searchForm.value.date,
      highSpeed: searchForm.value.highSpeed ? '1' : '0'
    }
  })
}

function goSearch(route) {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const dateStr = tomorrow.toISOString().slice(0, 10)
  router.push({
    path: '/search',
    query: { from: route.from, to: route.to, date: dateStr }
  })
}

function goToAiChat() {
  router.push('/ai-chat')
}

function goToSearch() {
  router.push('/search')
}

function goOrderDetail(order) {
  router.push(`/order/${order.trainId}`)
}

function orderStatusTag(status) {
  const map = { PENDING: 'warning', COMPLETED: 'success', REFUNDED: 'info' }
  return map[status] || 'info'
}

function orderStatusText(status) {
  const map = { PENDING: '待出行', COMPLETED: '已完成', REFUNDED: '已退票' }
  return map[status] || status
}

async function fetchStations() {
  try {
    const res = await listStations()
    const data = res.data || []
    if (data.length > 0 && typeof data[0] === 'object') {
      stationOptions.value = data.map(s => ({
        value: s.name,
        label: s.name,
        pinyin: s.pinyin || '',
        id: s.id,
        name: s.name
      }))
    } else {
      stationOptions.value = (Array.isArray(data) ? data : []).map(name => ({ value: name, label: name, pinyin: '', id: name }))
    }
  } catch {
    stationOptions.value = [
      { value: '北京', label: '北京', pinyin: 'beijing', id: 1 },
      { value: '上海', label: '上海', pinyin: 'shanghai', id: 2 },
      { value: '广州', label: '广州', pinyin: 'guangzhou', id: 3 },
      { value: '深圳', label: '深圳', pinyin: 'shenzhen', id: 4 },
      { value: '杭州', label: '杭州', pinyin: 'hangzhou', id: 5 },
      { value: '成都', label: '成都', pinyin: 'chengdu', id: 6 },
      { value: '重庆', label: '重庆', pinyin: 'chongqing', id: 7 },
      { value: '西安', label: '西安', pinyin: 'xian', id: 8 },
      { value: '郑州', label: '郑州', pinyin: 'zhengzhou', id: 9 },
      { value: '武汉', label: '武汉', pinyin: 'wuhan', id: 10 },
      { value: '南京', label: '南京', pinyin: 'nanjing', id: 11 },
      { value: '长沙', label: '长沙', pinyin: 'changsha', id: 12 }
    ]
  }
  if (!searchForm.value.fromStation && stationOptions.value.some(s => s.value === '北京')) {
    searchForm.value.fromStation = '北京'
  }
  if (!searchForm.value.toStation && stationOptions.value.some(s => s.value === '上海')) {
    searchForm.value.toStation = '上海'
  }
}

async function fetchRecentOrders() {
  if (!userStore.isLoggedIn) return
  loadingOrders.value = true
  try {
    const res = await getMyOrders({ page: 1, size: 5 })
    recentOrders.value = res.data?.records || res.data?.list || res.data || []
  } catch {
    recentOrders.value = []
  } finally {
    loadingOrders.value = false
  }
}

onMounted(() => {
  const today = new Date()
  today.setDate(today.getDate() + 1)
  searchForm.value.date = today.toISOString().slice(0, 10)
  fetchStations()
  fetchRecentOrders()
})
</script>

<style lang="scss" scoped>
.home-page {
  padding-bottom: 20px;
}

/* =============== Banner 区 =============== */
.banner-section {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  margin-bottom: 20px;
}

.banner-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(219, 234, 254, 0.85) 0%, rgba(191, 219, 254, 0.6) 40%, rgba(147, 197, 253, 0.2) 100%),
    url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='1200' height='360' viewBox='0 0 1200 360'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0%25' stop-color='%23dbeafe'/><stop offset='60%25' stop-color='%2393c5fd'/><stop offset='100%25' stop-color='%23bfdbfe'/></linearGradient></defs><rect fill='url(%23g)' width='1200' height='360'/><g stroke='%231d4ed8' stroke-width='2' opacity='0.15'><line x1='0' y1='320' x2='1200' y2='320'/><line x1='0' y1='328' x2='1200' y2='328'/></g><g fill='%231e40af' opacity='0.08'><rect x='50' y='260' width='800' height='50' rx='10'/><rect x='900' y='270' width='200' height='40' rx='8'/></g></svg>") center/cover no-repeat;
  border-radius: var(--radius-md);
  z-index: 0;
}

.banner-content {
  position: relative;
  z-index: 1;
  padding: 28px 24px 36px;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
}

/* === 搜索卡片 === */
.ticket-search-card {
  background: #ffffff;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-search);
  display: flex;
  overflow: hidden;
  min-height: 280px;
}

.search-sidebar {
  width: 72px;
  background: #e8f1fb;
  display: flex;
  flex-direction: column;
  padding: 14px 0;

  .sidebar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 14px 8px;
    font-size: 12px;
    color: #475569;
    cursor: pointer;
    transition: all 0.15s;

    .el-icon { font-size: 20px; }

    &.active {
      background: #ffffff;
      color: var(--primary-blue);
      font-weight: 600;
      border-left: 3px solid var(--primary-blue);
      padding-left: 5px;
    }

    &:hover:not(.active) {
      background: rgba(255, 255, 255, 0.5);
    }
  }
}

.search-main {
  flex: 1;
  padding: 18px 24px;
  display: flex;
  flex-direction: column;
}

.search-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);

  .tab-item {
    :deep(.el-radio) {
      margin-right: 0;
      .el-radio__label { padding-left: 4px; font-size: 14px; }
    }

    &.active :deep(.el-radio__label) {
      color: var(--primary-blue);
      font-weight: 600;
    }
  }
}

.search-form {
  :deep(.el-form-item) { margin-bottom: 0; }

  .form-row-1 {
    display: grid;
    grid-template-columns: 1fr auto 1fr 1.1fr;
    gap: 12px;
    align-items: center;
    margin-bottom: 18px;
  }

  .form-field {
    display: flex;
    flex-direction: column;
    gap: 6px;

    label {
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 500;
    }
  }

  .field-input { width: 100%; }
  .station-select { width: 100%; }
  .date-picker { width: 100%; }

  .station-option {
    display: flex;
    align-items: center;
    gap: 8px;

    .station-name {
      font-weight: 500;
      color: #303133;
    }

    .station-pinyin {
      font-size: 12px;
      color: #909399;
    }
  }

  .swap-btn {
    margin-top: 18px;
    background: #fff;
    border: 1px solid var(--border-medium);
    color: var(--primary-blue);
    transition: all 0.2s;

    &:hover {
      background: var(--primary-blue-bg);
      border-color: var(--primary-blue);
      transform: rotate(90deg);
    }
  }

  .form-row-2 {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;

    .checkboxes {
      display: flex;
      gap: 20px;

      :deep(.el-checkbox__label) {
        color: var(--text-secondary);
        font-size: 13px;
      }
    }
  }

  .query-btn {
    width: 220px;
    height: 40px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 8px;
    padding: 0 24px;
  }
}

/* === AI 智能助手推广卡 === */
.ai-assistant-card {
  background: linear-gradient(160deg, #1e40af 0%, #2563eb 60%, #3b82f6 100%);
  border-radius: var(--radius-sm);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.28);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  color: #fff;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: -40px;
    right: -40px;
    width: 120px;
    height: 120px;
    background: radial-gradient(circle, rgba(249, 115, 22, 0.35) 0%, transparent 70%);
    border-radius: 50%;
  }
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 12px;
  position: relative;
  z-index: 1;

  .ai-avatar {
    position: relative;
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.18);
    backdrop-filter: blur(6px);
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid rgba(255, 255, 255, 0.3);
  }

  .ai-pulse {
    position: absolute;
    top: -2px;
    right: -2px;
    width: 12px;
    height: 12px;
    background: #22c55e;
    border-radius: 50%;
    border: 2px solid #2563eb;
    animation: pulse 1.8s ease-in-out infinite;
  }

  .ai-title-box {
    h3 {
      font-size: 18px;
      margin: 0;
      font-weight: 700;
      letter-spacing: 0.5px;
    }

    .ai-online {
      font-size: 11px;
      color: #86efac;
      font-weight: 500;
    }
  }
}

.ai-body {
  padding: 6px 20px 22px;
  position: relative;
  z-index: 1;

  .ai-desc {
    font-size: 13px;
    line-height: 1.6;
    margin: 0 0 14px;
    opacity: 0.95;
  }

  .ai-features {
    list-style: none;
    padding: 0;
    margin: 0 0 18px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    li {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12.5px;
      opacity: 0.92;

      .el-icon {
        font-size: 14px;
        color: #fdba74;
      }
    }
  }

  .ai-cta {
    width: 100%;
    height: 38px;
    background: var(--gradient-button);
    border: none;
    font-weight: 600;
    font-size: 14px;
    letter-spacing: 1px;
    box-shadow: 0 4px 12px rgba(249, 115, 22, 0.4);

    .cta-arrow {
      margin-left: 4px;
      transition: transform 0.2s;
    }

    &:hover .cta-arrow {
      transform: translateX(3px);
    }
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
}

/* 轮播指示 */
.banner-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 2;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.8);
    border: 1px solid rgba(37, 99, 235, 0.3);
    transition: all 0.2s;
    cursor: pointer;

    &.active {
      background: var(--primary-orange);
      width: 22px;
      border-radius: 4px;
      border-color: var(--primary-orange);
    }
  }
}

/* =============== 功能图标行 =============== */
.feature-icons-section {
  max-width: 1200px;
  margin: 0 auto 28px;
  background: #ffffff;
  border-radius: var(--radius-md);
  padding: 22px 24px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 16px;
  border: 1px solid var(--border-light);
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-4px);

    .icon-box {
      transform: scale(1.08);
      box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
    }

    .icon-label {
      color: var(--primary-blue);
    }
  }

  .icon-box {
    width: 54px;
    height: 54px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.25s;

    &.theme-blue {
      background: linear-gradient(135deg, var(--primary-blue-bg) 0%, #dbeafe 100%);
      color: var(--primary-blue);
    }

    &.theme-orange {
      background: linear-gradient(135deg, var(--primary-orange-bg) 0%, #ffedd5 100%);
      color: var(--primary-orange);
    }

    &.theme-ai {
      background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
      color: #fff;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }
  }

  .icon-label {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
    text-align: center;
    transition: color 0.2s;
  }
}

/* =============== 服务卡片 =============== */
.service-cards-section {
  max-width: 1200px;
  margin: 0 auto 28px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.service-card {
  position: relative;
  background: #ffffff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  overflow: hidden;
  min-height: 130px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;

  &:hover {
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-3px);
    border-color: var(--border-blue);
  }
}

.card-left-bar {
  width: 6px;
  background: var(--gradient-card-header);
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  z-index: 2;
  position: relative;
}

.card-header {
  .card-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--primary-blue-deep);
    margin: 0 0 6px;
  }

  .card-subtitle {
    margin: 0;
    font-size: 12px;
    color: var(--text-tertiary);
    line-height: 1.6;
  }
}

.card-footer {
  .more-link {
    display: inline-block;
    color: var(--primary-blue);
    font-size: 13px;
    font-weight: 500;
  }
}

.card-decoration {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--primary-blue-lighter);
  opacity: 0.6;
  z-index: 1;
}

.ai-card {
  background: linear-gradient(90deg, #fff 0%, #eff6ff 70%, #dbeafe 100%);

  .card-decoration {
    color: var(--primary-blue-light);
    opacity: 0.5;
  }

  &:hover {
    border-color: var(--primary-blue);
  }
}

.service-card-orange {
  background: linear-gradient(90deg, #fff 0%, #fff7ed 70%, #ffedd5 100%);

  .card-left-bar {
    width: 6px;
    background: linear-gradient(180deg, var(--primary-orange) 0%, #fb923c 100%);
    flex-shrink: 0;
  }

  .card-title {
    color: var(--primary-orange-deep) !important;
  }

  .more-link {
    color: var(--primary-orange) !important;
  }

  .card-decoration {
    color: var(--primary-orange-light);
    opacity: 0.55;
  }

  &:hover {
    border-color: var(--primary-orange);
  }
}

/* =============== 通用 Section =============== */
.section {
  max-width: 1200px;
  margin: 0 auto 32px;
  padding: 0 4px;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--primary-blue-bg);

  h2 {
    font-size: 18px;
    color: var(--primary-blue-deep);
    margin: 0;
    font-weight: 700;
    position: relative;
    padding-left: 12px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 4px;
      bottom: 4px;
      width: 4px;
      background: linear-gradient(180deg, var(--primary-orange) 0%, var(--primary-blue) 100%);
      border-radius: 2px;
    }
  }

  .section-desc {
    color: var(--text-muted);
    font-size: 13px;
  }

  .view-all {
    color: var(--primary-blue);
    font-size: 13px;
    font-weight: 500;
  }
}

/* =============== 最近订单 =============== */
.recent-orders {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px;
  border: 1px solid var(--border-light);
  min-height: 160px;
}

.empty-state {
  padding: 32px 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-radius: var(--radius-sm);
  background: var(--primary-blue-bg);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    background: #fff;
    border-color: var(--border-blue);
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  }

  .order-main {
    flex: 1;
  }

  .order-train {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;

    .train-no {
      font-weight: 700;
      font-size: 15px;
      color: var(--primary-blue-deep);
      font-family: 'DIN', monospace;
    }
  }

  .order-route {
    display: grid;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    gap: 14px;
    margin-bottom: 6px;

    .route-station {
      text-align: center;

      .time {
        display: block;
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary);
        font-family: 'DIN', monospace;
      }
      .station {
        display: block;
        font-size: 13px;
        color: var(--text-tertiary);
        margin-top: 2px;
      }
    }

    .route-line {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      position: relative;

      .line {
        flex: 1;
        max-width: 50px;
        height: 1px;
        background: var(--primary-blue);
      }
      .duration {
        font-size: 11px;
        color: var(--text-muted);
        white-space: nowrap;
      }
      .arrow-icon {
        color: var(--primary-blue);
        font-size: 14px;
      }
    }
  }

  .order-seat {
    font-size: 12px;
    color: var(--text-muted);
  }

  .order-price {
    text-align: right;

    .currency {
      color: var(--primary-orange);
      font-size: 13px;
      font-weight: 500;
    }

    .amount {
      color: var(--primary-orange);
      font-weight: 700;
      font-size: 22px;
      font-family: 'DIN', Arial, sans-serif;
      margin-left: 2px;
    }
  }
}

/* =============== 热门线路 =============== */
.route-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.route-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 18px 20px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    border-color: var(--primary-blue);
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(37, 99, 235, 0.1);
  }

  .route-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .train-icon {
      color: var(--primary-blue);
    }

    :deep(.el-tag) {
      border: 1px solid var(--border-blue);
      background: var(--primary-blue-bg);
      color: var(--primary-blue-deep);
    }
  }

  .route-cities {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    padding: 12px 0;
    background: linear-gradient(90deg, transparent 0%, var(--primary-blue-bg) 50%, transparent 100%);
    border-radius: var(--radius-sm);

    .city {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-primary);
      padding: 0 12px;
    }

    .arrow-line {
      display: flex;
      align-items: center;
      gap: 4px;
      color: var(--primary-blue);

      .dash {
        width: 18px;
        height: 2px;
        background: repeating-linear-gradient(90deg, var(--primary-blue) 0 4px, transparent 4px 6px);
      }
    }
  }

  .route-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .route-time {
      color: var(--text-muted);
      font-size: 13px;
    }

    .route-price {
      color: var(--primary-orange);
      font-weight: 700;
      font-size: 18px;
      font-family: 'DIN', monospace;

      small {
        font-size: 12px;
        color: var(--text-muted);
        font-weight: 400;
        margin-left: 2px;
      }
    }
  }
}
</style>
