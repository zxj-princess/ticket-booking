<template>
  <div class="change-page page-container">
    <div class="page-header">
      <h2>改签</h2>
      <p class="header-desc">选择新的车次进行改签</p>
    </div>

    <div class="change-layout">
      <div class="change-main">
        <div class="card original-order">
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>原订单信息</span>
          </div>
          <div class="order-info" v-loading="loading">
            <div class="info-row">
              <span class="label">原车次</span>
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
            <div class="info-row">
              <span class="label">原座位</span>
              <span class="value">{{ orderData.seatLabel }}</span>
              <span class="price">¥{{ orderData.totalPrice || orderData.price }}</span>
            </div>
          </div>
        </div>

        <div class="card select-train">
          <div class="card-title">
            <el-icon><Search /></el-icon>
            <span>选择新车次</span>
          </div>
          <el-form :model="searchForm" inline class="search-form">
            <el-form-item label="出发站">
              <el-input v-model="searchForm.fromStation" placeholder="出发站" />
            </el-form-item>
            <el-form-item label="到达站">
              <el-input v-model="searchForm.toStation" placeholder="到达站" />
            </el-form-item>
            <el-form-item label="日期">
              <el-date-picker
                v-model="searchForm.date"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-button type="primary" @click="searchNewTrains">查询</el-button>
          </el-form>

          <div class="train-list" v-if="newTrains.length > 0">
            <div
              v-for="train in newTrains"
              :key="train.id || train.trainNo"
              class="train-item"
              :class="{ selected: selectedTrain?.id === train.id }"
              @click="selectTrain(train)"
            >
              <div class="train-info">
                <span class="train-no">{{ train.trainNo }}</span>
                <span class="train-time">{{ train.departTime }}-{{ train.arriveTime }}</span>
              </div>
              <div class="train-route">
                <span>{{ train.fromStation }}</span>
                <span class="arrow">→</span>
                <span>{{ train.toStation }}</span>
              </div>
              <div class="train-price-list">
                <template v-for="opt in seatOptions" :key="opt.value">
                  <span v-if="train[opt.seatsField] > 0" class="price-tag">
                    {{ opt.label }} ¥{{ train[opt.priceField] }}
                  </span>
                </template>
              </div>
            </div>
          </div>
          <div class="no-train" v-else-if="searched">
            <el-empty description="未找到可改签的车次" :image-size="80" />
          </div>
          <div class="search-tip" v-else>
            <el-empty description="请查询可改签的新车次" :image-size="60" />
          </div>

          <div class="seat-selector" v-if="selectedTrain">
            <div class="seat-title">选择座位类型</div>
            <el-radio-group v-model="selectedSeatType">
              <el-radio
                v-for="opt in seatOptions"
                :key="opt.value"
                :label="opt.value"
                :disabled="!selectedTrain[opt.seatsField] || selectedTrain[opt.seatsField] <= 0"
                class="seat-radio"
              >
                {{ opt.label }} ¥{{ selectedTrain[opt.priceField] }}
              </el-radio>
            </el-radio-group>
          </div>
        </div>
      </div>

      <div class="change-sidebar">
        <div class="card change-summary">
          <div class="card-title">
            <el-icon><Money /></el-icon>
            <span>改签费用</span>
          </div>
          <div class="fee-calc">
            <div class="fee-row">
              <span>原票价</span>
              <span class="fee-value">¥{{ originalPrice }}</span>
            </div>
            <div class="fee-row">
              <span>新票价</span>
              <span class="fee-value">¥{{ newPrice }}</span>
            </div>
            <div class="fee-row" :class="{ positive: priceDiff >= 0, negative: priceDiff < 0 }">
              <span>差价</span>
              <span class="fee-value">
                {{ priceDiff >= 0 ? '+' : '' }}¥{{ priceDiff }}
              </span>
            </div>
            <div class="fee-row">
              <span>改签手续费</span>
              <span class="fee-value">¥{{ changeFee }}</span>
            </div>
            <div class="fee-total" v-if="finalAmount > 0">
              <span>需支付</span>
              <span class="total">¥{{ finalAmount }}</span>
            </div>
            <div class="fee-total" v-else-if="finalAmount < 0">
              <span>退还钱包</span>
              <span class="total">¥{{ Math.abs(finalAmount) }}</span>
            </div>
            <div class="fee-total" v-else>
              <span>无需补退</span>
              <span class="total">¥0</span>
            </div>
          </div>
          <el-button
            type="primary"
            size="large"
            class="confirm-btn"
            :disabled="!canChange"
            :loading="submitting"
            @click="confirmChange"
          >
            确认改签
          </el-button>
        </div>

        <div class="card notice-card">
          <div class="card-title">
            <el-icon><Warning /></el-icon>
            <span>改签须知</span>
          </div>
          <ul class="notice-list">
            <li>发车前48小时以上可办理改签</li>
            <li>改签直接修改原订单，不创建新订单</li>
            <li>每张车票仅可改签一次，改签后无法再次改签</li>
            <li>改签手续费：距发车48h以上免费，24-48h收5%，不足24h收10%</li>
            <li>改签后票价高于原价：需补差价+手续费</li>
            <li>改签后票价低于原价：差额扣除手续费后退回钱包</li>
            <li>不支持改变出发站和到达站</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, Search, Money, Warning, ArrowRight } from '@element-plus/icons-vue'
