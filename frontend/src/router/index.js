import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Login from '@/views/Login.vue'
import MainLayout from '@/layouts/MainLayout.vue'
import Forbidden from '@/views/Forbidden.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { public: true } },
    { path: '/403', name: 'Forbidden', component: Forbidden, meta: { public: true } },
    {
      path: '/',
      name: 'Main',
      component: MainLayout,
      redirect: { name: 'MainRedirect' },
      children: [
        {
          path: '',
          name: 'MainRedirect',
          component: { render: () => null },
          beforeEnter: (_to, _from, next) => {
            const user = useUserStore()
            const p = user.defaultHomePath()
            next(p || '/403')
          },
        },
        {
          path: 'mag/project/:projectId',
          name: 'MagProjectWorkspace',
          component: () => import('@/views/mag/ProjectWorkspace.vue'),
          meta: { title: 'MAG 工作台', permission: 'mag:project:list' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  const user = useUserStore()
  if (to.meta.public) {
    if (to.path === '/login' && user.token) {
      next(user.defaultHomePath())
      return
    }
    next()
    return
  }
  if (!user.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (!user.user) {
    try {
      await user.loadSession()
    } catch {
      user.clear()
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  } else if (!user.dynamicRegistered) {
    user.registerDynamicRoutes()
  }
  const need = to.meta.permission
  if (need && !user.hasPerm(need)) {
    next('/403')
    return
  }
  next()
})

export default router
