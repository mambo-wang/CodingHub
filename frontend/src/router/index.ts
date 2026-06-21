import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/HomePage.vue')
  },
  {
    path: '/tools/:id',
    name: 'ToolDetail',
    component: () => import('@/pages/DetailPage.vue')
  },
  {
    path: '/tools/upload',
    name: 'UploadTool',
    component: () => import('@/pages/UploadPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/me/tools',
    name: 'MyTools',
    component: () => import('@/pages/MyToolsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/me/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/me/tools/:id/edit',
    name: 'EditTool',
    component: () => import('@/pages/EditToolPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/RegisterPage.vue')
  },
  {
    path: '/forum',
    name: 'ForumList',
    component: () => import('@/pages/forum/PostListPage.vue')
  },
  {
    path: '/forum/posts/:id/edit',
    name: 'EditPost',
    component: () => import('@/pages/forum/PostEditorPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/forum/posts/:id',
    name: 'ForumPostDetail',
    component: () => import('@/pages/forum/PostDetailPage.vue')
  },
  {
    path: '/forum/editor',
    name: 'ForumEditor',
    component: () => import('@/pages/forum/PostEditorPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/forum/my-posts',
    name: 'MyPosts',
    component: () => import('@/pages/forum/MyPostsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/forum/my-favorites',
    name: 'MyFavorites',
    component: () => import('@/pages/forum/MyFavoritesPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/overview',
    name: 'Overview',
    component: () => import('@/pages/OverviewPage.vue')
  },
  {
    path: '/quickstart',
    name: 'QuickStart',
    component: () => import('@/pages/QuickStartPage.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/pages/AboutPage.vue')
  },
  {
    path: '/videos',
    name: 'VideoList',
    component: () => import('@/pages/video/VideoListPage.vue')
  },
  {
    path: '/videos/upload',
    name: 'VideoUpload',
    component: () => import('@/pages/video/VideoUploadPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/videos/:id/edit',
    name: 'EditVideo',
    component: () => import('@/pages/video/VideoEditPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/videos/:id',
    name: 'VideoDetail',
    component: () => import('@/pages/video/VideoDetailPage.vue')
  },
  {
    path: '/admin/approvals',
    name: 'AdminApprovals',
    component: () => import('@/pages/admin/ApprovalPage.vue'),
    meta: { requiresAuth: true, roles: ['SUPER_ADMIN'] }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/pages/admin/UserListPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFoundPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard for protected routes
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // Role-based access control
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const userRole = authStore.user?.role
    if (!userRole || !requiredRoles.includes(userRole)) {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

export default router