import { getOrder, changeOrder } from '@/api/order'
import { searchTrains } from '@/api/train'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const orderData = ref({})
const newTrains = ref([])
const searched = ref(false)
const selectedTrain = ref(null)
const selectedSeatType = ref(2)

const seatOptions = [
  { value: 0, label: '商务座', priceField: 'businessPrice', seatsField: 'businessSeats' },
  { value: 1, label: '一等座', priceField: 'firstPrice', seatsField: 'firstSeats' },
  { value: 2, label: '二等座', priceField: 'secondPrice', seatsField: 'secondSeats' },
  { value: 3, label: '软卧', priceField: 'softPrice', seatsField: 'softSleepers' },
  { value: 4, label: '硬卧', priceField: 'hardPrice', seatsField: 'hardSleepers' },
  { value: 5, label: '硬座', priceField: 'seatPrice', seatsField: 'hardSeats' }
]

const searchForm = ref({
  fromStation: '',
  toStation: '',
  date: ''
})

const originalPrice = computed(() => Number(orderData.value.totalPrice || orderData.value.price) || 0)
const newPrice = computed(() => {
  if (!selectedTrain.value) return 0
  const opt = seatOptions.find(o => o.value === selectedSeatType.value)
  if (!opt) return 0
  return Number(selectedTrain.value[opt.priceField] || 0)
})
const priceDiff = computed(() => newPrice.value - originalPrice.value)
const changeFee = computed(() => {
  if (!selectedTrain.value || !orderData.value.departTime) return 0
  const depart = new Date(orderData.value.departTime).getTime()
  const now = Date.now()
  const hours = (depart - now) / 3600000
  if (hours >= 48) return 0
  if (hours >= 24) return Math.round(originalPrice.value * 0.05 * 100) / 100
  return Math.round(originalPrice.value * 0.10 * 100) / 100
})
const finalAmount = computed(() => priceDiff.value + changeFee.value)
const canChange = computed(() => selectedTrain.value !== null)

async function fetchOrderDetail() {
  const orderId = route.params.orderId
  loading.value = true
  try {
    const res = await getOrder(orderId)
    orderData.value = res.data || {}
    searchForm.value.fromStation = orderData.value.fromStation || ''
    searchForm.value.toStation = orderData.value.toStation || ''
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    searchForm.value.date = tomorrow.toISOString().slice(0, 10)
  } catch {
    ElMessage.error('获取订单信息失败')
  } finally {
    loading.value = false
  }
}

async function searchNewTrains() {
  if (!searchForm.value.fromStation || !searchForm.value.toStation) {
    ElMessage.warning('请输入出发站和到达站')
    return
  }
  searched.value = true
  try {
    const res = await searchTrains(searchForm.value)
    newTrains.value = res.data || []
  } catch {
    newTrains.value = []
  }
}

function selectTrain(train) {
  selectedTrain.value = train
  selectedSeatType.value = 2
}

