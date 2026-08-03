<template>
  <div class="refund-page page-container">
    <div class="page-header">
      <h2>退票</h2>
      <p class="header-desc">申请退票，了解退票规则</p>
    </div>

    <div class="refund-layout">
      <div class="refund-main">
        <div class="card order-info">
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>订单信息</span>
          </div>
          <div class="info-content" v-loading="loading">
            <div class="info-row">
              <span class="label">车次</span>
              <span class="value train-no">{{ orderData.trainNo }}</span>
            </div>
            <div class="route-display">
              <div class="station-block">
                <span class="time">{{ orderData.departTimeStr || orderData.departTime }}</span>
                <span class="station">{{ orderData.fromStation }}</span>
              </div>
              <div class="route-arrow">
                <span class="line"></span>
                <span class="duration">{{ orderData.durationText || orderData.duration || '--' }}</span>
                <el-icon class="arrow-icon"><ArrowRight /></el-icon>
                <span class="line"></span>
              </div>
              <div class="station-block">
                <span class="time">{{ orderData.arriveTimeStr || orderData.arriveTime }}</span>
                <span class="station">{{ orderData.toStation }}</span>
              </div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">座位</span>
                <span class="value">{{ orderData.seatLabel }}</span>
              </div>
              <div class="info-item">
                <span class="label">乘客</span>
                <span class="value">{{ orderData.passengerName }}</span>
              </div>
              <div class="info-item">
                <span class="label">购票时间</span>
                <span class="value">{{ orderData.createTime || '--' }}</span>
              </div>
              <div class="info-item">
                <span class="label">订单状态</span>
                <el-tag :type="orderStatusTag" size="default" effect="light">{{ orderStatusText }}</el-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="card refund-reason">
          <div class="card-title">
            <el-icon><EditPen /></el-icon>
            <span>退票原因</span>
          </div>
          <el-form :model="refundForm" label-width="100px">
            <el-form-item label="退票原因" required>
              <el-radio-group v-model="refundForm.reason">
                <el-radio
                  v-for="reason in refundReasons"
                  :key="reason.value"
                  :value="reason.value"
                >
                  {{ reason.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="详细说明">
              <el-input
                v-model="refundForm.description"
                type="textarea"
                :rows="3"
                placeholder="请详细说明退票原因（选填）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <div class="card refund-rule">
          <div class="card-title">
            <el-icon><Warning /></el-icon>
            <span>退票规则</span>
          </div>
          <div class="rule-content">
            <div class="rule-item">
              <div class="rule-time-box">
                <el-icon><Calendar /></el-icon>
                <span class="rule-time">发车前15天以上</span>
              </div>
              <el-tag type="success" effect="light">免手续费</el-tag>
            </div>
            <div class="rule-item">
              <div class="rule-time-box">
                <el-icon><Calendar /></el-icon>
                <span class="rule-time">发车前48小时-15天</span>
              </div>
              <el-tag type="warning" effect="light">收取5%手续费</el-tag>
            </div>
            <div class="rule-item">
              <div class="rule-time-box">
                <el-icon><Calendar /></el-icon>
                <span class="rule-time">发车前24-48小时</span>
              </div>
              <el-tag type="warning" effect="light">收取10%手续费</el-tag>
            </div>
            <div class="rule-item">
              <div class="rule-time-box">
                <el-icon><Clock /></el-icon>
                <span class="rule-time">发车前24小时内</span>
              </div>
              <el-tag type="danger" effect="light">收取20%手续费</el-tag>
            </div>
            <div class="rule-item">
              <div class="rule-time-box">
                <el-icon><CircleClose /></el-icon>
                <span class="rule-time">发车后</span>
              </div>
              <el-tag type="info" effect="dark">不予退票</el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="refund-sidebar">
        <div class="card refund-summary">
          <div class="card-title">
            <el-icon><Wallet /></el-icon>
            <span>退票金额</span>
          </div>
          <div class="fee-calc">
            <div class="fee-row">
              <span>原票价</span>
              <span class="fee-value">¥{{ originalPrice }}</span>
            </div>
            <div class="fee-row">
              <span>保险费</span>
              <span class="fee-value">¥{{ insuranceFee }}</span>
            </div>
            <div class="fee-row highlight">
              <span>退票手续费</span>
              <span class="fee-value fee">-¥{{ refundFee }}</span>
            </div>
            <div class="fee-row">
              <span>优惠</span>
              <span class="fee-value discount">+¥{{ discount }}</span>
            </div>
            <div class="fee-total">
              <span>应退金额</span>
              <span class="total">¥{{ refundAmount }}</span>
            </div>
          </div>
          <div class="refund-rate">
            <el-progress :percentage="feeRate" :color="feeRateColor" :stroke-width="8" />
            <span class="rate-text">当前手续费率 {{ feeRate }}%</span>
          </div>
          <el-button
            type="danger"
            size="large"
            class="confirm-btn"
            :disabled="!canRefund"
            :loading="submitting"
            @click="confirmRefund"
          >
            确认退票
          </el-button>
          <p class="refund-tip">
            <el-icon :size="12"><InfoFilled /></el-icon>
            退票款将原路退回至支付账户，一般3-7个工作日到账
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Tickets, EditPen, Warning, Wallet, ArrowRight,
  Calendar, Clock, CircleClose, InfoFilled
} from '@element-plus/icons-vue'
import { getOrder, refundOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const orderData = ref({})

const refundForm = ref({
  reason: '',
  description: ''
})

const refundReasons = [
  { value: 'change_plan', label: '行程变更' },
  { value: 'wrong_order', label: '订票错误' },
  { value: 'health', label: '身体原因' },
  { value: 'weather', label: '天气原因' },
  { value: 'other', label: '其他原因' }
]

const originalPrice = computed(() => Number(orderData.value.totalPrice || orderData.value.price) || 0)
const insuranceFee = computed(() => Math.round(originalPrice.value * 0.02))
const discount = computed(() => {
  if (originalPrice.value > 500) return 20
  if (originalPrice.value > 200) return 10
  return 0
})

const feeRate = computed(() => {
  const now = new Date()
  const dateStr = orderData.value.date
  let departDate
  if (dateStr) {
    departDate = new Date(dateStr.replace(/-/g, '/'))
  } else if (orderData.value.departTime) {
    const dt = orderData.value.departTime
    if (typeof dt === 'string') {
      departDate = new Date(dt.replace(/-/g, '/'))
    } else if (Array.isArray(dt)) {
      departDate = new Date(dt[0], (dt[1] || 1) - 1, dt[2] || 1, dt[3] || 0, dt[4] || 0)
    }
  }
  if (!departDate || isNaN(departDate)) return 5
  const diffDays = Math.ceil((departDate - now) / (1000 * 60 * 60 * 24))
  if (diffDays >= 15) return 0
  if (diffDays >= 2) return 5
  if (diffDays >= 1) return 10
  return 20
})

const refundFee = computed(() => Math.round(originalPrice.value * feeRate.value / 100))
const refundAmount = computed(() => {
  return Math.max(0, originalPrice.value + insuranceFee.value - refundFee.value + discount.value)
})
const canRefund = computed(() => {
  return refundForm.value.reason && (orderData.value.statusStr === 'PENDING' || orderData.value.statusStr === 'UNPAID')
})
const feeRateColor = computed(() => {
  if (feeRate.value <= 5) return '#16a34a'
  if (feeRate.value <= 10) return '#f59e0b'
  return '#ef4444'
})
const orderStatusTag = computed(() => {
  const map = { UNPAID: 'danger', PENDING: 'warning', COMPLETED: 'success', REFUNDED: 'info' }
  return map[orderData.value.statusStr] || 'info'
})
const orderStatusText = computed(() => {
  const map = { UNPAID: '待支付', PENDING: '待出行', COMPLETED: '已完成', REFUNDED: '已退票' }
  return map[orderData.value.statusStr] || orderData.value.statusStr || '--'
})

async function fetchOrderDetail() {
  const orderId = route.params.orderId
  loading.value = true
  try {
    const res = await getOrder(orderId)
    orderData.value = res.data || {}
  } catch {
    ElMessage.error('获取订单信息失败')
  } finally {
    loading.value = false
  }
}

async function confirmRefund() {
  if (!canRefund.value) return
  submitting.value = true
  try {
    const orderNo = orderData.value.orderNumber || orderData.value.orderNo || route.params.orderId
    await ElMessageBox.confirm(
      `确认退票 ${orderData.value.trainNo}，应退金额 ¥${refundAmount.value}？`,
      '确认退票',
      { type: 'warning', confirmButtonText: '确认退票', cancelButtonText: '再想想' }
    )
    await refundOrder(orderNo)
    ElMessage.success('退票申请已提交，票款将原路退回')
    router.push('/my-orders')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('退票失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style lang="scss" scoped>
.refund-page {
  padding-bottom: 40px;

  .card {
    background: var(--bg-card);
    border-radius: var(--radius-md);
    padding: 24px;
    box-shadow: var(--shadow-card);
    margin-bottom: 20px;
    border: 1px solid var(--border-light);
    transition: box-shadow 0.25s ease;

    &:hover {
      box-shadow: var(--shadow-card-hover);
    }
  }

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--primary-blue-deep);
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 2px solid var(--primary-blue-bg);

    .el-icon {
      color: var(--primary-blue);
      font-size: 20px;
    }
  }
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 4px;
    color: var(--primary-blue-deep);
    font-size: 22px;
    font-weight: 700;
  }

  .header-desc {
    margin: 0;
    color: var(--text-muted);
    font-size: 13px;
  }
}

.refund-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.order-info {
  .info-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .info-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .label {
      color: var(--text-muted);
      min-width: 80px;
      font-size: 14px;
    }

    .train-no {
      font-size: 20px;
      font-weight: 700;
      color: var(--primary-orange-deep);
    }
  }

  .route-display {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    background: linear-gradient(90deg, var(--primary-blue-bg) 0%, #f0f9ff 50%, var(--primary-blue-bg) 100%);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-blue);

    .station-block {
      text-align: center;

      .time {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: var(--primary-blue-deep);
        font-family: 'DIN', monospace;
      }

      .station {
        display: block;
        color: var(--text-secondary);
        font-size: 14px;
        margin-top: 4px;
        font-weight: 500;
      }
    }

    .route-arrow {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      color: var(--text-muted);

      .line {
        width: 80px;
        height: 2px;
        background: repeating-linear-gradient(90deg, var(--primary-blue-light) 0 4px, transparent 4px 8px);
      }

      .duration {
        font-size: 12px;
        color: var(--text-secondary);
      }

      .arrow-icon {
        color: var(--primary-orange);
        font-size: 14px;
      }
    }
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 14px;
    background: var(--bg-soft);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-light);

    .label {
      color: var(--text-muted);
      font-size: 13px;
    }

    .value {
      color: var(--text-primary);
      font-weight: 500;
    }
  }
}

.refund-reason {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-radio-group) {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  :deep(.el-radio) {
    margin-right: 0;
  }
}

.refund-rule {
  .rule-content {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
  }

  .rule-item {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 16px;
    background: linear-gradient(135deg, var(--primary-blue-bg) 0%, #fff 100%);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-blue);

    .rule-time-box {
      display: flex;
      align-items: center;
      gap: 6px;

      .el-icon {
        color: var(--primary-blue);
        font-size: 16px;
      }

      .rule-time {
        color: var(--text-secondary);
        font-size: 14px;
        font-weight: 500;
      }
    }
  }
}

.refund-sidebar {
  .refund-summary {
    position: sticky;
    top: 90px;

    .fee-calc {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 20px;
    }

    .fee-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: var(--text-secondary);
      font-size: 14px;

      .fee-value {
        color: var(--text-primary);
        font-weight: 500;
        font-family: 'DIN', monospace;

        &.fee {
          color: var(--brand-red);
          font-weight: 600;
        }

        &.discount {
          color: #16a34a;
          font-weight: 600;
        }
      }

      &.highlight .fee-value {
        color: var(--brand-red);
        font-weight: 700;
      }
    }

    .fee-total {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 16px;
      border-top: 2px dashed var(--border-blue);

      span:first-child {
        color: var(--text-secondary);
        font-weight: 500;
      }

      .total {
        color: #16a34a;
        font-size: 30px;
        font-weight: 700;
        font-family: 'DIN', monospace;
      }
    }

    .refund-rate {
      margin-bottom: 20px;
      text-align: center;

      .rate-text {
        display: inline-block;
        margin-top: 8px;
        font-size: 12px;
        color: var(--text-muted);
      }
    }

    .confirm-btn {
      width: 100%;
      height: 46px;
      font-size: 16px;
      letter-spacing: 4px;
      font-weight: 600;
    }

    .refund-tip {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      text-align: center;
      color: var(--text-muted);
      font-size: 12px;
      margin: 12px 0 0;
      line-height: 1.5;

      .el-icon {
        color: var(--primary-blue);
      }
    }
  }
}
</style>
