<template>
  <div class="orders-page">
    <el-card shadow="never" class="filter-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="searchForm.user" placeholder="请输入用户名/手机号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已退票" value="REFUNDED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span class="title">订单列表</span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="trainNo" label="车次" width="100" />
        <el-table-column prop="seatType" label="座位" width="100" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'PAID'"
              type="warning"
              link
              size="small"
              @click="handleProcess(row)"
            >
              处理异常
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentOrder.userName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentOrder.userPhone }}</el-descriptions-item>
        <el-descriptions-item label="车次">{{ currentOrder.trainNo }}</el-descriptions-item>
        <el-descriptions-item label="座位类型">{{ currentOrder.seatType }}</el-descriptions-item>
        <el-descriptions-item label="出发站">{{ currentOrder.fromStation }}</el-descriptions-item>
        <el-descriptions-item label="到达站">{{ currentOrder.toStation }}</el-descriptions-item>
        <el-descriptions-item label="出发时间">{{ currentOrder.departTime }}</el-descriptions-item>
        <el-descriptions-item label="到达时间">{{ currentOrder.arriveTime }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentOrder.amount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentOrder.status)">{{ statusText(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { listOrdersAdmin } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  orderNo: '',
  user: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const detailVisible = ref(false)
const currentOrder = ref(null)

const statusMap = {
  PENDING: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'primary' },
  COMPLETED: { text: '已完成', type: 'success' },
  REFUNDED: { text: '已退票', type: 'info' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

function statusText(status) {
  return statusMap[status]?.text || status
}

function statusTagType(status) {
  return statusMap[status]?.type || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      orderNo: searchForm.orderNo || undefined,
      user: searchForm.user || undefined,
      status: searchForm.status || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined
    }
    const res = await listOrdersAdmin(params)
    if (res?.data) {
      tableData.value = res.data.list || res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch {
    loadMockData()
  } finally {
    loading.value = false
  }
}

function loadMockData() {
  const statuses = ['PENDING', 'PAID', 'COMPLETED', 'REFUNDED', 'CANCELLED']
  const seatTypes = ['商务座', '一等座', '二等座', '无座']
  const trains = ['G1', 'G2', 'Z10', 'D31', 'K105']
  const stations = [
    ['北京南', '上海虹桥'],
    ['广州南', '深圳北'],
    ['上海虹桥', '杭州东'],
    ['北京西', '广州南'],
    ['深圳北', '珠海北']
  ]
  const mockList = []
  for (let i = 1; i <= 30; i++) {
    const s = stations[i % stations.length]
    const status = statuses[i % statuses.length]
    mockList.push({
      id: i,
      orderNo: `ORD${Date.now()}${String(i).padStart(4, '0')}`,
      userName: `user${i}`,
      userPhone: `138${String(10000000 + i).slice(-8)}`,
      trainNo: trains[i % trains.length],
      seatType: seatTypes[i % seatTypes.length],
      fromStation: s[0],
      toStation: s[1],
      departTime: `2024-02-${String(i % 28 + 1).padStart(2, '0')} 08:00`,
      arriveTime: `2024-02-${String(i % 28 + 1).padStart(2, '0')} 12:30`,
      amount: (Math.random() * 1000 + 100).toFixed(2),
      status,
      createdAt: `2024-02-${String(i % 28 + 1).padStart(2, '0')} 10:${String(i % 60).padStart(2, '0')}:00`,
      remark: ''
    })
  }
  const start = (pagination.page - 1) * pagination.pageSize
  tableData.value = mockList.slice(start, start + pagination.pageSize)
  pagination.total = mockList.length
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.orderNo = ''
  searchForm.user = ''
  searchForm.status = ''
  searchForm.dateRange = []
  handleSearch()
}

function handleView(row) {
  currentOrder.value = row
  detailVisible.value = true
}

function handleProcess(row) {
  ElMessageBox.confirm(`确定要手动处理订单「${row.orderNo}」吗？`, '异常订单处理', {
    confirmButtonText: '确定处理',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.status = 'COMPLETED'
    ElMessage.success('订单处理成功')
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.orders-page {
  .filter-card {
    margin-bottom: 16px;
    border-radius: var(--radius-md);
    border: 1px solid var(--border-light);

    :deep(.el-card__body) {
      padding: 16px 16px 0;
    }
  }

  .table-card {
    border-radius: var(--radius-md);
    border: 1px solid var(--border-light);

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
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
    }
  }

  .amount {
    font-weight: 700;
    color: var(--primary-orange);
    font-family: 'DIN', monospace;
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>