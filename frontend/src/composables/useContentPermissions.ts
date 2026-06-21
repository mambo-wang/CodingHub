import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * Content permission composable.
 * Returns computed refs indicating whether the current user can edit/delete content.
 * Permission: owner OR admin (ADMIN/SUPER_ADMIN role).
 */
export function useContentPermissions(ownerId: () => number | undefined | null) {
  const authStore = useAuthStore()

  const canEdit = computed(() => {
    if (!authStore.isLoggedIn) return false
    const userId = authStore.user?.id
    if (userId === undefined || userId === null) return false
    const oId = ownerId()
    if (oId === undefined || oId === null) return false
    return userId === oId || authStore.isAdmin
  })

  const canDelete = canEdit // same permission

  return { canEdit, canDelete }
}
