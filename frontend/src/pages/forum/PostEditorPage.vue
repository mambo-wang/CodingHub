<template>
  <div class="post-editor-page">
    <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div class="form-group">
      <input
        v-model="title"
        placeholder="标题"
        class="title-input"
      />
    </div>

    <div class="form-group">
      <label>分类</label>
      <select v-model="categoryId">
        <option value="">选择分类</option>
        <option
          v-for="cat in categories"
          :key="cat.id"
          :value="cat.id"
        >
          {{ cat.name }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label>可见性</label>
      <div class="visibility-options">
        <label class="visibility-option" :class="{ active: visibility === 'PUBLIC' }">
          <input type="radio" v-model="visibility" value="PUBLIC" />
          <Globe :size="16" />
          <span>公开</span>
        </label>
        <label class="visibility-option" :class="{ active: visibility === 'PRIVATE' }">
          <input type="radio" v-model="visibility" value="PRIVATE" />
          <Lock :size="16" />
          <span>私有</span>
        </label>
      </div>
      <p class="visibility-hint">
        {{ visibility === 'PRIVATE' ? '仅自己和管理员可见' : '所有人可见' }}
      </p>
    </div>

    <div class="form-group">
      <label>标签</label>
      <TagSelector v-model="selectedTags" tagType="FORUM" />
    </div>

    <div class="form-group">
      <div class="content-toolbar">
        <label>内容（Markdown）</label>
        <button
          type="button"
          class="upload-img-btn"
          :disabled="uploading"
          @click="triggerFileInput"
          title="插入图片"
        >
          <ImageIcon :size="16" />
          {{ uploading ? '上传中...' : '插入图片' }}
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleFileSelect"
        />
      </div>
      <textarea
        ref="contentRef"
        v-model="content"
        placeholder="输入内容... 支持粘贴或拖拽图片"
        class="content-input"
        rows="15"
        @paste="handlePaste"
        @drop.prevent="handleDrop"
        @dragover.prevent
      ></textarea>
    </div>

    <div class="form-actions">
      <button @click="publish" class="publish-btn">{{ isEdit ? '更新' : '发布' }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Globe, Lock, Image as ImageIcon } from '@lucide/vue';
import { useForumStore } from '@/stores/forum';
import forumService from '@/services/forum';
import api from '@/services/api';
import type { Tag } from '@/types';
import TagSelector from '@/components/common/TagSelector.vue';

const router = useRouter();
const route = useRoute();
const forumStore = useForumStore();
const { categories } = storeToRefs(forumStore);

const isEdit = computed(() => !!route.params.id);

const title = ref('');
const categoryId = ref<number | ''>('');
const content = ref('');
const visibility = ref('PUBLIC');
const errorMessage = ref('');
const loading = ref(false);
const selectedTags = ref<Tag[]>([]);

// 图片上传相关
const fileInputRef = ref<HTMLInputElement | null>(null);
const contentRef = ref<HTMLTextAreaElement | null>(null);
const uploading = ref(false);

const triggerFileInput = () => {
  fileInputRef.value?.click();
};

const handleFileSelect = (e: Event) => {
  const input = e.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    uploadImage(input.files[0]);
  }
  input.value = ''; // 重置，允许重复选择同一文件
};

const handlePaste = (e: ClipboardEvent) => {
  const items = e.clipboardData?.items;
  if (!items) return;
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault();
      const file = item.getAsFile();
      if (file) uploadImage(file);
      break;
    }
  }
};

const handleDrop = (e: DragEvent) => {
  const files = e.dataTransfer?.files;
  if (!files) return;
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      uploadImage(file);
      break;
    }
  }
};

const insertAtCursor = (text: string) => {
  const textarea = contentRef.value;
  if (!textarea) {
    content.value += text;
    return;
  }
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  content.value = content.value.substring(0, start) + text + content.value.substring(end);
  // 恢复光标位置到插入文本之后
  requestAnimationFrame(() => {
    textarea.selectionStart = textarea.selectionEnd = start + text.length;
    textarea.focus();
  });
};

const uploadImage = async (file: File) => {
  if (uploading.value) return;
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/uploads/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    const url = res.data.data?.url;
    if (url) {
      insertAtCursor(`![${file.name}](${url})\n`);
    }
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || '图片上传失败，请重试';
  } finally {
    uploading.value = false;
  }
};

onMounted(async () => {
  await forumStore.fetchCategories();
  if (route.params.id) {
    try {
      const post = await forumService.getPostById(Number(route.params.id));
      title.value = post.title;
      categoryId.value = post.categoryId;
      content.value = post.content;
      visibility.value = post.visibility || 'PUBLIC';
      if (post.tags) {
        selectedTags.value = post.tags;
      }
    } catch (e) {
      errorMessage.value = '加载帖子失败';
    }
  }
});

const publish = async () => {
  errorMessage.value = '';

  if (!title.value.trim()) {
    errorMessage.value = '请填写标题';
    return;
  }

  if (!categoryId.value) {
    errorMessage.value = '请选择分类';
    return;
  }

  loading.value = true;
  try {
    const data = {
      title: title.value,
      content: content.value,
      categoryId: categoryId.value as number,
      tagIds: selectedTags.value.map(t => t.id),
      visibility: visibility.value
    };
    if (isEdit.value) {
      await forumService.updatePost(Number(route.params.id), data);
      router.push(`/forum/posts/${route.params.id}`);
    } else {
      await forumService.createPost(data);
      router.push('/forum');
    }
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || (isEdit.value ? '更新失败，请重试' : '发布失败，请重试');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.post-editor-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.post-editor-page h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-editor-page h1::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 28px;
  background: linear-gradient(180deg, var(--accent-1), var(--accent-2));
  border-radius: 2px;
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

.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.content-toolbar label {
  margin-bottom: 0;
}

.upload-img-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-glass);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-img-btn:hover:not(:disabled) {
  border-color: var(--accent-1);
  color: var(--accent-1);
  background: rgba(139, 92, 246, 0.08);
}

.upload-img-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.title-input {
  width: 100%;
  padding: 14px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  outline: none;
  transition: all 0.2s;
}

.title-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.title-input::placeholder {
  color: var(--text-muted);
}

select {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

select:focus {
  border-color: var(--accent-1);
}

.visibility-options {
  display: flex;
  gap: 12px;
}

.visibility-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
  user-select: none;
}

.visibility-option input[type="radio"] {
  display: none;
}

.visibility-option:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.visibility-option.active {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
  color: var(--accent-1);
  font-weight: 500;
}

.visibility-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.content-input {
  width: 100%;
  padding: 14px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: all 0.2s;
  min-height: 300px;
}

.content-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.content-input::placeholder {
  color: var(--text-muted);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.form-actions button {
  padding: 12px 24px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.publish-btn {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.publish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  color: var(--accent-3);
  font-size: 14px;
  margin-bottom: 20px;
}
</style>