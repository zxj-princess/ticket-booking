import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code === undefined || res.code === null) {
      return res
    }

    if (res.code === 200 || res.code === 0) {
      return res
    }

    const message = res.msg || res.message || '请求失败'

    if (res.code === 401) {
      ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const userStore = useUserStore()
        userStore.clearAuth()
        router.push('/login')
      })
      return Promise.reject(new Error(message))
    }

    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      const message = error.response.data?.msg || error.response.data?.message || error.message

      if (status === 401) {
        ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          const userStore = useUserStore()
          userStore.clearAuth()
          router.push('/login')
        })
      } else if (status === 403) {
        ElMessage.error('没有访问权限')
      } else if (status === 404) {
        ElMessage.error('请求资源不存在')
      } else if (status === 500) {
        ElMessage.error(message || '服务器内部错误')
      } else {
        ElMessage.error(message || '请求失败')
      }
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }

    return Promise.reject(error)
  }
)

export default request