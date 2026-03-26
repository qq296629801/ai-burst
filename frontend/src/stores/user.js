import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, fetchMe } from '@/api/auth'
import router from '@/router'

const TOKEN_KEY = 'aiburst_token'

const viewModules = import.meta.glob('../views/**/*.vue')

function resolvePage(componentPath) {
  if (!componentPath) {
    return viewModules['../views/Forbidden.vue']
  }
  const key = `../views/${componentPath}.vue`
  return viewModules[key] || viewModules['../views/Forbidden.vue']
}

function collectMenuRoutes(nodes, out) {
  if (!nodes) return
  for (const n of nodes) {
    if (n.permType === 2 && n.path && n.component) {
      out.push(n)
    }
    if (n.children?.length) {
      collectMenuRoutes(n.children, out)
    }
  }
}

function firstLeafPath(nodes) {
  if (!nodes) return ''
  for (const n of nodes) {
    if (n.permType === 2 && n.path) {
      return n.path
    }
    const sub = firstLeafPath(n.children)
    if (sub) return sub
  }
  return ''
}

function trimChildPath(fullPath) {
  if (!fullPath) return ''
  return fullPath.startsWith('/') ? fullPath.slice(1) : fullPath
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
    permissions: [],
    menus: [],
    dynamicRegistered: false,
  }),
  getters: {
    permSet: (s) => new Set(s.permissions || []),
  },
  actions: {
    hasPerm(code) {
      if (!code) return true
      return this.permSet.has(code)
    },
    applyLoginPayload(payload) {
      this.token = payload.token || this.token
      this.user = payload.user
      this.permissions = payload.permissions || []
      this.menus = payload.menus || []
      if (this.token) {
        localStorage.setItem(TOKEN_KEY, this.token)
      }
    },
    registerDynamicRoutes() {
      if (this.dynamicRegistered) return
      const leaves = []
      collectMenuRoutes(this.menus, leaves)
      for (const m of leaves) {
        const childPath = trimChildPath(m.path)
        const routeName = 'menu-' + m.id
        if (!childPath || router.hasRoute(routeName)) {
          continue
        }
        router.addRoute('Main', {
          path: childPath,
          name: routeName,
          component: resolvePage(m.component),
          meta: { title: m.permName, permission: m.permCode },
        })
      }
      this.dynamicRegistered = true
    },
    clearDynamicRoutes() {
      this.dynamicRegistered = false
      router.getRoutes().forEach((r) => {
        if (r.name && String(r.name).startsWith('menu-')) {
          router.removeRoute(r.name)
        }
      })
    },
    async login(form) {
      const res = await apiLogin(form)
      this.applyLoginPayload(res.data)
      this.clearDynamicRoutes()
      this.registerDynamicRoutes()
    },
    async loadSession() {
      const res = await fetchMe()
      this.applyLoginPayload({ ...res.data, token: this.token })
      this.clearDynamicRoutes()
      this.registerDynamicRoutes()
    },
    async logout() {
      try {
        await apiLogout()
      } catch (_) {
        /* ignore */
      }
      this.token = ''
      this.user = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      this.clearDynamicRoutes()
    },
    clear() {
      this.token = ''
      this.user = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      this.clearDynamicRoutes()
    },
    defaultHomePath() {
      return firstLeafPath(this.menus) || '/403'
    },
  },
})
