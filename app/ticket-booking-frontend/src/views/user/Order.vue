<template>
  <div class="book-page page-container">
    <div class="page-header">
      <h2>预订车票</h2>
      <p class="header-desc">请确认车次信息并选择乘客</p>
    </div>

    <div class="book-layout">
      <!-- 左侧主内容 -->
      <div class="book-main">
        <!-- 车次信息 -->
        <div class="card-section train-detail" v-loading="loading">
          <div class="section-title">
            <div class="title-icon train-icon">
              <el-icon><Tickets /></el-icon>
            </div>
            <span>车次信息</span>
          </div>
          <div class="train-info-display">
            <div class="train-head-row">
              <div class="train-no-badge">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16"/><circle cx="8" cy="19" r="2"/><circle cx="16" cy="19" r="2"/></svg>
                <span>{{ trainData.trainNo }}</span>
              </div>
              <el-tag size="default" :type="trainTypeTag(trainData.trainTypeStr)">
                {{ trainTypeLabel(trainData.trainTypeStr) }}
              </el-tag>
            </div>

            <div class="route-detail">
              <div class="station-block">
                <span class="station-time">{{ trainData.departTimeStr || trainData.departTime }}</span>
                <span class="station-name">{{ trainData.fromStation }}</span>
                <span class="station-label">出发站</span>
              </div>
              <div class="route-arrow">
                <span class="dash"></span>
                <div class="duration-box">
                  <span class="duration-label">历时</span>
                  <span class="duration">{{ trainData.durationText || trainData.duration }}</span>
                </div>
                <el-icon class="arrow"><ArrowRight /></el-icon>
                <span class="dash"></span>
              </div>
              <div class="station-block">
                <span class="station-time">{{ trainData.arriveTimeStr || trainData.arriveTime }}</span>
                <span class="station-name">{{ trainData.toStation }}</span>
                <span class="station-label">到达站</span>
              </div>
            </div>

            <div class="selected-seat">
              <span class="label">座位类型</span>
              <el-tag type="primary" effect="light" size="large">{{ selectedSeatLabel }}</el-tag>
              <span class="seat-price">¥{{ selectedSeatPrice }}</span>
            </div>
          </div>
        </div>

        <!-- 乘客信息 -->
        <div class="card-section passenger-section">
          <div class="section-title">
            <div class="title-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <span>乘车人信息</span>
          </div>
          <el-form :model="bookingForm" :rules="passengerRules" ref="bookingFormRef" class="passenger-form">
            <el-form-item prop="passengerId">
              <el-select
                v-model="bookingForm.passengerId"
                placeholder="请选择乘车人"
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
              <el-empty description="暂无乘车人信息，请先到个人中心添加" :image-size="60" />
            </div>
          </el-form>
        </div>

        <!-- 联系方式 -->
        <div class="card-section contact-section">
          <div class="section-title">
            <div class="title-icon msg-icon">
              <el-icon><Message /></el-icon>
            </div>
            <span>联系方式</span>
          </div>
          <el-form :model="bookingForm" label-width="90px">
            <el-form-item label="手机号">
              <el-input v-model="bookingForm.phone" placeholder="请输入手机号，用于接收订单通知" maxlength="11" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="bookingForm.remark" type="textarea" :rows="2" placeholder="选填，备注信息" maxlength="100" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧费用汇总 -->
      <div class="book-sidebar">
        <div class="card-section price-summary sticky-card">
          <div class="section-title">
            <div class="title-icon wallet-icon">
              <el-icon><Wallet /></el-icon>
            </div>
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
              <span>优惠抵扣</span>
              <span class="price-value">-¥{{ discount }}</span>
            </div>
            <div class="price-total">
              <span>应付金额</span>
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
            <a href="#" @click.prevent="showAgreement">《12306购票服务协议》</a>
          </p>
        </div>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payDialogVisible" title="模拟支付" width="420px" :close-on-click-modal="false">
      <div class="pay-dialog-content">
        <div class="pay-amount">
          <span class="pay-label">支付金额</span>
          <span class="pay-price">¥{{ totalPrice }}</span>
        </div>
        <div class="pay-title">请选择支付方式</div>
        <div class="pay-methods">
          <div
            v-for="method in payMethods"
            :key="method.value"
            class="pay-method"
            :class="{ active: payMethod === method.value }"
            @click="payMethod = method.value"
          >
            <div class="method-left">
              <div class="method-icon" :class="method.value">
                <el-icon :size="22"><component :is="method.icon" /></el-icon>
              </div>
              <div class="method-info">
                <div class="method-name">{{ method.label }}</div>
                <div class="method-desc">{{ method.desc }}</div>
              </div>
            </div>
            <el-icon v-if="payMethod === method.value" class="check-icon"><CircleCheckFilled /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelPay">取消</el-button>
        <el-button type="primary" size="default" :loading="paying" @click="confirmPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Tickets, ArrowRight, User, Message, Wallet, Check, CircleCheckFilled } from '@element-plus/icons-vue'
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
const totalPrice = computed(() => Math.max(0, selectedSeatPrice.value + insuranceFee.value - discount.value))

