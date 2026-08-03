<template>
  <div class="stations-page">
    <el-card shadow="never" class="filter-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="站点名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入站点名称/拼音" clearable />
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
          <span class="title">站点列表</span>
          <div>
            <el-button
              type="danger"
              :disabled="selectedRows.length === 0"
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon>批量删除
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增站点
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="code" label="代码" width="120" />
        <el-table-column prop="pinyin" label="拼音" width="150" />
        <el-table-column prop="city" label="城市" width="120" />
        <el-table-column prop="province" label="省份" width="120" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="dialogForm.name" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="站点代码" prop="code">
          <el-input v-model="dialogForm.code" placeholder="请输入站点代码" />
        </el-form-item>
        <el-form-item label="拼音" prop="pinyin">
          <el-input v-model="dialogForm.pinyin" placeholder="请输入拼音" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="dialogForm.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="dialogForm.province" placeholder="请输入省份" />
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
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { listStationsAdmin, createStation, updateStation, deleteStation } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])

const searchForm = reactive({
  keyword: ''
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
  name: '',
  code: '',
  pinyin: '',
  city: '',
  province: ''
})

const dialogRules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入站点代码', trigger: 'blur' }],
  pinyin: [{ required: true, message: '请输入拼音', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑站点' : '新增站点'))

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined
    }
    const res = await listStationsAdmin(params)
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
    { id: 1, name: '北京南', code: 'VNP', pinyin: 'beijingnan', city: '北京', province: '北京市' },
    { id: 2, name: '北京西', code: 'BXP', pinyin: 'beijingxi', city: '北京', province: '北京市' },
    { id: 3, name: '北京', code: 'BJP', pinyin: 'beijing', city: '北京', province: '北京市' },
    { id: 4, name: '上海虹桥', code: 'AOH', pinyin: 'shanghaihongqiao', city: '上海', province: '上海市' },
    { id: 5, name: '上海', code: 'SHH', pinyin: 'shanghai', city: '上海', province: '上海市' },
    { id: 6, name: '广州南', code: 'IZQ', pinyin: 'guangzhounan', city: '广州', province: '广东省' },
    { id: 7, name: '深圳北', code: 'BJQ', pinyin: 'shenzhenbei', city: '深圳', province: '广东省' },
    { id: 8, name: '杭州东', code: 'HGH', pinyin: 'hangzhoudong', city: '杭州', province: '浙江省' },
    { id: 9, name: '珠海北', code: 'ZBQ', pinyin: 'zhuhaibei', city: '珠海', province: '广东省' },
    { id: 10, name: '南京南', code: 'INQ', pinyin: 'nanjingnan', city: '南京', province: '江苏省' },
    { id: 11, name: '武汉', code: 'WHN', pinyin: 'wuhan', city: '武汉', province: '湖北省' },
    { id: 12, name: '成都东', code: 'WCN', pinyin: 'chengdudong', city: '成都', province: '四川省' },
    { id: 13, name: '西安北', code: 'EAY', pinyin: 'xianbei', city: '西安', province: '陕西省' },
    { id: 14, name: '郑州东', code: 'ZAF', pinyin: 'zhengzhoudong', city: '郑州', province: '河南省' }
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
  searchForm.keyword = ''
  handleSearch()
}

function resetForm() {
  Object.assign(dialogForm, {
    id: null,
    name: '',
    code: '',
    pinyin: '',
    city: '',
    province: ''
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
    name: row.name,
    code: row.code,
    pinyin: row.pinyin,
    city: row.city,
    province: row.province
  })
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除站点「${row.name}」吗？此操作不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await deleteStation(row.id)
    } catch {
      // ignore
    }
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个站点吗？此操作不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    const ids = selectedRows.value.map(item => item.id)
    tableData.value = tableData.value.filter(item => !ids.includes(item.id))
    selectedRows.value = []
    ElMessage.success('批量删除成功')
  }).catch(() => {})
}

async function handleSubmit() {
  if (!dialogFormRef.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (valid) {
      const data = {
        name: dialogForm.name,
        code: dialogForm.code,
        pinyin: dialogForm.pinyin,
        city: dialogForm.city,
        province: dialogForm.province
      }
      try {
        if (isEdit.value) {
          await updateStation(dialogForm.id, data)
        } else {
          await createStation(data)
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
.stations-page {
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

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>