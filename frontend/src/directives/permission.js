import { useUserStore } from '@/stores/user'

export const permissionDirective = {
  mounted(el, binding) {
    const code = binding.value
    if (!code) return
    const user = useUserStore()
    if (!user.hasPerm(code)) {
      el.parentNode?.removeChild(el)
    }
  },
}
