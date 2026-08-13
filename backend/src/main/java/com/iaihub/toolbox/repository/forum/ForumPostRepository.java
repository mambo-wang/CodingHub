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
}
