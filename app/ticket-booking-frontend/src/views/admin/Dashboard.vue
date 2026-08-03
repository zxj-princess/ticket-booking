<template>
  <div class="dashboard-page">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon icon-blue">
              <el-icon :size="28" color="#fff"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOrders }}</div>
              <div class="stat-label">今日订单数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon icon-orange">
              <el-icon :size="28" color="#fff"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ stats.todayAmount }}</div>
              <div class="stat-label">今日购票金额</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon icon-green">
              <el-icon :size="28" color="#fff"><RefreshLeft /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayRefunds }}</div>
              <div class="stat-label">今日退票数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon icon-purple">
              <el-icon :size="28" color="#fff"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeUsers }}</div>
              <div class="stat-label">活跃用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单趋势（近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单状态分布</span>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>热门线路 Top 5</span>
            </div>
          </template>
          <div ref="routeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>座位类型分布</span>
            </div>
          </template>
          <div ref="seatChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Tickets, Money, RefreshLeft, User } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/admin'

const trendChartRef = ref(null)
const statusChartRef = ref(null)
const routeChartRef = ref(null)
const seatChartRef = ref(null)

let trendChart = null
let statusChart = null
let routeChart = null
let seatChart = null

const stats = reactive({
  todayOrders: 0,
  todayAmount: '0.00',
  todayRefunds: 0,
  activeUsers: 0
})

const trendData = reactive({
  dates: [],
  orders: []
})

const statusData = reactive({
  labels: [],
  values: []
})

const routeData = reactive({
  names: [],
  values: []
})

const seatData = reactive({
  labels: [],
  values: []
})

async function loadData() {
  try {
    const res = await getDashboardStats()
    if (res?.data) {
      const data = res.data
      stats.todayOrders = data.todayOrders ?? 128
      stats.todayAmount = data.todayAmount ?? '25,680.00'
      stats.todayRefunds = data.todayRefunds ?? 5
      stats.activeUsers = data.activeUsers ?? 342

      if (data.trend) {
        trendData.dates = data.trend.dates || []
        trendData.orders = data.trend.orders || []
      }
      if (data.statusDistribution) {
        statusData.labels = data.statusDistribution.labels || []
        statusData.values = data.statusDistribution.values || []
      }
      if (data.topRoutes) {
        routeData.names = data.topRoutes.names || []
        routeData.values = data.topRoutes.values || []
      }
      if (data.seatDistribution) {
        seatData.labels = data.seatDistribution.labels || []
        seatData.values = data.seatDistribution.values || []
      }
    }
  } catch {
    loadMockData()
  }

  await nextTick()
  initCharts()
}

function loadMockData() {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  trendData.dates = days
  trendData.orders = [42, 58, 65, 73, 80, 95, 128]

  statusData.labels = ['已支付', '已完成', '已退票', '待支付']
  statusData.values = [156, 89, 23, 12]

  routeData.names = ['北京→上海', '广州→深圳', '上海→杭州', '北京→广州', '深圳→珠海']
  routeData.values = [1280, 960, 820, 720, 650]

  seatData.labels = ['商务座', '一等座', '二等座', '无座']
  seatData.values = [5, 42, 128, 23]
}

function initCharts() {
  initTrendChart()
  initStatusChart()
  initRouteChart()
  initSeatChart()
}

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trendData.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b' }
    },
    series: [
      {
        name: '订单数',
        type: 'line',
        data: trendData.orders,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.25)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      }
    ]
  })
}

function initStatusChart() {
  if (!statusChartRef.value) return
  statusChart = echarts.init(statusChartRef.value)
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data: statusData.labels.map((label, i) => ({
          name: label,
          value: statusData.values[i]
        }))
      }
    ],
    color: ['#2563eb', '#f97316', '#16a34a', '#f59e0b', '#8b5cf6']
  })
}

function initRouteChart() {
  if (!routeChartRef.value) return
  routeChart = echarts.init(routeChartRef.value)
  routeChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'category',
      data: routeData.names.slice().reverse(),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontWeight: 500 }
    },
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: routeData.values.slice().reverse(),
        barWidth: 20,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#2563eb' },
            { offset: 1, color: '#60a5fa' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#475569',
          fontWeight: 500
        }
      }
    ]
  })
}

function initSeatChart() {
  if (!seatChartRef.value) return
  seatChart = echarts.init(seatChartRef.value)
  seatChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        name: '座位类型',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data: seatData.labels.map((label, i) => ({
          name: label,
          value: seatData.values[i]
        }))
      }
    ],
    color: ['#f97316', '#2563eb', '#60a5fa', '#16a34a']
  })
}

function handleResize() {
  trendChart?.resize()
  statusChart?.resize()
  routeChart?.resize()
  seatChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  routeChart?.dispose()
  seatChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  .stat-cards {
    margin-bottom: 16px;
  }

  .stat-card {
    border-radius: var(--radius-md);
    border: 1px solid var(--border-light);
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.icon-blue {
      background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }
    &.icon-orange {
      background: linear-gradient(135deg, var(--primary-orange) 0%, var(--primary-orange-light) 100%);
      box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
    }
    &.icon-green {
      background: linear-gradient(135deg, #16a34a 0%, #4ade80 100%);
      box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
    }
    &.icon-purple {
      background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
      box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
    }
  }

  .stat-info {
    flex: 1;
    min-width: 0;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 700;
    color: var(--primary-blue-deep);
    line-height: 1.2;
    font-family: 'DIN', monospace;
  }

  .stat-label {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 4px;
  }

  .chart-row {
    margin-bottom: 16px;

    :deep(.el-card) {
      border-radius: var(--radius-md);
      border: 1px solid var(--border-light);

      .el-card__header {
        padding: 14px 20px;
        border-bottom: 1px solid var(--border-light);
      }
    }
  }

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: var(--primary-blue-deep);
    display: flex;
    align-items: center;
    gap: 8px;

    &::before {
      content: '';
      display: inline-block;
      width: 4px;
      height: 16px;
      background: var(--primary-blue);
      border-radius: 2px;
    }
  }

  .chart-container {
    height: 320px;
    width: 100%;
  }
}
</style>
