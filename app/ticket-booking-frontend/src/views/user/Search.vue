<template>
  <div class="search-page page-container">
    <!-- 顶部搜索条 -->
    <div class="search-header-card">
      <div class="header-title">
        <el-icon :size="22" color="#2563eb"><Search /></el-icon>
        <span>车次查询</span>
      </div>
      <el-form :model="searchForm" inline class="search-form-inline" @submit.prevent="handleSearch">
        <div class="form-row">
          <el-form-item label="出发站">
            <el-select
              v-model="searchForm.fromStation"
              filterable
              placeholder="请输入或选择"
              class="station-input"
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
                  <span class="station-pinyin" v-if="station.pinyin">{{ station.pinyin }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-button class="swap-btn" circle :icon="Sort" @click="swapStations" />
          <el-form-item label="到达站">
            <el-select
              v-model="searchForm.toStation"
              filterable
              placeholder="请输入或选择"
              class="station-input"
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
                  <span class="station-pinyin" v-if="station.pinyin">{{ station.pinyin }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="出发日期">
            <el-date-picker
              v-model="searchForm.date"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              class="date-input"
            />
          </el-form-item>
          <el-button type="primary" size="default" @click="handleSearch" class="query-btn">
            <el-icon><Search /></el-icon>
            查询车票
          </el-button>
        </div>
      </el-form>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar" v-if="results.length > 0">
      <span class="filter-label">车型：</span>
      <div class="filter-tabs">
        <div
          v-for="type in trainTypes"
          :key="type.value"
          class="filter-tab"
          :class="{ active: activeFilter === type.value }"
          @click="activeFilter = type.value"
        >
          {{ type.label }}
        </div>
      </div>
      <el-select v-model="departurePeriod" placeholder="发车时段" style="width:140px"><el-option label="全天" value="" /><el-option label="上午 00–12" value="am" /><el-option label="下午 12–18" value="pm" /><el-option label="晚上 18–24" value="night" /></el-select>
      <el-select v-model="seatFilter" placeholder="席别" style="width:140px"><el-option label="全部席别" :value="-1" /><el-option v-for="(label,i) in ['商务座','一等座','二等座','软卧','硬卧','硬座']" :key="i" :label="label" :value="i" /></el-select>
      <el-checkbox v-model="onlyAvailable">仅看有票</el-checkbox>
      <el-select v-model="sortBy" aria-label="车次排序" style="width: 150px">
        <el-option label="默认排序" value="default" />
        <el-option label="价格从低到高" value="price" /><el-option label="耗时最短" value="duration" /><el-option label="最早出发" value="departure" />
      </el-select>
      <span class="result-count">共 <strong>{{ filteredResults.length }}</strong> 趟车次</span>
    </div>

    <!-- 车次列表 -->
    <div class="search-results" v-if="filteredResults.length > 0" v-loading="loading">
      <!-- 表头 -->
      <div class="result-table-header">
        <div class="col-train">车次信息</div>
        <div class="col-time">出发 - 到达</div>
        <div class="col-seat">座位信息（票价 / 余票）</div>
        <div class="col-action">操作</div>
      </div>
      <!-- 车次行 -->
      <div class="train-list">
        <div v-for="train in filteredResults" :key="train.id || train.trainNo" class="train-row">
          <div class="col-train">
            <div class="train-no">{{ train.trainNo }}</div>
            <el-tag size="small" :type="trainTypeTag(train.trainTypeStr)">{{ trainTypeLabel(train.trainTypeStr) }}</el-tag>
          </div>
          <div class="col-time">
            <div class="station-box from">
              <span class="time">{{ train.departTimeStr || train.departTime }}</span>
              <span class="station-name">{{ train.fromStation }}</span>
            </div>
            <div class="duration-box">
              <div class="dash-line"></div>
              <span class="duration-text">{{ train.durationText }}</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
              <div class="dash-line"></div>
            </div>
            <div class="station-box to">
              <span class="time">{{ train.arriveTimeStr || train.arriveTime }}</span>
              <span class="station-name">{{ train.toStation }}</span>
            </div>
          </div>
          <div class="col-seat">
            <div class="seat-grid">
              <div
                v-for="seat in getSeatTypes(train)"
                :key="seat.type"
                class="seat-cell"
                :class="{ available: seat.available > 0, soldout: seat.available === 0, selected: isSelectedSeat(train, seat) }"
                @click="seat.available > 0 && selectSeat(train, seat)"
              >
                <div class="seat-name">{{ seat.label }}</div>
                <div class="seat-price">¥{{ seat.price }}</div>
                <div class="seat-count">
                  <span :class="{ has: seat.available > 0, none: seat.available === 0 }">
                    {{ seat.available > 0 ? seat.available + '张' : '无票' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="col-action">
            <div class="price-area" v-if="selectedSeat && selectedSeat.trainId === (train.id || train.trainNo)">
              <div class="label">已选 {{ selectedSeat.label }}</div>
              <div class="price-num">¥{{ selectedSeat.price }}</div>
            </div>
            <div class="price-area" v-else>
              <div class="label">最低票价</div>
              <div class="price-num low">¥{{ getLowestPrice(train) }}</div>
            </div>
            <el-button
              type="primary"
              size="default"
              class="book-btn"
              :disabled="!canBook(train)"
              @click="goBook(train)"
            >
              {{ canBook(train) ? '立即预订' : '选择座位' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-box" v-if="!loading && results.length === 0 && searched">
      <el-empty description="未找到符合条件的车次，请尝试其他日期或站点" />
    </div>
    <div class="empty-box" v-if="!searched">
      <el-empty description="请选择出发站、到达站和日期进行查询" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Sort, ArrowRight } from '@element-plus/icons-vue'
import { searchTrains } from '@/api/train'
import { listStations } from '@/api/station'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const trainTypes = [
  { label: '全部', value: '' },
  { label: '高铁/G', value: 'G' },
  { label: '城际/C', value: 'C' },
  { label: '动车/D', value: 'D' },
  { label: '直达/Z', value: 'Z' },
  { label: '特快/T', value: 'T' },
  { label: '快速/K', value: 'K' }
]

const searchForm = ref({
  fromStation: '',
  toStation: '',
  date: ''
})

const stationOptions = ref([])
const results = ref([])
const loading = ref(false)
const searched = ref(false)
const activeFilter = ref('')
const onlyAvailable = ref(false)
const departurePeriod = ref('')
const seatFilter = ref(-1)
const sortBy = ref('default')
const selectedSeat = ref(null)

const filteredResults = computed(() => {
  const items = results.value.filter(t => {
    const hour = Number((t.departTime || '00:00').slice(0, 2))
    const period = !departurePeriod.value || (departurePeriod.value === 'am' ? hour < 12 : departurePeriod.value === 'pm' ? hour >= 12 && hour < 18 : hour >= 18)
    const seats = getSeatTypes(t).filter(s => seatFilter.value === -1 || Number(s.type) === seatFilter.value)
    return (!activeFilter.value || t.trainTypeStr === activeFilter.value) && period && seats.length && (!onlyAvailable.value || seats.some(s => s.available > 0))
  })
  if (sortBy.value === 'price') return items.sort((a,b) => getLowestPrice(a)-getLowestPrice(b))
  if (sortBy.value === 'duration') return items.sort((a,b) => Number(a.duration)-Number(b.duration))
  if (sortBy.value === 'departure') return items.sort((a,b) => String(a.departTime).localeCompare(String(b.departTime)))
  return items
})

function trainTypeTag(type) {
  const map = { G: 'danger', C: 'danger', D: 'warning', Z: 'success', T: 'info', K: 'primary' }
  return map[type] || 'info'
}
function trainTypeLabel(type) {
  const map = { G: '高铁', C: '城际', D: '动车', Z: '直达', T: '特快', K: '快速' }
  return map[type] || type
}

function getSeatTypes(train) {
  if (train.seats) return train.seats
  if (train.seatTypes) return train.seatTypes
  const typeStr = train.trainTypeStr
  if (typeStr === 'G' || typeStr === 'D' || typeStr === 'C') {
    const seats = []
    if (train.businessSeats > 0 || train.businessPrice > 0) {
      seats.push({ type: 0, label: '商务座', price: Number(train.businessPrice) || 0, available: train.businessSeats ?? 0 })
    }
    if (train.firstSeats > 0 || train.firstPrice > 0) {
      seats.push({ type: 1, label: '一等座', price: Number(train.firstPrice) || 0, available: train.firstSeats ?? 0 })
    }
    seats.push({ type: 2, label: '二等座', price: Number(train.secondPrice) || 0, available: train.secondSeats ?? 0 })
    return seats
  }
  const seats = []
  if (train.hardSeats > 0 || train.seatPrice > 0) {
    seats.push({ type: 5, label: '硬座', price: Number(train.seatPrice) || 0, available: train.hardSeats ?? 0 })
  }
  if (train.hardSleepers > 0 || train.hardPrice > 0) {
    seats.push({ type: 4, label: '硬卧', price: Number(train.hardPrice) || 0, available: train.hardSleepers ?? 0 })
  }
  if (train.softSleepers > 0 || train.softPrice > 0) {
    seats.push({ type: 3, label: '软卧', price: Number(train.softPrice) || 0, available: train.softSleepers ?? 0 })
  }
  if (seats.length === 0) {
    seats.push({ type: 2, label: '二等座', price: Number(train.secondPrice) || 0, available: train.secondSeats ?? 0 })
  }
  return seats
}

function getLowestPrice(train) {
  const seats = getSeatTypes(train)
  const available = seats.filter(s => s.available > 0)
  const prices = (available.length ? available : seats).map(s => Number(s.price)).filter(p => Number.isFinite(p) && p > 0)
  return prices.length ? Math.min(...prices) : 0
}

function isSelectedSeat(train, seat) {
  if (!selectedSeat.value) return false
  return selectedSeat.value.trainId === (train.id || train.trainNo) && selectedSeat.value.type === seat.type
}

function canBook(train) {
  if (!selectedSeat.value) return false
  return selectedSeat.value.trainId === (train.id || train.trainNo) && selectedSeat.value.available > 0
}

function selectSeat(train, seat) {
  selectedSeat.value = {
    ...seat,
    trainId: train.id || train.trainNo,
    trainData: train
  }
}

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

async function handleSearch() {
  if (!searchForm.value.fromStation || !searchForm.value.toStation) {
    ElMessage.warning('请选择出发站和到达站')
    return
  }
  if (!searchForm.value.date) {
    ElMessage.warning('请选择出发日期')
    return
  }
  loading.value = true
  searched.value = true
  selectedSeat.value = null
  activeFilter.value = ''
  try {
    const res = await searchTrains(searchForm.value)
    results.value = res.data || []
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function goBook(train) {
  if (!canBook(train)) {
    ElMessage.warning('请先选择座位类型')
    return
  }
  const trainId = train.id || train.trainNo
  router.push({
    path: `/order/${trainId}`,
    query: {
      seatType: selectedSeat.value.type,
      seatLabel: selectedSeat.value.label,
      seatPrice: selectedSeat.value.price,
      date: searchForm.value.date,
      trainData: JSON.stringify(train)
    }
  })
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
      stationOptions.value = (Array.isArray(data) ? data : []).map(name => ({ value: name, label: name, pinyin: '', id: name, name }))
    }
  } catch {
    stationOptions.value = [
      { value: '北京', label: '北京', pinyin: 'beijing', id: 1 },
      { value: '北京南', label: '北京南', pinyin: 'beijingnan', id: 2 },
      { value: '北京西', label: '北京西', pinyin: 'beijingxi', id: 3 },
      { value: '上海', label: '上海', pinyin: 'shanghai', id: 4 },
      { value: '上海虹桥', label: '上海虹桥', pinyin: 'shanghaihongqiao', id: 5 },
      { value: '广州', label: '广州', pinyin: 'guangzhou', id: 6 },
      { value: '广州南', label: '广州南', pinyin: 'guangzhounan', id: 7 },
      { value: '深圳', label: '深圳', pinyin: 'shenzhen', id: 8 },
      { value: '杭州', label: '杭州', pinyin: 'hangzhou', id: 9 },
      { value: '杭州东', label: '杭州东', pinyin: 'hangzhoudong', id: 10 },
      { value: '成都东', label: '成都东', pinyin: 'chengdudong', id: 11 },
      { value: '重庆北', label: '重庆北', pinyin: 'chongqingbei', id: 12 },
      { value: '西安北', label: '西安北', pinyin: 'xianbei', id: 13 },
      { value: '郑州东', label: '郑州东', pinyin: 'zhengzhoudong', id: 14 },
      { value: '武汉', label: '武汉', pinyin: 'wuhan', id: 15 },
      { value: '南京', label: '南京', pinyin: 'nanjing', id: 16 },
      { value: '南京南', label: '南京南', pinyin: 'nanjingnan', id: 17 },
      { value: '长沙南', label: '长沙南', pinyin: 'changshanana', id: 18 },
      { value: '南昌西', label: '南昌西', pinyin: 'nanchangxi', id: 19 },
      { value: '济南', label: '济南', pinyin: 'jinan', id: 20 },
      { value: '济南西', label: '济南西', pinyin: 'jinanxi', id: 21 },
      { value: '合肥南', label: '合肥南', pinyin: 'hefeinan', id: 22 },
      { value: '福州南', label: '福州南', pinyin: 'fuzhounan', id: 23 },
      { value: '厦门北', label: '厦门北', pinyin: 'xiamenbei', id: 24 },
      { value: '昆明南', label: '昆明南', pinyin: 'kunmingnan', id: 25 },
      { value: '贵阳北', label: '贵阳北', pinyin: 'guiyangbei', id: 26 },
      { value: '重庆西', label: '重庆西', pinyin: 'chongqingxi', id: 27 },
      { value: '郑州', label: '郑州', pinyin: 'zhengzhou', id: 28 },
      { value: '成都', label: '成都', pinyin: 'chengdu', id: 29 },
      { value: '南京', label: '南京', pinyin: 'nanjing', id: 30 }
    ]
  }
}

watch(() => route.query, (val) => {
  if (val.from && val.to && val.date) {
    searchForm.value.fromStation = val.from
    searchForm.value.toStation = val.to
    searchForm.value.date = val.date
    handleSearch()
  }
}, { immediate: true })

onMounted(() => {
  fetchStations()
  if (!route.query.date) {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    searchForm.value.date = tomorrow.toISOString().slice(0, 10)
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  padding-bottom: 40px;
}

/* ========== 顶部搜索卡片 ========== */
.search-header-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 20px 28px;
  box-shadow: var(--shadow-card);
  margin-bottom: 18px;
  border: 1px solid var(--border-light);

  .header-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: 700;
    color: var(--primary-blue-deep);
    margin-bottom: 18px;
    padding-bottom: 14px;
    border-bottom: 2px solid var(--primary-blue-bg);
  }

  .search-form-inline {
    :deep(.el-form-item) { margin-bottom: 0; }
  }

  .form-row {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
  }

  .station-input { width: 150px; }
  .date-input { width: 180px; }

  .swap-btn {
    flex-shrink: 0;
    background: var(--primary-blue);
    border: none;
    color: #fff;
    transition: all 0.25s ease;

    &:hover {
      background: var(--primary-blue-deep);
      transform: rotate(180deg);
    }
  }

  .query-btn {
    padding: 0 28px;
    height: 40px;
    font-weight: 600;
    letter-spacing: 2px;
  }
}

/* ========== 筛选栏 ========== */
.filter-bar {
  background: var(--primary-blue-bg);
  border: 1px solid var(--border-blue);
  border-radius: var(--radius-md);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  flex-wrap: wrap;

  .filter-label {
    font-weight: 600;
    color: var(--primary-blue-deep);
    font-size: 14px;
  }

  .filter-tabs {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    flex: 1;
  }

  .filter-tab {
    padding: 5px 14px;
    border-radius: var(--radius-xs);
    background: #fff;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 13px;
    border: 1px solid var(--border-blue);

    &:hover {
      color: var(--primary-blue);
      border-color: var(--primary-blue);
    }
    &.active {
      background: var(--primary-blue);
      color: #fff;
      border-color: var(--primary-blue);
      font-weight: 500;
    }
  }

  .result-count {
    color: var(--text-secondary);
    font-size: 13px;

    strong {
      color: var(--primary-orange);
      font-size: 16px;
      font-weight: 700;
    }
  }
}

/* ========== 结果列表 ========== */
.search-results {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

/* 表头 */
.result-table-header {
  display: grid;
  grid-template-columns: 140px 1fr 1.5fr 180px;
  align-items: center;
  padding: 12px 20px;
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  border-bottom: 1px solid var(--border-blue);
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-blue-deep);
}

/* 车次行 */
.train-list {
  display: flex;
  flex-direction: column;
}

.train-row {
  display: grid;
  grid-template-columns: 140px 1fr 1.5fr 180px;
  align-items: stretch;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.2s;

  &:last-child { border-bottom: none; }
  &:hover { background: var(--primary-blue-bg); }

  .col-train {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;

    .train-no {
      font-size: 20px;
      font-weight: 700;
      color: var(--primary-blue-deep);
      font-family: 'DIN', monospace;
    }
  }

  .col-time {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    gap: 10px;

    .station-box {
      text-align: center;
      .time {
        display: block;
        font-size: 22px;
        font-weight: 700;
        color: var(--text-primary);
        font-family: 'DIN', monospace;
      }
      .station-name {
        display: block;
        color: var(--text-tertiary);
        font-size: 13px;
        margin-top: 2px;
      }
      &.from .time { color: var(--primary-blue-deep); }
    }

    .duration-box {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;

      .dash-line {
        flex: 1;
        max-width: 30px;
        height: 1px;
        background: var(--primary-blue-light);
      }
      .duration-text {
        color: var(--text-muted);
        font-size: 12px;
        white-space: nowrap;
      }
      .arrow {
        color: var(--primary-blue);
        font-size: 12px;
      }
    }
  }

  .col-seat {
    padding: 0 16px;
    border-left: 1px dashed var(--border-light);
    border-right: 1px dashed var(--border-light);

    .seat-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
      gap: 8px;
    }

    .seat-cell {
      padding: 8px 10px;
      border: 1px solid var(--border-light);
      border-radius: var(--radius-xs);
      text-align: center;
      cursor: pointer;
      transition: all 0.2s;
      background: #fff;

      .seat-name { font-size: 13px; color: var(--text-secondary); margin-bottom: 2px; }
      .seat-price { font-size: 15px; font-weight: 700; color: var(--primary-orange); font-family: 'DIN', monospace; }
      .seat-count { font-size: 11px; margin-top: 2px;
        .has { color: #16a34a; }
        .none { color: var(--text-muted); }
      }

      &.available:hover {
        border-color: var(--primary-orange);
        background: var(--primary-orange-bg);
        transform: translateY(-1px);
      }
      &.soldout {
        background: #f8fafc;
        cursor: not-allowed;
        opacity: 0.6;
      }
      &.selected {
        border-color: var(--primary-orange);
        background: var(--primary-orange-bg);
        box-shadow: 0 0 0 2px rgba(249, 115, 22, 0.18);
      }
    }
  }

  .col-action {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 10px;
    padding-left: 16px;

    .price-area {
      text-align: center;
      .label { font-size: 12px; color: var(--text-muted); }
      .price-num {
        font-size: 22px;
        font-weight: 700;
        color: var(--primary-orange);
        font-family: 'DIN', monospace;
        &.low { color: var(--primary-orange); }
      }
    }

    .book-btn {
      width: 120px;
      height: 38px;
      font-weight: 600;
      letter-spacing: 2px;

      &:disabled {
        background: #cbd5e1 !important;
      }
    }
  }
}

.empty-box {
  padding: 60px 0;
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.station-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .station-name {
    font-weight: 500;
    color: var(--text-primary);
  }

  .station-pinyin {
    color: var(--text-muted);
    font-size: 12px;
    margin-left: 12px;
  }
}
.filter-bar { flex-wrap: wrap; gap: 12px; }
@media(max-width:768px) { .search-results { overflow-x:auto; } .train-row { min-width:650px; } .search-form { flex-wrap:wrap; } }
</style>
