<template>
  <div class="video-edit-page">
    <div class="edit-container glass-card">
      <h1>编辑视频信息</h1>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <Loader2 :size="24" class="spin" />
        <span>加载中...</span>
      </div>

      <template v-else>
        <div class="form-group">
          <label>标题</label>
          <input v-model="title" placeholder="视频标题" class="form-input" />
        </div>

        <div class="form-group">
          <label>简介</label>
          <textarea
            v-model="description"
            placeholder="视频简介..."
            class="form-textarea"
            rows="6"
          ></textarea>
        </div>

        <div class="form-group">
          <label>标签</label>
          <TagSelector v-model="selectedTags" tagType="VIDEO" />
        </div>

        <VideoCoverPicker
          :videoSrc="`/api/v1/videos/${videoId}/stream`"
          :coverUrl="currentCoverUrl"
          @cover-capture="(blob) => { coverBlob = blob; coverFile = null }"
          @cover-upload="(file) => { coverFile = file; coverBlob = null }"
          @cover-remove="() => { coverBlob = null; coverFile = null }"
        />

        <div class="form-actions">
          <button @click="router.back()" class="cancel-btn">取消</button>
          <button @click="save" class="save-btn" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loader2 } from '@lucide/vue'
import { videoService } from '@/services/video'
import type { Tag } from '@/types'
import TagSelector from '@/components/common/TagSelector.vue'
import VideoCoverPicker from '@/components/video/VideoCoverPicker.vue'

const route = useRoute()
const router = useRouter()

const title = ref('')
const description = ref('')
const errorMessage = ref('')
const loading = ref(true)
const saving = ref(false)
const selectedTags = ref<Tag[]>([])
const coverBlob = ref<Blob | null>(null)
const coverFile = ref<File | null>(null)
const currentCoverUrl = ref<string | null>(null)
const videoId = ref(0)

onMounted(async () => {
  try {
    videoId.value = Number(route.params.id)
    const video = await videoService.getVideoDetail(videoId.value)
    title.value = video.title
    description.value = video.description || ''
    currentCoverUrl.value = video.coverUrl || null
    if (video.tags) {
      selectedTags.value = video.tags
    }
  } catch (e) {
    errorMessage.value = '加载视频信息失败'
  } finally {
    loading.value = false
  }
})

const save = async () => {
  errorMessage.value = ''
  if (!title.value.trim()) {
    errorMessage.value = '请填写标题'
    return
  }
  saving.value = true
  try {
    const tagIds = selectedTags.value.map(t => t.id)
    await videoService.updateVideo(videoId.value, {
      title: title.value,
      description: description.value || undefined,
      tagIds: tagIds.length > 0 ? tagIds : undefined
    })

    // Upload cover if changed
    if (coverBlob.value) {
      await videoService.uploadCover(videoId.value, coverBlob.value)
    } else if (coverFile.value) {
      await videoService.uploadCover(videoId.value, coverFile.value)
    }

    router.push(`/videos/${route.params.id}`)
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.video-edit-page {
  max-width: 640px;
  margin: 40px auto;
  padding: 0 20px;
}

.edit-container {
  padding: 32px;
}

.edit-container h1 {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.error-message {
  padding: 12px 16px;
  margin-bottom: 16px;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
  font-size: 14px;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  justify-content: center;
  color: var(--text-muted);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 16px;
  transition: border-color 200ms;
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-1);
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  min-height: 120px;
  font-family: var(--font-mono);
  transition: border-color 200ms;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--accent-1);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.cancel-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1.5px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 200ms;
}

.cancel-btn:hover {
  border-color: var(--accent-1);
  color: var(--accent-1);
}

.save-btn {
  padding: 10px 24px;
  border-radius: 8px;
  border: none;
  background: var(--accent-1);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms;
}

.save-btn:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.4);
  transform: translateY(-1px);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
