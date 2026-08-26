package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.Plugin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {

    @Query("SELECT p FROM Plugin p WHERE p.status = 'NORMAL' " +
           "AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%) " +
           "ORDER BY p.createdAt DESC")
    Page<Plugin> findByFilters(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Plugin p WHERE p.status = 'NORMAL' " +
           "AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%) " +
           "ORDER BY p.score DESC")
    Page<Plugin> findByFiltersOrderByHot(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Plugin p WHERE p.id = :id AND p.status = 'NORMAL'")
    Optional<Plugin> findByIdAndStatusNormal(@Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Plugin> findAllByStatusOrderByCreatedAtDesc(Plugin.Status status);

    // ============ 计数原子更新（不触发 @PreUpdate，避免 updatedAt 被计数操作刷新） ============
    // 热度权重：score = view×1 + like×3 + comment×5

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.viewCount = COALESCE(p.viewCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 1 " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.likeCount = COALESCE(p.likeCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 3 " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.likeCount = CASE WHEN COALESCE(p.likeCount, 0) > 0 THEN p.likeCount - 1 ELSE 0 END, " +
           "p.score = CASE WHEN COALESCE(p.score, 0) >= 3 THEN p.score - 3 ELSE p.score END " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.commentCount = COALESCE(p.commentCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 5 " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.commentCount = CASE WHEN COALESCE(p.commentCount, 0) > 0 THEN p.commentCount - 1 ELSE 0 END, " +
           "p.score = CASE WHEN COALESCE(p.score, 0) >= 5 THEN p.score - 5 ELSE p.score END " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int decrementCommentCount(@Param("id") Long id);
}
