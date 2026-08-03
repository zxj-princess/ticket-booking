<template>
  <div class="book-page">
    <div class="page-header">
      <h2>预订车票</h2>
      <p class="header-desc">请确认车次信息并选择乘客</p>
    </div>

    <div class="book-layout">
      <div class="book-main">
        <div class="card train-detail" v-loading="loading">
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>车次信息</span>
          </div>
          <div class="train-info-display">
            <div class="train-detail-row">
              <span class="detail-label">车次</span>
              <span class="detail-value train-no">{{ trainData.trainNo }}</span>
            </div>
            <div class="route-detail">
              <div class="station-block">
                <span class="station-time">{{ trainData.departTime }}</span>
                <span class="station-name">{{ trainData.fromStation }}</span>
              </div>
              <div class="route-arrow">
                <span class="line"></span>
                <span class="duration">{{ trainData.durationText || trainData.duration }}</span>
                <el-icon><ArrowRight /></el-icon>
                <span class="line"></span>
              </div>
              <div class="station-block">
                <span class="station-time">{{ trainData.arriveTime }}</span>
                <span class="station-name">{{ trainData.toStation }}</span>
              </div>
            </div>
            <div class="selected-seat">
              <span class="detail-label">座位类型</span>
              <el-tag type="primary" size="large">{{ selectedSeatLabel }}</el-tag>
              <span class="seat-price">¥{{ selectedSeatPrice }}</span>
            </div>
          </div>
        </div>

        <div class="card passenger-section">
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>选择乘客</span>
          </div>
          <el-form :model="bookingForm" :rules="passengerRules" ref="bookingFormRef" class="passenger-form">
            <el-form-item prop="passengerId">
              <el-select
                v-model="bookingForm.passengerId"
                placeholder="请选择乘客"
                class="passenger-select"
                filterable
              >
                <el-option
                  v-for="p in passengerList"
                  :key="p.id"
                  :label="`${p.name} - ${p.idCard}`"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
            <div class="passenger-info" v-if="selectedPassenger">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">姓名</span>
                  <span class="info-value">{{ selectedPassenger.name }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">身份证</span>
                  <span class="info-value">{{ selectedPassenger.idCard }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">手机号</span>
                  <span class="info-value">{{ selectedPassenger.phone }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">性别</span>
                  <span class="info-value">{{ selectedPassenger.gender === 'M' ? '男' : '女' }}</span>
                </div>
              </div>
            </div>
            <div class="no-passenger" v-else>
              <el-empty description="暂无乘客信息，请先到个人中心添加" :image-size="60" />
            </div>
          </el-form>
        </div>

        <div class="card contact-section">
          <div class="card-title">
            <el-icon><Message /></el-icon>
            <span>联系方式</span>
          </div>
          <el-form :model="bookingForm" label-width="80px">
            <el-form-item label="手机号">
              <el-input v-model="bookingForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="bookingForm.remark" type="textarea" :rows="2" placeholder="选填，备注信息" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="book-sidebar">
        <div class="card price-summary">
          <div class="card-title">
            <el-icon><Wallet /></el-icon>
            <span>费用明细</span>
          </div>
          <div class="price-items">
            <div class="price-item">
              <span>车票票价</span>
              <span class="price-value">¥{{ selectedSeatPrice }}</span>
            </div>
            <div class="price-item">
              <span>保险费</span>
              <span class="price-value">¥{{ insuranceFee }}</span>
            </div>
            <div class="price-item discount">
              <span>优惠</span>
              <span class="price-value">-¥{{ discount }}</span>
            </div>
            <div class="price-total">
              <span>合计</span>
              <span class="total-price">¥{{ totalPrice }}</span>
            </div>
          </div>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            提交订单
          </el-button>
          <p class="agreement">
            提交订单即表示同意
            <a href="#" @click.prevent="showAgreement">《火车票购票服务协议》</a>
          </p>
        </div>
      </div>
    </div>

    <el-dialog v-model="payDialogVisible" title="模拟支付" width="400px" :close-on-click-modal="false">
      <div class="pay-dialog-content">
        <div class="pay-amount">
          <span class="pay-label">支付金额</span>
          <span class="pay-price">¥{{ totalPrice }}</span>
        </div>
        <div class="pay-methods">
          <div
            v-for="method in payMethods"
            :key="method.value"
            class="pay-method"
            :class="{ active: payMethod === method.value }"
            @click="payMethod = method.value"
          >
            <el-icon :size="20"><component :is="method.icon" /></el-icon>
            <span>{{ method.label }}</span>
            <el-icon v-if="payMethod === method.value" class="check-icon"><Check /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelPay">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Tickets, ArrowRight, User, Message, Wallet, Check } from '@element-plus/icons-vue'
import { getTrainById } from '@/api/train'
import { createOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const trainData = ref({})
const passengerList = ref([])
const bookingFormRef = ref(null)
const payDialogVisible = ref(false)
const paying = ref(false)
const payMethod = ref('wechat')

const bookingForm = ref({
  passengerId: '',
  phone: '',
  remark: ''
})

const selectedSeatLabel = computed(() => route.query.seatLabel || '二等座')
const selectedSeatPrice = computed(() => Number(route.query.seatPrice) || 0)

const insuranceFee = computed(() => Math.round(selectedSeatPrice.value * 0.02))
const discount = computed(() => {
  if (selectedSeatPrice.value > 500) return 20
  if (selectedSeatPrice.value > 200) return 10
  return 0
})
const totalPrice = computed(() => {
  return Math.max(0, selectedSeatPrice.value + insuranceFee.value - discount.value)
})

const selectedPassenger = computed(() => {
  if (!bookingForm.value.passengerId) return null
  return passengerList.value.find(p => p.id === bookingForm.value.passengerId)
})

const canSubmit = computed(() => {
  return bookingForm.value.passengerId && bookingForm.value.phone && totalPrice.value > 0
})

const passengerRules = {
  passengerId: [{ required: true, message: '请选择乘客', trigger: 'change' }]
}

const payMethods = [
  { value: 'wechat', label: '微信支付', icon: 'ChatDotRound' },
  { value: 'alipay', label: '支付宝', icon: 'Money' },
  { value: 'bank', label: '银行卡', icon: 'CreditCard' }
]

async function fetchTrainDetail() {
  const trainId = route.params.trainId
  if (route.query.trainData) {
    try {
      trainData.value = JSON.parse(route.query.trainData)
      return
    } catch {}
  }
  loading.value = true
  try {
    const res = await getTrainById(trainId)
    trainData.value = res.data || {}
  } catch {
    trainData.value = { trainNo: '--', fromStation: '--', toStation: '--', departTime: '--', arriveTime: '--', duration: '--' }
  } finally {
    loading.value = false
  }
}

function fetchPassengers() {
  const userInfo = userStore.userInfo
  if (userInfo?.passengers && Array.isArray(userInfo.passengers)) {
    passengerList.value = userInfo.passengers
  } else {
    passengerList.value = [
      { id: 1, name: userInfo?.nickname || userInfo?.username || '张三', idCard: '110101199001011234', phone: userInfo?.phone || '13800138000', gender: 'M' },
      { id: 2, name: '李四', idCard: '110101199203054321', phone: '13900139000', gender: 'F' }
    ]
  }
  if (passengerList.value.length > 0 && !bookingForm.value.passengerId) {
    bookingForm.value.passengerId = passengerList.value[0].id
  }
  if (!bookingForm.value.phone && userInfo?.phone) {
    bookingForm.value.phone = userInfo.phone
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    ElMessage.warning('请完善订单信息')
    return
  }
  submitting.value = true
  try {
    await bookingFormRef.value?.validate()
    payDialogVisible.value = true
  } catch {
    ElMessage.warning('请完善订单信息')
  } finally {
    submitting.value = false
  }
}

function cancelPay() {
  payDialogVisible.value = false
}

function mapSeatType(seatType) {
  if (typeof seatType === 'number') return seatType
  const map = { 'BUSINESS': 0, 'FIRST_CLASS': 1, 'SECOND_CLASS': 2, 'SOFT_SLEEPER': 3, 'HARD_SLEEPER': 4, 'HARD_SEAT': 5 }
  return map[seatType] ?? 2
}

async function confirmPay() {
  paying.value = true
  try {
    const orderData = {
      trainId: Number(route.params.trainId),
      seatType: mapSeatType(route.query.seatType),
      passengerIds: [bookingForm.value.passengerId].filter(Boolean),
      seatCount: 1,
      date: route.query.date || new Date().toISOString().split('T')[0]
    }
    await createOrder(orderData)
    payDialogVisible.value = false
    ElMessage.success('支付成功！')
    router.push('/my-orders')
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}

function showAgreement() {
  ElMessageBox.alert(
    '1. 购票成功后，请按时乘车。\n2. 改签请在发车前24小时办理。\n3. 退票请遵循退票规则。\n4. 请妥善保管车票信息。',
    '购票服务协议',
    { confirmButtonText: '我知道了' }
  )
}

onMounted(() => {
  fetchTrainDetail()
  fetchPassengers()
})
</script>

<style lang="scss" scoped>
.book-page {
  .page-container {
    padding: var(--space-lg);
    max-width: 1200px;
    margin: 0 auto;
  }

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

.book-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.train-detail {
  .train-info-display {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .train-detail-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .detail-label {
      color: var(--text-muted);
      font-size: 14px;
    }

    .train-no {
      font-size: 22px;
      font-weight: 700;
      color: var(--primary-orange-deep);
    }
  }

  .route-detail {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    background: linear-gradient(90deg, var(--primary-blue-bg) 0%, #f0f9ff 50%, var(--primary-blue-bg) 100%);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-blue);

    .station-block {
      text-align: center;

      .station-time {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: var(--primary-blue-deep);
        font-family: 'DIN', monospace;
      }

      .station-name {
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

      .el-icon {
        font-size: 14px;
        color: var(--primary-orange);
      }
    }
  }

  .selected-seat {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    background: var(--gradient-button);
    border-radius: var(--radius-sm);
    color: #fff;
    box-shadow: var(--shadow-button);

    .detail-label {
      color: rgba(255, 255, 255, 0.92);
    }

    :deep(.el-tag) {
      background: rgba(255, 255, 255, 0.2) !important;
      border: 1px solid rgba(255, 255, 255, 0.4) !important;
      color: #fff !important;
    }

    .seat-price {
      margin-left: auto;
      font-size: 22px;
      font-weight: 700;
      font-family: 'DIN', monospace;
    }
  }
}

.passenger-section {
  .passenger-select {
    width: 100%;
  }

  .passenger-info {
    margin-top: 16px;
    padding: 16px;
    background: var(--primary-blue-bg);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-blue);
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
    padding: 10px 12px;
    background: #fff;
    border-radius: var(--radius-xs);

    .info-label {
      color: var(--text-muted);
      font-size: 13px;
    }

    .info-value {
      color: var(--text-primary);
      font-weight: 500;
    }
  }

  .no-passenger {
    padding: 20px 0;
  }
}

.passenger-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}

.contact-section {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}

.price-summary {
  position: sticky;
  top: 90px;

  .price-items {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 24px;
  }

  .price-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: var(--text-secondary);
    font-size: 14px;

    .price-value {
      color: var(--text-primary);
      font-weight: 500;
      font-family: 'DIN', monospace;
    }

    &.discount .price-value {
      color: #16a34a;
      font-weight: 600;
    }
  }

  .price-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 2px dashed var(--border-blue);

    span:first-child {
      color: var(--text-secondary);
      font-weight: 500;
    }

    .total-price {
      color: var(--primary-orange);
      font-size: 30px;
      font-weight: 700;
      font-family: 'DIN', monospace;
    }
  }

  .submit-btn {
    width: 100%;
    height: 46px;
    background: var(--gradient-button);
    border: none;
    border-radius: var(--radius-sm);
    margin-bottom: 12px;
    font-size: 16px;
    letter-spacing: 4px;
    font-weight: 600;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(249, 115, 22, 0.4);
    }

    &:active:not(:disabled) {
      transform: scale(0.98);
    }
  }

  .agreement {
    text-align: center;
    color: var(--text-muted);
    font-size: 12px;
    margin: 0;
    line-height: 1.5;

    a {
      color: var(--primary-blue);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.pay-dialog-content {
  .pay-amount {
    text-align: center;
    padding: 20px 0;
    border-bottom: 1px solid var(--border-blue);
    margin-bottom: 16px;
    background: linear-gradient(180deg, var(--primary-blue-bg) 0%, #fff 100%);
    border-radius: var(--radius-sm);

    .pay-label {
      display: block;
      color: var(--text-muted);
      font-size: 13px;
      margin-bottom: 8px;
    }

    .pay-price {
      font-size: 36px;
      font-weight: 700;
      color: var(--primary-orange);
      font-family: 'DIN', monospace;
    }
  }

  .pay-methods {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .pay-method {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border: 2px solid var(--border-light);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      border-color: var(--primary-blue-light);
      background: var(--primary-blue-bg);
    }

    &.active {
      border-color: var(--primary-blue);
      background: var(--primary-blue-lighter);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
    }

    > .el-icon {
      color: var(--primary-blue);
      font-size: 22px;
    }

    span {
      flex: 1;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .check-icon {
      color: var(--primary-blue-deep);
      font-size: 18px;
    }
  }
}
</style>