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
           "ORDER BY p.pinned DESC, p.score DESC")
    Page<Plugin> findByFiltersOrderByHot(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Plugin p WHERE p.id = :id AND p.status = 'NORMAL'")
    Optional<Plugin> findByIdAndStatusNormal(@Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Plugin> findAllByStatusOrderByCreatedAtDesc(Plugin.Status status);

    List<Plugin> findByStatus(Plugin.Status status);

    // ============ 计数原子更新（不触发 @PreUpdate，避免 updatedAt 被计数操作刷新） ============
    // 热度权重：score = view×1 + like×3 + favorite×4 + comment×5

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.favoriteCount = COALESCE(p.favoriteCount, 0) + 1, " +
           "p.score = COALESCE(p.score, 0) + 4 " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int incrementFavoriteCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Plugin p SET p.favoriteCount = CASE WHEN COALESCE(p.favoriteCount, 0) > 0 THEN p.favoriteCount - 1 ELSE 0 END, " +
           "p.score = CASE WHEN COALESCE(p.score, 0) >= 4 THEN p.score - 4 ELSE p.score END " +
           "WHERE p.id = :id AND p.status = 'NORMAL'")
    int decrementFavoriteCount(@Param("id") Long id);

    // 置顶/取消置顶
    @Modifying
    @Transactional
    @Query("UPDATE Plugin p SET p.pinned = true WHERE p.id = :id")
    int pinById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Plugin p SET p.pinned = false WHERE p.id = :id")
    int unpinById(@Param("id") Long id);

    // 热度 Top5（仅返回有互动数据的插件，避免新插件被标记为热门）
    @Query("SELECT p.id FROM Plugin p WHERE p.status = 'NORMAL' AND p.score > 0 ORDER BY p.score DESC")
    List<Long> findTop5ByStatusOrderByScoreDesc(Pageable pageable);
}
