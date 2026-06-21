import { ref } from 'vue'
import { interactionApi, type TargetType, type CommentResponse } from '@/services/interaction'
import type { InteractionPageResponse } from '@/services/interaction'

export function useInteraction(targetType: TargetType, targetId: number) {
  // Like state
  const liked = ref(false)
  const likeCount = ref(0)
  const likeLoading = ref(false)

  // Favorite state
  const favorited = ref(false)
  const favoriteLoading = ref(false)

  // Comments state
  const comments = ref<CommentResponse[]>([])
  const commentsTotalElements = ref(0)
  const commentsTotalPages = ref(0)
  const commentsPage = ref(0)
  const commentsLoading = ref(false)
  const commentSubmitting = ref(false)

  const loadLikeStatus = async () => {
    try {
      const data = await interactionApi.getLikeStatus(targetType, targetId)
      liked.value = data.liked
      likeCount.value = data.likeCount
    } catch (e) {
      console.error('Failed to load like status:', e)
    }
  }

  const toggleLike = async () => {
    if (likeLoading.value) return
    likeLoading.value = true
    try {
      const data = await interactionApi.toggleLike(targetType, targetId)
      liked.value = data.liked
      likeCount.value = data.likeCount
    } finally {
      likeLoading.value = false
    }
  }

  const loadFavoriteStatus = async () => {
    try {
      const data = await interactionApi.getFavoriteStatus(targetType, targetId)
      favorited.value = data.favorited
    } catch (e) {
      // Not logged in or other error — silently ignore
    }
  }

  const toggleFavorite = async () => {
    if (favoriteLoading.value) return
    favoriteLoading.value = true
    try {
      const data = await interactionApi.toggleFavorite(targetType, targetId)
      favorited.value = data.favorited
    } finally {
      favoriteLoading.value = false
    }
  }

  const loadComments = async (page: number = 0, size: number = 20) => {
    commentsLoading.value = true
    try {
      const data: InteractionPageResponse<CommentResponse> = await interactionApi.getComments(targetType, targetId, page, size)
      comments.value = data.content
      commentsTotalElements.value = data.totalElements
      commentsTotalPages.value = data.totalPages
      commentsPage.value = data.page
    } finally {
      commentsLoading.value = false
    }
  }

  const addComment = async (content: string, parentId?: number, userName?: string) => {
    commentSubmitting.value = true
    try {
      const newComment = await interactionApi.addComment(targetType, targetId, content, parentId, userName)
      // Reload comments to show the new one
      await loadComments(commentsPage.value)
      return newComment
    } finally {
      commentSubmitting.value = false
    }
  }

  const deleteComment = async (commentId: number) => {
    await interactionApi.deleteComment(commentId)
    await loadComments(commentsPage.value)
  }

  return {
    // Like
    liked,
    likeCount,
    likeLoading,
    loadLikeStatus,
    toggleLike,
    // Favorite
    favorited,
    favoriteLoading,
    loadFavoriteStatus,
    toggleFavorite,
    // Comments
    comments,
    commentsTotalElements,
    commentsTotalPages,
    commentsPage,
    commentsLoading,
    commentSubmitting,
    loadComments,
    addComment,
    deleteComment,
  }
}
