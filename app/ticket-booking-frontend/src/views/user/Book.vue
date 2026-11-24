<template>
  <div class="book-page page-container">
    <div class="page-header">
      <h2>预订车票</h2>
      <p class="header-desc">请确认车次信息并选择乘客</p>
    </div>

    <el-steps :active="payDialogVisible ? 1 : 0" simple class="booking-steps"><el-step title="选择乘客" /><el-step title="核对订单" /><el-step title="订单页模拟支付" /></el-steps>
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
            <el-button class="add-passenger-btn" type="primary" plain @click="openPassengerDialog">新增乘客</el-button>
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
                  <span class="info-label">乘客类型</span>
                  <span class="info-value">{{ ['成人', '学生', '儿童', '军人'][selectedPassenger.type] || '成人' }}</span>
                </div>
              </div>
            </div>
            <div class="no-passenger" v-else-if="!passengerList.length">
              <el-empty description="还没有乘客，添加后即可继续预订" :image-size="60">
                <el-button type="primary" @click="openPassengerDialog">新增乘客</el-button>
              </el-empty>
            </div>
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

    <el-dialog v-model="passengerDialogVisible" title="新增乘客" width="480px" :close-on-click-modal="false" :close-on-press-escape="!savingPassenger" :show-close="!savingPassenger">
      <p class="passenger-dialog-hint">保存到常用乘客，并自动选中用于本次预订。</p>
      <el-form ref="newPassengerFormRef" :model="newPassenger" :rules="newPassengerRules" label-position="top" :disabled="savingPassenger" @submit.prevent="savePassenger">
        <el-form-item label="乘客姓名" prop="name"><el-input v-model.trim="newPassenger.name" maxlength="50" placeholder="请填写与证件一致的姓名" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model.trim="newPassenger.idCard" maxlength="18" placeholder="请输入18位身份证号" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model.trim="newPassenger.phone" maxlength="11" placeholder="请输入11位手机号" /></el-form-item>
        <el-form-item label="乘客类型" prop="type"><el-select v-model="newPassenger.type" style="width: 100%"><el-option label="成人" :value="0" /><el-option label="学生" :value="1" /><el-option label="儿童" :value="2" /><el-option label="军人" :value="3" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="savingPassenger" @click="passengerDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingPassenger" @click="savePassenger">保存并选择</el-button></template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" title="确认订单" width="400px" :close-on-click-modal="false">
      <div class="pay-dialog-content">
        <div class="pay-amount">
          <span class="pay-label">订单金额</span>
          <span class="pay-price">¥{{ totalPrice }}</span>
        </div>
        <el-alert title="演示系统：创建订单后请前往订单页进行模拟支付，不会实际扣款" type="info" :closable="false" />

      </div>
      <template #footer>
        <el-button @click="cancelPay">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">确认创建订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, ArrowRight, User, Wallet, Check } from '@element-plus/icons-vue'
import { getTrainById } from '@/api/train'
import { createOrder } from '@/api/order'
import { getMyPassengers, addPassenger } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const trainData = ref({})
const passengerList = ref([])
const bookingFormRef = ref(null)
const payDialogVisible = ref(false)
const paying = ref(false)
const payMethod = ref('wechat')

const bookingForm = ref({
  passengerId: ''
})

const passengerDialogVisible = ref(false)
const savingPassenger = ref(false)
const newPassengerFormRef = ref(null)
const newPassenger = ref({ name: '', idCard: '', phone: '', type: 0 })
const newPassengerRules = {
  name: [{ required: true, whitespace: true, message: '请输入乘客姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /^\d{17}[\dXx]$/, message: '请输入18位身份证号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }]
}
async function openPassengerDialog() {
  newPassenger.value = { name: '', idCard: '', phone: '', type: 0 }
  passengerDialogVisible.value = true
  await nextTick()
  newPassengerFormRef.value?.clearValidate()
}
async function savePassenger() {
  if (savingPassenger.value) return
  const valid = await newPassengerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = { ...newPassenger.value, idCard: newPassenger.value.idCard.toUpperCase() }
  const existing = passengerList.value.find(p => p.idCard?.toUpperCase() === payload.idCard)
  if (existing) {
    bookingForm.value.passengerId = existing.id
    passengerDialogVisible.value = false
    ElMessage.info('该乘客已存在，已为您选中')
    return
  }
  savingPassenger.value = true
  try {
    const res = await addPassenger(payload)
    if (!res.data?.id) throw new Error('乘客保存结果异常')
    passengerList.value.push(res.data)
    bookingForm.value.passengerId = res.data.id
    passengerDialogVisible.value = false
    bookingFormRef.value?.clearValidate('passengerId')
    ElMessage.success('乘客已保存并选中')
  } catch {
    ElMessage.error('乘客保存失败，请重试')
  } finally {
    savingPassenger.value = false
  }
}

const selectedSeatLabel = computed(() => route.query.seatLabel || '二等座')
const selectedSeatPrice = computed(() => Number(trainData.value[['businessPrice', 'firstPrice', 'secondPrice', 'softPrice', 'hardPrice', 'seatPrice'][mapSeatType(route.query.seatType)]]) || 0)

const insuranceFee = computed(() => 0)
const discount = computed(() => 0)
const totalPrice = computed(() => selectedSeatPrice.value)

const selectedPassenger = computed(() => {
  if (!bookingForm.value.passengerId) return null
  return passengerList.value.find(p => p.id === bookingForm.value.passengerId)
})

const canSubmit = computed(() => {
  return !!selectedPassenger.value && totalPrice.value > 0
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

async function fetchPassengers() {
  try {
    const res = await getMyPassengers()
    passengerList.value = res.data || []
    if (passengerList.value.length) bookingForm.value.passengerId = passengerList.value[0].id
  } catch {
    ElMessage.error('乘客信息加载失败，请刷新重试')
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
  if (seatType !== undefined && /^[0-5]$/.test(String(seatType))) return Number(seatType)
  const map = { 'BUSINESS': 0, 'FIRST_CLASS': 1, 'SECOND_CLASS': 2, 'SOFT_SLEEPER': 3, 'HARD_SLEEPER': 4, 'HARD_SEAT': 5 }
  return map[seatType] ?? 2
}

async function confirmPay() {
  if (paying.value) return
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
    ElMessage.success('订单已创建，请在我的订单中完成模拟支付')
    router.push('/my-orders')
  } catch {
    ElMessage.error('创建订单失败，请重试')
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
  grid-template-columns: minmax(0, 1fr) 300px;
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
    background: #eff6ff;
    border: 1px solid #dbeafe;
    border-radius: var(--radius-sm);
    color: #183555;
    box-shadow: none;

    .detail-label {
      color: #475569;
    }

    :deep(.el-tag) {
      background: #fff !important;
      border: 1px solid #bfdbfe !important;
      color: #1d4ed8 !important;
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
.booking-steps { margin-bottom: 24px; border: 1px solid var(--border-light); border-radius: 12px; background: #fff; padding: 20px; }
.add-passenger-btn { margin-left: auto; }
.passenger-dialog-hint { color: var(--text-secondary); line-height: 1.6; margin-bottom: 20px; }
.book-main, .book-sidebar { min-width: 0; }
@media (max-width: 600px) { .booking-steps { padding: 14px 8px; } .booking-steps :deep(.el-step__title) { font-size: 12px; } .book-page .card { padding: 18px; } }
@media (max-width: 768px) { .book-layout { display: block !important; } .book-sidebar { width: 100% !important; margin-top: 16px; } }
</style>
