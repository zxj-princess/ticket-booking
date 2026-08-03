import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '首页', requiresAuth: false }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/user/Search.vue'),
        meta: { title: '车次查询', requiresAuth: false }
      },
      {
        path: 'order/:trainId',
        name: 'Order',
        component: () => import('@/views/user/Order.vue'),
        meta: { title: '预订车票', requiresAuth: true }
      },
      {
        path: 'my-orders',
        name: 'MyOrders',
        component: () => import('@/views/user/MyOrders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'change/:orderId',
        name: 'Change',
        component: () => import('@/views/user/Change.vue'),
        meta: { title: '改签', requiresAuth: true }
      },
      {
        path: 'refund/:orderId',
        name: 'Refund',
        component: () => import('@/views/user/Refund.vue'),
        meta: { title: '退票', requiresAuth: true }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('@/views/user/AiChat.vue'),
        meta: { title: 'AI 助手', requiresAuth: false }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板', requiresAuth: true }
      },
      {
        path: 'trains',
        name: 'AdminTrains',
        component: () => import('@/views/admin/Trains.vue'),
        meta: { title: '车次管理', requiresAuth: true }
      },
      {
        path: 'stations',
        name: 'AdminStations',
        component: () => import('@/views/admin/Stations.vue'),
        meta: { title: '站点管理', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理', requiresAuth: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', requiresAuth: true }
      },
      {
        path: 'chat-records',
        name: 'AdminChatRecords',
        component: () => import('@/views/admin/ChatRecords.vue'),
        meta: { title: 'AI对话管理', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')

  if (to.meta.requiresAuth !== false) {
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else if (to.meta.role && userInfo) {
      const role = JSON.parse(userInfo).role
      if (to.meta.role !== role) {
        next({ path: '/home' })
      } else {
        next()
      }
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router