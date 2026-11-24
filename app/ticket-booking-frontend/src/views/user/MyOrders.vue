<template>
  <div class="my-orders-page page-container">
    <!-- 页面标题 -->
    <div class="page-header-card">
      <div class="page-title">
        <el-icon :size="22" color="#2563eb"><Tickets /></el-icon>
        <span>我的订单</span>
      </div>
      <p class="title-desc">管理您的所有火车票订单</p>
    </div>

    <!-- 选项卡 -->
    <div class="tabs-card">
      <div class="tabs-row">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <em v-if="tab.count > 0" class="badge">{{ tab.count }}</em>
        </div>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list" v-loading="loading">
      <div v-if="filteredOrders.length === 0 && !loading" class="empty-state-card">
        <el-empty description="暂无相关订单" />
        <el-button type="primary" @click="goSearch" class="go-book-btn">
          <el-icon><Tickets /></el-icon>
          去订票
        </el-button>
      </div>

      <div v-for="order in filteredOrders" :key="order.id" class="order-card">
        <div class="card-header-bar">
          <div class="left-info">
            <span class="order-no-label">订单号：</span>
            <span class="order-no">{{ order.orderNumber || order.orderNo || order.id }}</span>
            <span class="order-date">{{ order.date || order.departDate || order.createdAt?.slice(0, 10) }}</span>
          </div>
          <el-tag
            :type="orderStatusTag(order.statusStr)"
            size="default"
            effect="light"
            class="status-tag"
          >
            {{ orderStatusText(order.statusStr) }}
          </el-tag>
        </div>

        <div class="card-body">
          <div class="train-route">
            <div class="station from">
              <div class="time">{{ order.departTimeStr || order.departTime }}</div>
              <div class="name">{{ order.fromStation }}</div>
            </div>
            <div class="route-mid">
              <div class="train-badge">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
                <span>{{ order.trainNo }}</span>
              </div>
              <div class="route-line">
                <span class="dash"></span>
                <span class="duration">{{ order.durationText || order.duration || '--' }}</span>
                <el-icon class="arrow"><ArrowRight /></el-icon>
                <span class="dash"></span>
              </div>
            </div>
            <div class="station to">
              <div class="time">{{ order.arriveTimeStr || order.arriveTime }}</div>
              <div class="name">{{ order.toStation }}</div>
            </div>
          </div>

          <div class="order-info-row">
            <div class="info-cell">
              <el-icon><User /></el-icon>
              <span>{{ order.passengerName }}</span>
            </div>
            <div class="info-cell">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2v8a4 4 0 0 0 4 4h4a4 4 0 0 0 4-4V2"/><path d="M6 14v6h12v-6"/><path d="M8 2h8"/></svg>
              <span>{{ order.seatLabel || order.seatType }}</span>
            </div>
            <div class="info-cell price-cell">
              <span class="label">票价：</span>
              <span class="price">¥{{ order.totalPrice || order.price }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer-bar">
          <div class="footer-left">
            <el-tag size="small" type="info" effect="plain">电子客票</el-tag>
            <span v-if="order.statusStr === 'PENDING'" class="tip-text">请按时乘车，发车前可办理退改签</span>
            <span v-if="order.statusStr === 'UNPAID'" class="tip-text">剩余支付时间 {{ remaining(order) }}；以服务器状态为准</span>
          </div>
          <div class="footer-actions"><el-button v-if="order.statusStr === 'UNPAID'" @click="handleCancel(order)">取消订单</el-button>
            <el-button
              v-if="order.statusStr === 'PENDING'"
              size="default"
              type="warning"
              class="action-btn"
              @click="handleChange(order)"
            >
              <el-icon><Refresh /></el-icon>
              改签
            </el-button>
            <el-button
              v-if="order.statusStr === 'PENDING'"
              size="default"
              type="danger"
              class="action-btn"
              @click="handleRefund(order)"
            >
              <el-icon><RefreshLeft /></el-icon>
              退票
            </el-button>
            <el-button
              v-if="order.statusStr === 'UNPAID'"
              size="default"
              type="primary"
              class="action-btn"
              @click="handlePay(order)"
            >
              <el-icon><Money /></el-icon>
              去支付
            </el-button>
            <el-button size="default" type="primary" class="action-btn" @click="handleDetail(order)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
<el-dialog v-model="detailOpen" title="订单处理记录" width="min(560px, 92vw)">
      <el-timeline v-if="detailEvents.length"><el-timeline-item v-for="(event, index) in detailEvents" :key="index" :timestamp="event.created_at">{{ event.detail }} <strong v-if="event.amount">¥{{ event.amount }}</strong></el-timeline-item></el-timeline>
      <el-empty v-else description="历史订单暂无操作记录" />
    </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Tickets, ArrowRight, Refresh, RefreshLeft, View, User, Money } from '@element-plus/icons-vue'
