<template>
  <div class="tag-selector" ref="rootRef">
    <label v-if="label" class="tag-selector-label">{{ label }}</label>

    <!-- Selected tags -->
    <div class="selected-tags" v-if="modelValue && modelValue.length > 0">
      <TagBadge
        v-for="tag in modelValue"
        :key="tag.id"
        :tag="tag"
        removable
        @remove="removeTag"
      />
    </div>

    <!-- Search input -->
    <div class="tag-input-wrap">
      <input
        ref="inputRef"
        v-model="searchQuery"
        class="tag-search-input"
        :placeholder="modelValue && modelValue.length > 0 ? '继续添加标签...' : '搜索或创建标签...'"
        @focus="showDropdown = true"
        @keydown.enter.prevent="handleEnter"
        @keydown.down.prevent="moveHighlight(1)"
        @keydown.up.prevent="moveHighlight(-1)"
        @keydown.escape="showDropdown = false"
      />
    </div>

    <!-- Dropdown -->
    <div v-if="showDropdown" class="tag-dropdown">
      <!-- Loading state -->
      <div v-if="loading" class="dropdown-empty">加载中...</div>

      <!-- No results + create hint -->
      <template v-else-if="filteredTags.length === 0">
        <div v-if="searchQuery.trim()" class="dropdown-create" @click="createAndSelect">
          <Plus :size="14" />
          <span>创建标签 "{{ searchQuery.trim() }}"</span>
        </div>
        <div v-else class="dropdown-empty">暂无标签</div>
      </template>

      <!-- Tag list -->
      <template v-else>
        <div
          v-for="(tag, idx) in filteredTags"
          :key="tag.id"
          class="dropdown-item"
          :class="{ highlighted: idx === highlightIndex }"
          role="option"
          @click="selectTag(tag)"
          @mouseenter="highlightIndex = idx"
        >
          <span class="dropdown-item-name">{{ tag.name }}</span>
          <span class="dropdown-item-count">{{ tag.usageCount }} 次使用</span>
        </div>

        <!-- Create new tag option (when search query doesn't match exactly) -->
        <div
          v-if="searchQuery.trim() && !exactMatch"
          class="dropdown-create"
          :class="{ highlighted: highlightIndex === filteredTags.length }"
          @click="createAndSelect"
        >
          <Plus :size="14" />
          <span>创建标签 "{{ searchQuery.trim() }}"</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Plus } from '@lucide/vue'
import type { Tag } from '@/types'
import { tagApi } from '@/services/api'
import TagBadge from './TagBadge.vue'

const props = defineProps<{
  modelValue: Tag[]
  tagType: string
  label?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', tags: Tag[]): void
}>()

const rootRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()
const searchQuery = ref('')
const showDropdown = ref(false)
const loading = ref(false)
const highlightIndex = ref(0)
const allTags = ref<Tag[]>([])

// Load tags on mount
onMounted(async () => {
  await loadTags()
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

async function loadTags() {
  loading.value = true
  try {
    allTags.value = await tagApi.getTags(props.tagType)
  } catch {
    allTags.value = []
  } finally {
    loading.value = false
  }
}

// Filter tags: exclude already selected, apply search
const filteredTags = computed(() => {
  const selectedIds = new Set(props.modelValue.map(t => t.id))
  let tags = allTags.value.filter(t => !selectedIds.has(t.id))
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    tags = tags.filter(t => t.name.toLowerCase().includes(q))
  }
  return tags
})

// Check if search query exactly matches an existing tag
const exactMatch = computed(() => {
  if (!searchQuery.value.trim()) return false
  const q = searchQuery.value.trim().toLowerCase()
  return allTags.value.some(t => t.name.toLowerCase() === q)
})

function selectTag(tag: Tag) {
  const newTags = [...(props.modelValue || []), tag]
  emit('update:modelValue', newTags)
  searchQuery.value = ''
  showDropdown.value = false
  inputRef.value?.focus()
}

function removeTag(tagId: number) {
  const newTags = (props.modelValue || []).filter(t => t.id !== tagId)
  emit('update:modelValue', newTags)
}

function moveHighlight(delta: number) {
  const maxIdx = filteredTags.value.length + (searchQuery.value.trim() && !exactMatch.value ? 0 : -1)
  highlightIndex.value = Math.max(0, Math.min(highlightIndex.value + delta, maxIdx))
}

function handleEnter() {
  if (highlightIndex.value < filteredTags.value.length) {
    selectTag(filteredTags.value[highlightIndex.value])
  } else if (searchQuery.value.trim() && !exactMatch.value) {
    createAndSelect()
  }
}

async function createAndSelect() {
  const name = searchQuery.value.trim()
  if (!name) return
  try {
    const newTag = await tagApi.createTag(name, props.tagType)
    // Add to local list
    allTags.value.push(newTag)
    // Select it
    const newTags = [...(props.modelValue || []), newTag]
    emit('update:modelValue', newTags)
    searchQuery.value = ''
    showDropdown.value = false
  } catch {
    // Tag creation failed (e.g., duplicate)
  }
}

function handleClickOutside(e: MouseEvent) {
  if (rootRef.value && !rootRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

// Reset highlight when search changes
watch(searchQuery, () => {
  highlightIndex.value = 0
})
</script>

<style scoped>
.tag-selector {
  position: relative;
  width: 100%;
}

.tag-selector-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag-input-wrap {
  position: relative;
}

.tag-search-input {
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.tag-search-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.15);
}

.tag-search-input::placeholder {
  color: var(--text-muted);
}

.tag-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  margin-top: 4px;
  padding: 4px;
  max-height: 200px;
  overflow-y: auto;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: var(--shadow-md);
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.dropdown-item:hover,
.dropdown-item.highlighted {
  background: rgba(139, 92, 246, 0.1);
}

.dropdown-item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-item-count {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 8px;
}

.dropdown-create {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 13px;
  color: var(--accent-1);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.dropdown-create:hover,
.dropdown-create.highlighted {
  background: rgba(139, 92, 246, 0.1);
}

.dropdown-empty {
  padding: 8px 10px;
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}

/* Light theme */
[data-theme="light"] .tag-dropdown {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

[data-theme="light"] .dropdown-item:hover,
[data-theme="light"] .dropdown-item.highlighted {
  background: rgba(139, 92, 246, 0.06);
}

@media (prefers-reduced-motion: reduce) {
  .tag-search-input,
  .dropdown-item,
  .dropdown-create {
    transition: none;
  }
}
</style>
