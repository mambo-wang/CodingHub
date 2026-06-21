<template>
  <div class="my-posts-page">
    <GeneralizedSidebar :items="sidebarItems" />
    <div class="main-content">
      <div class="page-header">
        <h1>我的帖子</h1>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">暂无帖子</div>
      <div v-else class="post-list">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :deletable="true"
          @click="goToDetail(post.id)"
          @delete="handlePostDelete"
        />
      </div>

      <ConfirmDialog
        :visible="dialogVisible"
        title="删除帖子"
        description="确定要删除这篇帖子吗？此操作不可撤销。"
        confirm-text="确认删除"
        cancel-text="取消"
        :danger="true"
        :loading="deleting"
        @confirm="handleConfirmDelete"
        @cancel="handleDialogCancel"
        @update:visible="dialogVisible = $event"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { LayoutGrid, FileText, Bookmark } from '@lucide/vue';
import { ElMessage } from 'element-plus';
import { useForumStore } from '@/stores/forum';
import PostCard from '@/components/forum/PostCard.vue';
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const router = useRouter();
const forumStore = useForumStore();

const sidebarItems: SidebarNavItem[] = [
  { label: '帖子列表', icon: LayoutGrid, to: '/forum' },
  { label: '我的帖子', icon: FileText, to: '/forum/my-posts' },
  { label: '我的收藏', icon: Bookmark, to: '/forum/my-favorites' }
];

const { posts, loading } = storeToRefs(forumStore);

const dialogVisible = ref(false);
const deleteId = ref<number | null>(null);
const deleting = ref(false);

onMounted(async () => {
  await forumStore.fetchMyPosts();
});

const goToDetail = (postId: number) => {
  router.push(`/forum/posts/${postId}`);
};

const handlePostDelete = (postId: number) => {
  deleteId.value = postId;
  dialogVisible.value = true;
};

const handleDialogCancel = () => {
  dialogVisible.value = false;
  deleteId.value = null;
};

const handleConfirmDelete = async () => {
  if (deleteId.value === null) return;
  deleting.value = true;

  try {
    const result = await forumStore.deletePost(deleteId.value);
    if (result.success) {
      ElMessage.success('帖子已删除');
      dialogVisible.value = false;
      deleteId.value = null;
    } else {
      handleDeleteError(result.errorCode!);
    }
  } catch (e) {
    ElMessage.error('删除失败，请稍后重试');
  } finally {
    deleting.value = false;
  }
};

const handleDeleteError = (errorCode: string) => {
  switch (errorCode) {
    case 'AUTH':
      ElMessage.warning('请先登录');
      break;
    case 'FORBIDDEN':
      ElMessage.warning('您不是该帖子的作者，无权删除');
      dialogVisible.value = false;
      break;
    case 'NOT_FOUND':
      ElMessage.warning('帖子不存在或已被删除');
      dialogVisible.value = false;
      break;
    default:
      ElMessage.error('删除失败，请稍后重试');
      dialogVisible.value = false;
      break;
  }
};
</script>

<style scoped>
.my-posts-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px 80px;
  display: flex;
  gap: 32px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty, .loading {
  text-align: center;
  padding: 48px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 15px;
}
</style>