import { getMyOrders, payOrder, cancelOrder, getOrderEvents } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const detailOpen = ref(false)
const detailEvents = ref([])
const now = ref(Date.now())
let clockTimer
function remaining(order) {
  const seconds = Math.max(0, Math.floor((new Date(order.createTime).getTime() + 15 * 60000 - now.value) / 1000))
  return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
}
async function handleCancel(order) { await cancelOrder(order.orderNumber); await fetchOrders() }
const loading = ref(false)
const activeTab = ref('ALL')
const orders = ref([])

const tabs = computed(() => [
  { label: '全部订单', value: 'ALL', count: orders.value.length },
  { label: '待支付', value: 'UNPAID', count: orders.value.filter(o => o.statusStr === 'UNPAID').length },
  { label: '待出行', value: 'PENDING', count: orders.value.filter(o => o.statusStr === 'PENDING').length },
  { label: '已完成', value: 'COMPLETED', count: orders.value.filter(o => o.statusStr === 'COMPLETED').length },
  { label: '已退票', value: 'REFUNDED', count: orders.value.filter(o => o.statusStr === 'REFUNDED').length }
])

const filteredOrders = computed(() => {
  if (activeTab.value === 'ALL') return orders.value
  return orders.value.filter(o => o.statusStr === activeTab.value)
})

function orderStatusTag(status) {
  const map = { UNPAID: 'danger', PENDING: 'warning', COMPLETED: 'success', REFUNDED: 'info', CANCELLED: 'info' }
  return map[status] || 'info'
}
function orderStatusText(status) {
  const map = { UNPAID: '待支付', PENDING: '待出行', COMPLETED: '已完成', REFUNDED: '已退票', CANCELLED: '已取消' }
  return map[status] || status
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getMyOrders({})
    orders.value = res.data?.records || res.data?.list || res.data || []
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function handleChange(order) {
  router.push(`/change/${order.orderNumber || order.orderNo || order.id}`)
}
function handleRefund(order) {
  router.push(`/refund/${order.orderNumber || order.orderNo || order.id}`)
}
async function handlePay(order) {
  try {
    const orderNo = order.orderNumber || order.orderNo || order.id
    await payOrder(orderNo)
    ElMessage.success('支付成功！')
    fetchOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '支付失败，请重试')
  }
}
async function handleDetail(order) {
  const res = await getOrderEvents(order.orderNumber)
  detailEvents.value = res.data || []
  detailOpen.value = true
}

function goSearch() {
  router.push('/search')
}

onMounted(() => {
  fetchOrders()
  clockTimer = setInterval(() => { now.value = Date.now() }, 1000)
})
onUnmounted(() => clearInterval(clockTimer))
</script>

<style lang="scss" scoped>
.my-orders-page {
  padding-bottom: 40px;
}

/* 页头 */
.page-header-card {
  margin-bottom: 16px;

  .page-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 22px;
    font-weight: 700;
    color: var(--primary-blue-deep);
    margin-bottom: 4px;
  }
  .title-desc {
    margin: 0;
    color: var(--text-muted);
    font-size: 13px;
    padding-left: 30px;
  }
}

