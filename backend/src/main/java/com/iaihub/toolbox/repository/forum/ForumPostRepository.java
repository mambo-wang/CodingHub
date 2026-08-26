package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.forum.ForumPostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    // --- Public post list queries (filter by status + visibility) ---

    Page<ForumPost> findByStatusAndVisibilityOrderByCreatedAtDesc(
        ForumPostStatus status, ForumPostVisibility visibility, Pageable pageable);

    Page<ForumPost> findByCategoryIdAndStatusAndVisibility(
        Long categoryId, ForumPostStatus status, ForumPostVisibility visibility, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.status = :status AND p.visibility = :visibility AND p.title LIKE %:keyword%")
    Page<ForumPost> searchByTitle(@Param("keyword") String keyword,
                                   @Param("status") ForumPostStatus status,
                                   @Param("visibility") ForumPostVisibility visibility,
                                   Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.id IN (SELECT pt.postId FROM ForumPostTag pt WHERE pt.tagId = :tagId) AND p.status = :status AND p.visibility = :visibility ORDER BY p.createdAt DESC")
    Page<ForumPost> findByTagIdAndStatusAndVisibilityOrderByCreatedAtDesc(
        @Param("tagId") Long tagId,
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.id IN (SELECT pt.postId FROM ForumPostTag pt WHERE pt.tagId = :tagId) AND p.status = :status AND p.visibility = :visibility ORDER BY p.pinned DESC, p.score DESC")
    Page<ForumPost> findByTagIdAndStatusAndVisibilityOrderByHot(
        @Param("tagId") Long tagId,
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.id IN (SELECT pt.postId FROM ForumPostTag pt WHERE pt.tagId = :tagId) AND p.status = :status AND p.visibility = :visibility AND p.title LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<ForumPost> searchByTagIdAndTitle(
        @Param("tagId") Long tagId,
        @Param("keyword") String keyword,
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.id IN (SELECT pt.postId FROM ForumPostTag pt WHERE pt.tagId = :tagId) AND p.status = :status AND p.visibility = :visibility AND p.title LIKE %:keyword% ORDER BY p.pinned DESC, p.score DESC")
    Page<ForumPost> searchByTagIdAndTitleOrderByHot(
        @Param("tagId") Long tagId,
        @Param("keyword") String keyword,
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.status = :status AND p.visibility = :visibility ORDER BY p.pinned DESC, p.score DESC")
    Page<ForumPost> findByStatusAndVisibilityOrderByHot(
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.categoryId = :categoryId AND p.status = :status AND p.visibility = :visibility ORDER BY p.pinned DESC, p.score DESC")
    Page<ForumPost> findByCategoryIdAndStatusAndVisibilityOrderByHot(
        @Param("categoryId") Long categoryId,
        @Param("status") ForumPostStatus status,
        @Param("visibility") ForumPostVisibility visibility,
        Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.status = :status AND p.visibility = :visibility AND p.title LIKE %:keyword% ORDER BY p.pinned DESC, p.score DESC")
    Page<ForumPost> searchByTitleOrderByHot(@Param("keyword") String keyword,
                                             @Param("status") ForumPostStatus status,
                                             @Param("visibility") ForumPostVisibility visibility,
                                             Pageable pageable);

    // --- My posts (show all visibilities) ---

    Page<ForumPost> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, ForumPostStatus status, Pageable pageable);

    // Hot Top5 (only public posts)
    @Query("SELECT p.id FROM ForumPost p WHERE p.status = 'NORMAL' AND p.visibility = 'PUBLIC' ORDER BY p.score DESC")
    List<Long> findTop5ByStatusOrderByScoreDesc(Pageable pageable);

    // Pin/Unpin
    @Modifying
    @Transactional
    @Query("UPDATE ForumPost p SET p.pinned = true WHERE p.id = :id")
    int pinById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE ForumPost p SET p.pinned = false WHERE p.id = :id")
    int unpinById(@Param("id") Long id);

    // ============ 计数原子更新（不触发 @PreUpdate，避免 updatedAt 被计数操作刷新） ============
    // 热度权重：score = view×1 + like×3 + comment×5

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ForumPost p SET p.viewCount = COALESCE(p.viewCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 1 " +
           "WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ForumPost p SET p.likeCount = COALESCE(p.likeCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 3 " +
           "WHERE p.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ForumPost p SET p.likeCount = CASE WHEN COALESCE(p.likeCount, 0) > 0 THEN p.likeCount - 1 ELSE 0 END, " +
           "p.score = CASE WHEN COALESCE(p.score, 0) >= 3 THEN p.score - 3 ELSE p.score END " +
           "WHERE p.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ForumPost p SET p.commentCount = COALESCE(p.commentCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 5 " +
           "WHERE p.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ForumPost p SET p.commentCount = CASE WHEN COALESCE(p.commentCount, 0) > 0 THEN p.commentCount - 1 ELSE 0 END, " +
           "p.score = CASE WHEN COALESCE(p.score, 0) >= 5 THEN p.score - 5 ELSE p.score END " +
           "WHERE p.id = :id")
    int decrementCommentCount(@Param("id") Long id);
}