const selectedPassenger = computed(() => {
  if (!bookingForm.value.passengerId) return null
  return passengerList.value.find(p => p.id === bookingForm.value.passengerId)
})

const canSubmit = computed(() => {
  return bookingForm.value.passengerId && bookingForm.value.phone && totalPrice.value > 0
})

const passengerRules = {
  passengerId: [{ required: true, message: '请选择乘车人', trigger: 'change' }]
}

const payMethods = [
  { value: 'wechat', label: '微信支付', desc: '推荐使用，即时到账', icon: 'ChatDotRound' },
  { value: 'alipay', label: '支付宝', desc: '支付宝安全支付', icon: 'Money' },
  { value: 'bank', label: '银行卡', desc: '储蓄卡/信用卡支付', icon: 'CreditCard' }
]

function trainTypeTag(type) {
  const map = { G: 'danger', C: 'danger', D: 'warning', Z: 'success', T: 'info', K: 'primary' }
  return map[type] || 'info'
}
function trainTypeLabel(type) {
  const map = { G: '高铁', C: '城际', D: '动车', Z: '直达', T: '特快', K: '快速' }
  return map[type] || type
}

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

async function confirmPay() {
  paying.value = true
  try {
    const orderData = {
      trainId: route.params.trainId,
      trainNo: trainData.value.trainNo,
      fromStation: trainData.value.fromStation,
      toStation: trainData.value.toStation,
      departTime: trainData.value.departTime,
      arriveTime: trainData.value.arriveTime,
      duration: trainData.value.duration,
      seatType: route.query.seatType || 'SECOND_CLASS',
      seatLabel: selectedSeatLabel.value,
      seatPrice: selectedSeatPrice.value,
      passengerId: bookingForm.value.passengerId,
      passengerName: selectedPassenger.value?.name,
      passengerIdCard: selectedPassenger.value?.idCard,
      phone: bookingForm.value.phone,
      remark: bookingForm.value.remark,
      totalPrice: totalPrice.value,
      insuranceFee: insuranceFee.value,
      discount: discount.value,
      status: 'PENDING'
    }
    await createOrder(orderData)
    payDialogVisible.value = false
    ElMessage.success('支付成功！订单已生成')
    router.push('/my-orders')
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}

function showAgreement() {
  ElMessageBox.alert(
    '1. 购票成功后，请按时乘车。\n2. 改签请在发车前24小时办理。\n3. 退票请遵循退票规则（发车前15天免手续费，48小时前5%，24小时前10%，24小时内20%）。\n4. 请妥善保管车票信息，乘车时携带有效证件。',
    '12306 购票服务协议',
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
  padding-bottom: 40px;
}

.page-header {
  margin-bottom: 18px;

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
  gap: 22px;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

/* ========== 通用卡片段 ========== */
.card-section {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  padding: 22px 24px;
  margin-bottom: 18px;
  box-shadow: var(--shadow-card);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-blue-deep);
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 2px solid var(--primary-blue-bg);

  .title-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;

    &.train-icon { background: linear-gradient(135deg, #2563eb, #3b82f6); }
    &.user-icon { background: linear-gradient(135deg, #0ea5e9, #38bdf8); }
    &.msg-icon { background: linear-gradient(135deg, #10b981, #34d399); }
    &.wallet-icon { background: linear-gradient(135deg, #f97316, #fb923c); }
  }
}

/* ========== 车次信息 ========== */
.train-info-display {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.train-head-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .train-no-badge {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 16px;
    background: var(--primary-orange-bg);
    border: 1px solid var(--primary-orange-light);
    color: var(--primary-orange-deep);
    border-radius: 20px;
    font-weight: 700;
    font-size: 15px;
  }
}

.route-detail {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(90deg, var(--primary-blue-bg) 0%, #f0f9ff 50%, var(--primary-blue-bg) 100%);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-blue);

  .station-block {
    text-align: center;

    .station-time {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: var(--primary-blue-deep);
      font-family: 'DIN', monospace;
    }
    .station-name {
      display: block;
      color: var(--text-secondary);
      font-size: 15px;
      font-weight: 500;
      margin-top: 4px;
    }
    .station-label {
      display: inline-block;
      margin-top: 6px;
      padding: 2px 10px;
      background: #fff;
      border: 1px solid var(--border-blue);
      color: var(--primary-blue);
      font-size: 11px;
      border-radius: 10px;
    }
  }

  .route-arrow {
    display: flex;
    align-items: center;
    gap: 6px;
    justify-content: center;
    padding: 0 10px;

    .dash {
      width: 50px;
      height: 2px;
      background: repeating-linear-gradient(90deg, var(--primary-blue-light) 0 4px, transparent 4px 8px);
    }
    .duration-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 4px 12px;
      background: #fff;
      border: 1px solid var(--border-blue);
      border-radius: 14px;

      .duration-label {
        font-size: 10px;
        color: var(--text-muted);
      }
      .duration {
        font-size: 13px;
        font-weight: 600;
        color: var(--primary-blue-deep);
      }
    }
    .arrow {
      color: var(--primary-orange);
      font-size: 16px;
    }
  }
}

.selected-seat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid var(--border-blue);
  border-radius: var(--radius-sm);

  .label {
    color: var(--text-secondary);
    font-size: 14px;
  }
  .seat-price {
    margin-left: auto;
    font-size: 22px;
    font-weight: 700;
    color: var(--primary-orange);
    font-family: 'DIN', monospace;
  }
}

/* ========== 乘客/联系方式 ========== */
.passenger-select {
  width: 100%;
}

.passenger-info {
  margin-top: 14px;
  padding: 16px;
  background: var(--primary-blue-bg);
  border: 1px solid var(--border-blue);
  border-radius: var(--radius-sm);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .info-label {
    font-size: 12px;
    color: var(--text-muted);
  }
  .info-value {
    color: var(--text-primary);
    font-weight: 500;
    font-family: 'Consolas', monospace;
  }
}

.no-passenger {
  padding: 20px 0 8px;
}

:deep(.passenger-form .el-form-item),
:deep(.contact-section .el-form-item) {
  margin-bottom: 14px;
}

/* ========== 费用汇总 ========== */
.sticky-card {
  position: sticky;
  top: 90px;
}

.price-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
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
  font-size: 16px;
  letter-spacing: 6px;
  padding-left: 6px;
  margin-bottom: 12px;
  font-weight: 600;
}

.agreement {
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  margin: 0;
  line-height: 1.6;

  a {
    color: var(--primary-blue);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
}

/* ========== 支付弹窗 ========== */
.pay-dialog-content {
  .pay-amount {
    text-align: center;
    padding: 18px 0 22px;
    border-bottom: 1px solid var(--border-light);
    margin-bottom: 18px;

    .pay-label {
      display: block;
      color: var(--text-muted);
      font-size: 13px;
      margin-bottom: 6px;
    }
    .pay-price {
      font-size: 36px;
      font-weight: 700;
      color: var(--primary-orange);
      font-family: 'DIN', monospace;
    }
  }

  .pay-title {
    font-size: 14px;
    color: var(--text-secondary);
    font-weight: 500;
    margin-bottom: 12px;
  }

  .pay-methods {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .pay-method {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border: 2px solid var(--border-light);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--primary-blue-light);
      background: var(--primary-blue-bg);
    }
    &.active {
      border-color: var(--primary-blue);
      background: var(--primary-blue-bg);
    }

    .method-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .method-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;

      &.wechat { background: linear-gradient(135deg, #07c160, #10d974); }
      &.alipay { background: linear-gradient(135deg, #1677ff, #4096ff); }
      &.bank { background: linear-gradient(135deg, #6b7280, #9ca3af); }
    }

    .method-info {
      .method-name {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary);
      }
      .method-desc {
        font-size: 12px;
        color: var(--text-muted);
        margin-top: 2px;
      }
    }

    .check-icon {
      color: var(--primary-blue);
      font-size: 22px;
    }
  }
}
</style>