/* 选项卡 */
.tabs-card {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  padding: 0 24px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-card);

  .tabs-row {
    display: flex;
    gap: 8px;
  }
  .tab-item {
    position: relative;
    padding: 14px 20px;
    font-size: 15px;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.2s;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;

    &::after {
      content: '';
      position: absolute;
      left: 20px;
      right: 20px;
      bottom: 0;
      height: 3px;
      background: transparent;
      border-radius: 2px;
      transition: background 0.2s;
    }
    &:hover {
      color: var(--primary-blue);
    }
    &.active {
      color: var(--primary-blue);
      font-weight: 600;

      &::after {
        background: var(--primary-orange);
      }

      .badge {
        background: var(--primary-orange);
        color: #fff;
      }
    }
    .badge {
      display: inline-block;
      min-width: 20px;
      height: 20px;
      line-height: 20px;
      padding: 0 6px;
      border-radius: 10px;
      background: var(--primary-blue-bg);
      color: var(--primary-blue);
      font-size: 12px;
      text-align: center;
      font-style: normal;
      font-weight: 600;
    }
  }
}

/* 空状态 */
.empty-state-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 60px 0;
  text-align: center;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-card);

  .go-book-btn {
    margin-top: 20px;
    padding: 10px 28px;
    letter-spacing: 2px;
  }
}

/* 订单列表 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: all 0.2s;

  &:hover {
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.1);
    border-color: var(--border-blue);
    transform: translateY(-1px);
  }
}

/* 卡片头 */
.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(90deg, #eff6ff 0%, #dbeafe 50%, #f8fafc 100%);
  border-bottom: 1px solid var(--border-blue);

  .left-info {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-secondary);

    .order-no-label { color: var(--text-muted); }
    .order-no {
      font-family: 'Consolas', monospace;
      color: var(--primary-blue-deep);
      font-weight: 600;
      margin-right: 18px;
    }
    .order-date {
      color: var(--text-muted);
    }
  }
  .status-tag {
    font-weight: 600;
    padding: 4px 14px;
  }
}

/* 卡片体 */
.card-body {
  padding: 20px 24px 12px;
}

.train-route {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 20px;
  padding: 8px 0 20px;
  border-bottom: 1px dashed var(--border-light);

  .station {
    text-align: center;

    .time {
      font-size: 26px;
      font-weight: 700;
      font-family: 'DIN', monospace;
      color: var(--text-primary);
    }
    .name {
      font-size: 14px;
      color: var(--text-tertiary);
      margin-top: 4px;
    }
    &.from .time { color: var(--primary-blue-deep); }
  }

  .route-mid {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;

    .train-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 4px 14px;
      background: var(--primary-orange-bg);
      border: 1px solid var(--primary-orange-light);
      color: var(--primary-orange-deep);
      border-radius: 20px;
      font-weight: 700;
      font-size: 14px;

      .el-icon { font-size: 14px; }
    }
    .route-line {
      display: flex;
      align-items: center;
      gap: 6px;

      .dash {
        width: 50px;
        height: 2px;
        background: repeating-linear-gradient(90deg, var(--primary-blue-light) 0 4px, transparent 4px 8px);
      }
      .duration {
        font-size: 12px;
        color: var(--text-muted);
        white-space: nowrap;
      }
      .arrow {
        color: var(--primary-blue);
        font-size: 14px;
      }
    }
  }
}

.order-info-row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px 0 4px;

  .info-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-secondary);

    .el-icon { color: var(--primary-blue); font-size: 14px; }
  }
  .price-cell {
    margin-left: auto;
    font-size: 14px;

    .label { color: var(--text-muted); }
    .price {
      color: var(--primary-orange);
      font-size: 22px;
      font-weight: 700;
      font-family: 'DIN', monospace;
    }
  }
}

/* 卡片尾 */
.card-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #f8fafc;
  border-top: 1px solid var(--border-light);

  .footer-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .tip-text {
      font-size: 12px;
      color: var(--text-muted);
    }
  }
  .footer-actions {
    display: flex;
    gap: 10px;

    .action-btn {
      padding: 8px 18px;
      letter-spacing: 1px;
    }
  }
}
</style>
