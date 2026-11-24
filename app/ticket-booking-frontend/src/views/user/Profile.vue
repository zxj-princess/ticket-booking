<template>
  <div class="profile-page page-container">
    <div class="page-header">
      <h2>个人中心</h2>
      <p class="header-desc">管理您的个人信息和乘客列表</p>
    </div>

    <div class="profile-layout">
      <div class="profile-main">
        <div class="card user-info-card">
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
            <el-button type="primary" size="small" class="edit-btn" @click="toggleEdit">
              {{ editing ? '取消' : '编辑' }}
            </el-button>
          </div>
          <div class="user-info-display">
            <div class="avatar-section">
              <el-avatar :size="80" :icon="UserFilled" class="user-avatar" />
              <h3 class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</h3>
              <p class="user-role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</p>
            </div>
            <div class="info-form" v-if="editing">
              <el-form :model="editForm" label-width="80px">
                <el-form-item label="昵称">
                  <el-input v-model="editForm.nickname" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="editForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="editForm.email" />
                </el-form-item>
                <el-button type="primary" @click="saveUserInfo">保存</el-button>
              </el-form>
            </div>
            <div class="info-details" v-else>
              <div class="detail-item">
                <span class="label">用户名</span>
                <span class="value">{{ userStore.userInfo?.username || '--' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">昵称</span>
                <span class="value">{{ userStore.userInfo?.nickname || '--' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">手机号</span>
                <span class="value">{{ userStore.userInfo?.phone || '--' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">邮箱</span>
                <span class="value">{{ userStore.userInfo?.email || '--' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">注册时间</span>
                <span class="value">{{ userStore.userInfo?.createTime || '--' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card passenger-card">
          <div class="card-title">
            <el-icon><UserFilled /></el-icon>
            <span>乘客管理</span>
            <el-button type="primary" size="small" class="add-btn" @click="openPassengerDialog()">
              <el-icon><Plus /></el-icon>
              添加乘客
            </el-button>
          </div>
          <div class="passenger-list">
            <div v-if="passengerList.length === 0" class="empty-passenger">
              <el-empty description="暂无乘客信息，点击添加" :image-size="80" />
            </div>
            <div v-for="p in passengerList" :key="p.id" class="passenger-item">
              <div class="passenger-info">
                <div class="passenger-avatar">
                  <el-avatar :size="40">{{ p.name?.charAt(0) }}</el-avatar>
                </div>
                <div class="passenger-detail">
                  <div class="passenger-name">{{ p.name }}</div>
                  <div class="passenger-meta">
                    <span>{{ p.type === 1 ? '学生' : '成人' }}</span>
                    <span class="divider">|</span>
                    <span>{{ p.idCard }}</span>
                    <span class="divider">|</span>
                    <span>{{ p.phone }}</span>
                  </div>
                </div>
              </div>
              <div class="passenger-actions">
                <el-button size="small" @click="openPassengerDialog(p)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button size="small" type="danger" @click="deletePassenger(p)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-sidebar">
        <div class="card password-card">
          <div class="card-title">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
          <el-form :model="passwordForm" label-width="90px" @submit.prevent="changePassword">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-button type="primary" @click="changePassword" :loading="changingPassword">
              修改密码
            </el-button>
          </el-form>
        </div>

        <div class="card action-card">
          <div class="card-title">
            <el-icon><SwitchButton /></el-icon>
            <span>账户操作</span>
          </div>
          <div class="action-list">
            <div class="action-item" @click="router.push('/my-orders')">
              <el-icon><Tickets /></el-icon>
              <span>我的订单</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-item" @click="router.push('/ai-chat')">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI 客服</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-item logout" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="passengerDialogVisible" :title="editingPassenger ? '编辑乘客' : '添加乘客'" width="450px">
      <el-form :model="passengerForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="passengerForm.name" placeholder="请输入乘客姓名" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="passengerForm.type">
            <el-radio :value="0">成人</el-radio>
            <el-radio :value="1">学生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="passengerForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="passengerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passengerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassenger">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  User, UserFilled, Plus, Edit, Delete, Lock, SwitchButton,
  ArrowRight, Tickets, ChatDotRound
} from '@element-plus/icons-vue'
import { updateUserInfo, changePassword as changePasswordApi, getMyPassengers, addPassenger as addPassengerApi, updatePassenger as updatePassengerApi, deletePassenger as deletePassengerApi } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const editing = ref(false)
const editForm = ref({
  nickname: '',
  phone: '',
  email: ''
})

const passengerList = ref([])
const passengerDialogVisible = ref(false)
const editingPassenger = ref(null)
const passengerForm = ref({
  name: '',
  type: 0,
  idCard: '',
  phone: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changingPassword = ref(false)

function toggleEdit() {
  if (editing.value) {
    editing.value = false
  } else {
    editForm.value = {
      nickname: userStore.userInfo?.nickname || '',
      phone: userStore.userInfo?.phone || '',
      email: userStore.userInfo?.email || ''
    }
    editing.value = true
  }
}

async function saveUserInfo() {
  try {
    await updateUserInfo(editForm.value)
    ElMessage.success('保存成功')
    await userStore.fetchUserInfo()
    editing.value = false
  } catch {
    ElMessage.error('保存失败')
  }
}

function openPassengerDialog(passenger = null) {
  if (passenger) {
    editingPassenger.value = passenger
    passengerForm.value = { ...passenger }
  } else {
    editingPassenger.value = null
    passengerForm.value = { name: '', type: 0, idCard: '', phone: '' }
  }
  passengerDialogVisible.value = true
}

function savePassenger() {
  if (!passengerForm.value.name || !passengerForm.value.idCard) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const data = {
    name: passengerForm.value.name,
    idCard: passengerForm.value.idCard,
    phone: passengerForm.value.phone,
    type: passengerForm.value.type || 0
  }
  if (editingPassenger.value) {
    updatePassengerApi(editingPassenger.value.id, data).then(() => {
      const idx = passengerList.value.findIndex(p => p.id === editingPassenger.value.id)
      if (idx !== -1) {
        passengerList.value[idx] = { ...passengerForm.value, id: editingPassenger.value.id }
      }
      ElMessage.success('乘客信息已更新')
    }).catch(() => {
      ElMessage.error('更新失败')
    })
  } else {
    addPassengerApi(data).then(res => {
      if (res.data) {
        passengerList.value.push({ ...res.data, ...passengerForm.value })
      } else {
        const newId = Date.now()
        passengerList.value.push({ ...passengerForm.value, id: newId })
      }
      ElMessage.success('乘客添加成功')
    }).catch(() => {
      const newId = Date.now()
      passengerList.value.push({ ...passengerForm.value, id: newId })
      ElMessage.success('乘客添加成功(本地)')
    })
  }
  passengerDialogVisible.value = false
}

async function deletePassenger(p) {
  try {
    await ElMessageBox.confirm(`确定删除乘客「${p.name}」吗？`, '删除确认', {
      type: 'warning'
    })
    if (p.id && p.id > 0) {
      try {
        await deletePassengerApi(p.id)
      } catch {}
    }
    const idx = passengerList.value.findIndex(item => item.id === p.id)
    if (idx !== -1) {
      passengerList.value.splice(idx, 1)
    }
    ElMessage.success('删除成功')
  } catch {}
}

async function changePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', { type: 'warning' })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  } catch {}
}

onMounted(async () => {
  try {
    const res = await getMyPassengers()
    if (res.data && res.data.length > 0) {
      passengerList.value = res.data.map(p => ({
        id: p.id,
        name: p.name,
        type: p.type ?? 0,
        idCard: p.idCard,
        phone: p.phone
      }))
    } else {
      passengerList.value = []
    }
  } catch {
    passengerList.value = []
  }
})
</script>

<style lang="scss" scoped>
.profile-page {
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

    .edit-btn,
    .add-btn {
      margin-left: auto;
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

.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.user-info-card {
  .user-info-display {
    display: flex;
    gap: 32px;
  }

  .avatar-section {
    text-align: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, var(--primary-blue-bg) 0%, #fff 100%);
    border-radius: var(--radius-sm);
    min-width: 140px;
    border: 1px solid var(--border-blue);

    .user-avatar {
      background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%) !important;
      margin-bottom: 12px;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
    }

    .user-name {
      margin: 0 0 4px;
      font-size: 18px;
      color: var(--primary-blue-deep);
      font-weight: 700;
    }

    .user-role {
      margin: 0;
      color: var(--text-muted);
      font-size: 13px;
    }
  }

  .info-form {
    flex: 1;

    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .info-details {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    align-content: start;
  }

  .detail-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 14px;
    background: var(--bg-soft);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-light);

    .label {
      color: var(--text-muted);
      font-size: 13px;
    }

    .value {
      color: var(--text-primary);
      font-weight: 500;
    }
  }
}

.passenger-card {
  .passenger-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .empty-passenger {
    padding: 20px 0;
  }

  .passenger-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    background: linear-gradient(90deg, var(--primary-blue-bg) 0%, #fff 100%);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-blue);
    transition: all 0.25s ease;

    &:hover {
      background: var(--primary-blue-lighter);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
    }
  }

  .passenger-info {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .passenger-avatar {
    :deep(.el-avatar) {
      background: linear-gradient(135deg, var(--primary-orange) 0%, var(--primary-orange-light) 100%) !important;
      color: #fff;
      font-weight: 600;
    }
  }

  .passenger-detail {
    .passenger-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--primary-blue-deep);
      margin-bottom: 6px;
    }

    .passenger-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-secondary);
      font-size: 13px;

      .divider {
        color: var(--border-medium);
      }
    }
  }

  .passenger-actions {
    display: flex;
    gap: 8px;
  }
}

.profile-sidebar {
  .password-card {
    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .action-card {
    .action-list {
      display: flex;
      flex-direction: column;
    }

    .action-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 12px;
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: all 0.2s ease;
      color: var(--text-secondary);
      border: 1px solid transparent;

      &:hover {
        background: var(--primary-blue-bg);
        color: var(--primary-blue-deep);
        border-color: var(--border-blue);
        transform: translateX(2px);
      }

      .el-icon {
        font-size: 18px;
        color: var(--primary-blue);
      }

      span {
        flex: 1;
        font-weight: 500;
      }

      .arrow {
        color: var(--text-muted);
        font-size: 14px;
      }

      &.logout {
        color: var(--brand-red);
        margin-top: 4px;
        border-top: 1px solid var(--border-light);
        border-radius: 0;
        padding-top: 18px;

        .el-icon {
          color: var(--brand-red);
        }

        &:hover {
          background: #fef2f2;
          color: var(--brand-red);
          border-color: #fecaca;
          border-radius: var(--radius-sm);
        }
      }
    }
  }
}
</style>
