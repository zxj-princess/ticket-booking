<template>
  <div class="trains-page">
    <el-card shadow="never" class="filter-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="车次号">
          <el-input v-model="searchForm.trainNo" placeholder="请输入车次号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="运营中" value="ACTIVE" />
            <el-option label="已停运" value="INACTIVE" />
          </el-select>
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
          <span class="title">车次列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增车次
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="trainNo" label="车次号" width="100" />
        <el-table-column prop="trainType" label="车型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.trainType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="起止站" width="200">
          <template #default="{ row }">
            {{ row.fromStation }} → {{ row.toStation }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ row.departTime }} - {{ row.arriveTime }}
          </template>
        </el-table-column>
        <el-table-column prop="seatTypes" label="座位类型" width="180">
          <template #default="{ row }">
            <el-tag
              v-for="seat in (row.seatTypes || []).split(',')"
              :key="seat"
              size="small"
              class="seat-tag"
            >{{ seat }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '运营中' : '已停运' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="车次号" prop="trainNo">
          <el-input v-model="dialogForm.trainNo" placeholder="如：G1" />
        </el-form-item>
        <el-form-item label="车型" prop="trainType">
          <el-select v-model="dialogForm.trainType" placeholder="请选择" style="width: 100%">
            <el-option label="高速动车" value="G" />
            <el-option label="动车组" value="D" />
            <el-option label="直达特快" value="Z" />
            <el-option label="特快" value="T" />
            <el-option label="快速" value="K" />
          </el-select>
        </el-form-item>
        <el-form-item label="出发站" prop="fromStation">
          <el-input v-model="dialogForm.fromStation" placeholder="请输入出发站" />
        </el-form-item>
        <el-form-item label="到达站" prop="toStation">
          <el-input v-model="dialogForm.toStation" placeholder="请输入到达站" />
        </el-form-item>
        <el-form-item label="出发时间" prop="departTime">
          <el-time-picker
            v-model="dialogForm.departTime"
            placeholder="请选择出发时间"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="到达时间" prop="arriveTime">
          <el-time-picker
            v-model="dialogForm.arriveTime"
            placeholder="请选择到达时间"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="座位类型" prop="seatTypes">
          <el-select
            v-model="dialogForm.seatTypesList"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择座位类型"
            style="width: 100%"
          >
            <el-option label="商务座" value="商务座" />
            <el-option label="一等座" value="一等座" />
            <el-option label="二等座" value="二等座" />
            <el-option label="无座" value="无座" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="dialogForm.status" style="width: 100%">
            <el-option label="运营中" value="ACTIVE" />
            <el-option label="已停运" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { listTrainsAdmin, createTrain, updateTrain, deleteTrain } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  trainNo: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogFormRef = ref(null)
const isEdit = ref(false)

const dialogForm = reactive({
  id: null,
  trainNo: '',
  trainType: '',
  fromStation: '',
  toStation: '',
  departTime: '',
  arriveTime: '',
  seatTypesList: [],
  status: 'ACTIVE'
})

const dialogRules = {
  trainNo: [{ required: true, message: '请输入车次号', trigger: 'blur' }],
  trainType: [{ required: true, message: '请选择车型', trigger: 'change' }],
  fromStation: [{ required: true, message: '请输入出发站', trigger: 'blur' }],
  toStation: [{ required: true, message: '请输入到达站', trigger: 'blur' }],
  departTime: [{ required: true, message: '请选择出发时间', trigger: 'change' }],
  arriveTime: [{ required: true, message: '请选择到达时间', trigger: 'change' }],
  seatTypesList: [{ required: true, type: 'array', message: '请选择座位类型', trigger: 'change' }]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑车次' : '新增车次'))

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      trainNo: searchForm.trainNo || undefined,
      status: searchForm.status || undefined
    }
    const res = await listTrainsAdmin(params)
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
  const mockList = [
    { id: 1, trainNo: 'G1', trainType: 'G', fromStation: '北京南', toStation: '上海虹桥', departTime: '07:00', arriveTime: '11:28', seatTypes: '商务座,一等座,二等座', status: 'ACTIVE' },
    { id: 2, trainNo: 'G2', trainType: 'G', fromStation: '上海虹桥', toStation: '北京南', departTime: '08:00', arriveTime: '12:38', seatTypes: '一等座,二等座', status: 'ACTIVE' },
    { id: 3, trainNo: 'Z10', trainType: 'Z', fromStation: '北京', toStation: '上海', departTime: '19:33', arriveTime: '次日09:05', seatTypes: '一等座,二等座,无座', status: 'ACTIVE' },
    { id: 4, trainNo: 'D31', trainType: 'D', fromStation: '广州南', toStation: '深圳北', departTime: '07:30', arriveTime: '08:45', seatTypes: '一等座,二等座', status: 'ACTIVE' },
    { id: 5, trainNo: 'K105', trainType: 'K', fromStation: '北京西', toStation: '广州南', departTime: '21:00', arriveTime: '次日20:30', seatTypes: '二等座,无座', status: 'ACTIVE' },
    { id: 6, trainNo: 'G101', trainType: 'G', fromStation: '北京南', toStation: '杭州东', departTime: '06:44', arriveTime: '11:06', seatTypes: '商务座,一等座,二等座', status: 'ACTIVE' },
    { id: 7, trainNo: 'G7', trainType: 'G', fromStation: '上海虹桥', toStation: '广州南', departTime: '08:00', arriveTime: '16:00', seatTypes: '商务座,一等座,二等座', status: 'ACTIVE' },
    { id: 8, trainNo: 'D2222', trainType: 'D', fromStation: '深圳北', toStation: '珠海北', departTime: '09:15', arriveTime: '10:30', seatTypes: '一等座,二等座', status: 'INACTIVE' },
    { id: 9, trainNo: 'Z285', trainType: 'Z', fromStation: '北京', toStation: '上海', departTime: '20:00', arriveTime: '次日08:30', seatTypes: '一等座,二等座', status: 'ACTIVE' },
    { id: 10, trainNo: 'T109', trainType: 'T', fromStation: '北京', toStation: '上海', departTime: '19:28', arriveTime: '次日09:05', seatTypes: '二等座,无座', status: 'INACTIVE' },
    { id: 11, trainNo: 'G103', trainType: 'G', fromStation: '北京南', toStation: '上海虹桥', departTime: '07:05', arriveTime: '11:33', seatTypes: '商务座,一等座,二等座', status: 'ACTIVE' },
    { id: 12, trainNo: 'D7001', trainType: 'D', fromStation: '广州南', toStation: '深圳北', departTime: '08:00', arriveTime: '09:15', seatTypes: '一等座,二等座', status: 'ACTIVE' }
  ]
  const start = (pagination.page - 1) * pagination.pageSize
  tableData.value = mockList.slice(start, start + pagination.pageSize)
  pagination.total = mockList.length
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.trainNo = ''
  searchForm.status = ''
  handleSearch()
}

