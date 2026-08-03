<template>
  <div class="chat-records-page">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon icon-blue">
              <el-icon :size="24" color="#fff"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalSessions }}</div>
              <div class="stat-label">总会话数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon icon-green">
              <el-icon :size="24" color="#fff"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon icon-orange">
              <el-icon :size="24" color="#fff"><ChatLineRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalMessages }}</div>
              <div class="stat-label">消息总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon icon-purple">
              <el-icon :size="24" color="#fff"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value-top">{{ stats.topQuestion }}</div>
              <div class="stat-label">高频问题Top1</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户">
          <el-input v-model="searchForm.user" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="searchForm.sessionId" placeholder="请输入会话ID" clearable />
        </el-form-item>
        <el-form-item label="时间范围">
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
          <span class="title">对话记录</span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="sessionId" label="会话ID" width="180" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="userMessage" label="消息内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="msg-text">{{ row.userMessage }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="aiReply" label="AI回复" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="reply-text">{{ row.aiReply }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看详情</el-button>
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

    <el-dialog v-model="detailVisible" title="对话详情" width="600px">
      <div v-if="currentRecord" class="chat-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="会话ID" :span="2">{{ currentRecord.sessionId }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentRecord.userName }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ currentRecord.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="chat-messages">
          <div class="message user-message">
            <div class="message-label">用户提问</div>
            <div class="message-content">{{ currentRecord.userMessage }}</div>
          </div>
          <div class="message ai-message">
            <div class="message-label">AI回复</div>
            <div class="message-content">{{ currentRecord.aiReply }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, User, ChatLineRound, TrendCharts, Search, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  user: '',
  sessionId: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = reactive({
  totalSessions: 0,
  totalUsers: 0,
  totalMessages: 0,
  topQuestion: ''
})

const detailVisible = ref(false)
const currentRecord = ref(null)

const highFrequencyQuestions = [
  '北京到上海的高铁时刻表',
  '如何购买车票',
  '可以退票吗',
  '商务座和一等座的区别',
  '如何改签车票',
  '儿童票怎么买',
  '高铁有餐车吗',
  '如何选择座位'
]

function generateMockReply(question) {
  const replies = [
    '您好！北京到上海的高铁每天有多个班次，其中G1次列车早上7:00从北京南出发，11:28到达上海虹桥，全程运行4小时28分钟。',
    '购票流程：1. 登录账号；2. 选择出发地和目的地；3. 选择车次和座位类型；4. 填写乘客信息；5. 完成支付。',
    '可以退票。发车前8天以上退票免手续费；发车前48小时以上收取5%手续费；发车前24小时内收取20%手续费。',
    '商务座是最高等级座位，配备平躺座椅、餐饮服务等；一等座座椅更宽敞，配有充电插座和小桌板；二等座为标准座椅。',
    '改签流程：进入"我的订单"→选择需要改签的订单→点击"改签"按钮→选择新车次→确认并支付差价。',
    '儿童票按成人票50%发售，适用于2-12岁儿童。购票时需提供儿童的身份证件信息。',
    '大部分高铁列车配有餐车，提供中餐、快餐和饮品服务。部分列车还支持扫码点餐服务。',
    '选座时建议选择靠前或靠后的位置，中间位置振动较大。靠窗座位视野好，过道座位方便进出。'
  ]
  const idx = Math.floor(Math.random() * replies.length)
  return replies[idx]
}

async function loadData() {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 300))
    loadMockData()
  } catch {
    loadMockData()
  } finally {
    loading.value = false
  }
}

function loadMockData() {
  const mockList = []
  for (let i = 1; i <= 30; i++) {
    const question = highFrequencyQuestions[i % highFrequencyQuestions.length]
    mockList.push({
      id: i,
      sessionId: `SESSION${Date.now()}${String(i).padStart(4, '0')}`,
      userName: `user${(i % 15) + 1}`,
      userMessage: question,
      aiReply: generateMockReply(question),
      createdAt: `2024-02-${String(i % 28 + 1).padStart(2, '0')} ${String(8 + (i % 12)).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}:00`
    })
  }
  const start = (pagination.page - 1) * pagination.pageSize
  tableData.value = mockList.slice(start, start + pagination.pageSize)
  pagination.total = mockList.length

  stats.totalSessions = 156
  stats.totalUsers = 89
  stats.totalMessages = 423
  stats.topQuestion = highFrequencyQuestions[0]
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.user = ''
  searchForm.sessionId = ''
  searchForm.dateRange = []
  handleSearch()
}

function handleView(row) {
  currentRecord.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.chat-records-page {
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

  .stat-item {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.icon-blue {
      background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.28);
    }
    &.icon-orange {
      background: linear-gradient(135deg, var(--primary-orange) 0%, var(--primary-orange-light) 100%);
      box-shadow: 0 3px 10px rgba(249, 115, 22, 0.28);
    }
    &.icon-green {
      background: linear-gradient(135deg, #16a34a 0%, #4ade80 100%);
      box-shadow: 0 3px 10px rgba(22, 163, 74, 0.28);
    }
    &.icon-purple {
      background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
      box-shadow: 0 3px 10px rgba(139, 92, 246, 0.28);
    }
  }

  .stat-info {
    flex: 1;
    min-width: 0;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--primary-blue-deep);
    line-height: 1.2;
    font-family: 'DIN', monospace;
  }

  .stat-value-top {
    font-size: 13px;
    font-weight: 600;
    color: var(--primary-blue-deep);
    line-height: 1.4;
    max-width: 150px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .stat-label {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 4px;
  }

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

  .msg-text {
    color: var(--text-secondary);
  }

  .reply-text {
    color: var(--text-secondary);
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .chat-detail {
    .chat-messages {
      margin-top: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message {
      padding: 14px 18px;
      border-radius: var(--radius-sm);

      .message-label {
        font-size: 12px;
        color: var(--text-muted);
        margin-bottom: 8px;
        font-weight: 600;
      }

      .message-content {
        font-size: 14px;
        color: var(--text-primary);
        line-height: 1.7;
      }
    }

    .user-message {
      background: linear-gradient(135deg, var(--primary-blue-bg) 0%, #fff 100%);
      border-left: 3px solid var(--primary-blue);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
    }

    .ai-message {
      background: linear-gradient(135deg, var(--primary-orange-bg) 0%, #fff 100%);
      border-left: 3px solid var(--primary-orange);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
    }
  }
}
</style>