async function confirmChange() {
  if (!canChange.value) return
  submitting.value = true
  try {
    const orderNo = orderData.value.orderNumber || orderData.value.orderNo || route.params.orderId
    const opt = seatOptions.find(o => o.value === selectedSeatType.value)
    let confirmMsg = `确认改签 ${orderData.value.trainNo} → ${selectedTrain.value.trainNo}（${opt?.label || '二等座'}）？`
    if (finalAmount.value > 0) {
      confirmMsg = `确认改签 ${orderData.value.trainNo} → ${selectedTrain.value.trainNo}（${opt?.label || '二等座'}），需支付 ¥${finalAmount.value}（差价+手续费）？\n温馨提示：车票仅支持一次改签，确认后无法再次更改。`
    } else if (finalAmount.value < 0) {
      confirmMsg = `确认改签 ${orderData.value.trainNo} → ${selectedTrain.value.trainNo}（${opt?.label || '二等座'}），差额 ¥${Math.abs(finalAmount.value)} 将退回您的钱包？\n温馨提示：车票仅支持一次改签，确认后无法再次更改。`
    } else {
      confirmMsg = `确认改签 ${orderData.value.trainNo} → ${selectedTrain.value.trainNo}（${opt?.label || '二等座'}）？\n温馨提示：车票仅支持一次改签，确认后无法再次更改。`
    }
    await ElMessageBox.confirm(confirmMsg, '确认改签', { type: 'warning' })
    await changeOrder(orderNo, {
      trainId: selectedTrain.value.id,
      seatType: selectedSeatType.value
    })
    if (finalAmount.value > 0) {
      ElMessage.success('改签成功！请前往「我的订单」完成支付')
    } else if (finalAmount.value < 0) {
      ElMessage.success(`改签成功！差额 ¥${Math.abs(finalAmount.value)} 已退回您的钱包`)
    } else {
      ElMessage.success('改签成功！')
    }
    router.push('/my-orders')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.msg || '改签失败，请重试')
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
.change-page {
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

.change-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.original-order {
  .order-info {
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
      min-width: 60px;
      font-size: 14px;
    }

    .value {
      color: var(--text-primary);
      font-weight: 500;
    }

    .train-no {
      font-size: 18px;
      font-weight: 700;
      color: var(--primary-orange-deep);
    }

    .price {
      margin-left: auto;
      color: var(--primary-orange);
      font-weight: 700;
      font-size: 18px;
      font-family: 'DIN', monospace;
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
}

.select-train {
  .search-form {
    margin-bottom: 16px;

    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .train-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-height: 400px;
    overflow-y: auto;
    padding: 4px;
  }

  .train-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 18px;
    border: 2px solid var(--border-light);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.2s ease;
    background: #fff;

    &:hover {
      border-color: var(--primary-blue-light);
      background: var(--primary-blue-bg);
      transform: translateX(2px);
    }

    &.selected {
      border-color: var(--primary-blue);
      background: var(--primary-blue-lighter);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
    }

    .train-info {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .train-no {
        font-weight: 700;
        color: var(--primary-orange-deep);
        font-size: 16px;
      }

      .train-time {
        font-size: 13px;
        color: var(--text-muted);
        font-family: 'DIN', monospace;
      }
    }

    .train-route {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-secondary);
      font-size: 14px;

      .arrow {
        color: var(--primary-orange);
        font-weight: 600;
      }
    }

    .train-price-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .price-tag {
        font-size: 12px;
        color: var(--primary-orange-deep);
        background: #fff7ed;
        border: 1px solid #fed7aa;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 500;
      }
    }

    .seat-selector {
      margin-top: 20px;
      padding: 16px;
      background: #f8fafc;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-light);

      .seat-title {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 12px;
      }

      .seat-radio {
        margin-right: 16px;
        margin-bottom: 8px;
      }
    }

    .train-price {
      text-align: right;

      .price-label {
        color: var(--text-muted);
        font-size: 12px;
        margin-right: 4px;
      }

      .price-value {
        color: var(--primary-orange);
        font-weight: 700;
        font-size: 20px;
        font-family: 'DIN', monospace;
      }
    }
  }

  .no-train,
  .search-tip {
    padding: 30px 0;
    text-align: center;
  }
}

.change-sidebar {
  .change-summary {
    position: sticky;
    top: 90px;

    .fee-calc {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 24px;
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
      }

      &.positive .fee-value {
        color: var(--brand-red);
        font-weight: 600;
      }

      &.negative .fee-value {
        color: #16a34a;
        font-weight: 600;
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
        color: var(--primary-orange);
        font-size: 28px;
        font-weight: 700;
        font-family: 'DIN', monospace;
      }
    }

    .confirm-btn {
      width: 100%;
      height: 46px;
      font-size: 16px;
      letter-spacing: 4px;
      font-weight: 600;
    }
  }

  .notice-card {
    .notice-list {
      padding-left: 20px;
      color: var(--text-secondary);
      font-size: 13px;
      line-height: 2;

      li {
        margin-bottom: 2px;
        list-style: disc;
        color: var(--text-secondary);

        &::marker {
          color: var(--primary-orange);
        }
      }
    }
  }
}
</style>