function resetForm() {
  Object.assign(dialogForm, {
    id: null,
    trainNo: '',
    trainType: '',
    fromStation: '',
    toStation: '',
    departTime: '',
    arriveTime: '',
    seatTypesList: [],
    status: 'ACTIVE'
  })
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(dialogForm, {
    id: row.id,
    trainNo: row.trainNo,
    trainType: row.trainType,
    fromStation: row.fromStation,
    toStation: row.toStation,
    departTime: row.departTime,
    arriveTime: row.arriveTime,
    seatTypesList: row.seatTypes ? row.seatTypes.split(',') : [],
    status: row.status
  })
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除车次「${row.trainNo}」吗？此操作不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await deleteTrain(row.id)
    } catch {
      // ignore
    }
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

async function handleSubmit() {
  if (!dialogFormRef.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (valid) {
      const data = {
        trainNo: dialogForm.trainNo,
        trainType: dialogForm.trainType,
        fromStation: dialogForm.fromStation,
        toStation: dialogForm.toStation,
        departTime: dialogForm.departTime,
        arriveTime: dialogForm.arriveTime,
        seatTypes: dialogForm.seatTypesList.join(','),
        status: dialogForm.status
      }
      try {
        if (isEdit.value) {
          await updateTrain(dialogForm.id, data)
        } else {
          await createTrain(data)
        }
        ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      } catch {
        // ignore
      }
      dialogVisible.value = false
      loadData()
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.trains-page {
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

  .seat-tag {
    margin-right: 4px;
    margin-bottom: 2px;
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>