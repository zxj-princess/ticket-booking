import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    if (res.data.user) {
      userInfo.value = res.data.user
      localStorage.setItem('userInfo', JSON.stringify(res.data.user))
    }
    return res
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      return res.data
    } catch {
      clearAuth()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuth()
    }
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function loadUserInfo() {
    const stored = localStorage.getItem('userInfo')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch {
        localStorage.removeItem('userInfo')
      }
    }
  }

  loadUserInfo()

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    fetchUserInfo,
    logout,
    clearAuth
  }
})