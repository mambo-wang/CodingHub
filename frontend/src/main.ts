import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import '@/assets/main.css'

const app = createApp(App)

// Initialize Pinia store with localStorage token
const pinia = createPinia()
app.use(pinia)
import { useAuthStore } from '@/stores/auth'
useAuthStore().initFromStorage()

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

app.mount('#app')
