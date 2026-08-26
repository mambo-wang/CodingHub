package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.Tool;
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
public interface ToolRepository extends JpaRepository<Tool, Long> {

    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    Page<Tool> findByFilters(@Param("categoryId") Long categoryId,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.name ASC")
    Page<Tool> findByFiltersOrderByName(@Param("categoryId") Long categoryId,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    @Query("SELECT t FROM Tool t WHERE t.id = :id AND t.status = 'NORMAL'")
    Optional<Tool> findByIdAndStatusNormal(@Param("id") Long id);

    @Query("SELECT t FROM Tool t JOIN FETCH t.category JOIN FETCH t.uploader WHERE t.id = :id AND t.status = 'NORMAL'")
    Optional<Tool> findByIdAndStatusNormalWithRelations(@Param("id") Long id);

    @Query("SELECT t FROM Tool t WHERE t.uploader.id = :uploaderId AND t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    Page<Tool> findByUploaderIdAndFilters(@Param("uploaderId") Long uploaderId,
                                           @Param("categoryId") Long categoryId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    boolean existsByNameAndUploaderIdAndCategoryIdAndStatus(String name, Long uploaderId, Long categoryId, Tool.Status status);

    boolean existsByNameAndUploaderIdAndCategoryIdAndStatusAndIdNot(String name, Long uploaderId, Long categoryId, Tool.Status status, Long id);

    boolean existsByNameAndUploaderIdAndStatus(String name, Long uploaderId, Tool.Status status);

    @Query("SELECT t FROM Tool t JOIN FETCH t.category WHERE t.status = 'NORMAL' " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    List<Tool> findApprovedToolsWithCategory(@Param("keyword") String keyword, Pageable pageable);

    // MCP Server 所需方法
    List<Tool> findTop10ByStatusAndNameContainingIgnoreCase(Tool.Status status, String keyword);

    List<Tool> findTop10ByStatusOrderByCreatedAtDesc(Tool.Status status);

    long countByStatus(Tool.Status status);

    // 热度排序查询：pinned DESC, score DESC
    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.pinned DESC, t.score DESC")
    Page<Tool> findByFiltersOrderByHot(@Param("categoryId") Long categoryId,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    // 带标签筛选的查询：EXISTS 子查询关联 tool_tag 表
    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "AND EXISTS (SELECT 1 FROM ToolTag tt WHERE tt.toolId = t.id AND tt.tagId = :tagId) " +
           "ORDER BY t.createdAt DESC")
    Page<Tool> findByFiltersWithTag(@Param("categoryId") Long categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("tagId") Long tagId,
                                     Pageable pageable);

    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "AND EXISTS (SELECT 1 FROM ToolTag tt WHERE tt.toolId = t.id AND tt.tagId = :tagId) " +
           "ORDER BY t.name ASC")
    Page<Tool> findByFiltersWithTagOrderByName(@Param("categoryId") Long categoryId,
                                                @Param("keyword") String keyword,
                                                @Param("tagId") Long tagId,
                                                Pageable pageable);

    @Query("SELECT t FROM Tool t WHERE t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "AND EXISTS (SELECT 1 FROM ToolTag tt WHERE tt.toolId = t.id AND tt.tagId = :tagId) " +
           "ORDER BY t.pinned DESC, t.score DESC")
    Page<Tool> findByFiltersWithTagOrderByHot(@Param("categoryId") Long categoryId,
                                               @Param("keyword") String keyword,
                                               @Param("tagId") Long tagId,
                                               Pageable pageable);

    // 热度 Top5（仅返回有互动数据的工具，避免新工具被标记为热门）
    @Query("SELECT t.id FROM Tool t WHERE t.status = 'NORMAL' AND t.score > 0 ORDER BY t.score DESC")
    List<Long> findTop5ByStatusOrderByScoreDesc(Pageable pageable);

    // 置顶/取消置顶
    @Modifying
    @Transactional
    @Query("UPDATE Tool t SET t.pinned = true WHERE t.id = :id")
    int pinById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Tool t SET t.pinned = false WHERE t.id = :id")
    int unpinById(@Param("id") Long id);

    // ============ 计数原子更新（不触发 @PreUpdate，避免 updatedAt 被计数操作刷新） ============
    // 热度权重：score = view×1 + download×2 + like×3 + favorite×4 + comment×5

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.viewCount = COALESCE(t.viewCount, 0) + 1, " +
           "t.score = COALESCE(t.score, 0) + 1 " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.likeCount = COALESCE(t.likeCount, 0) + 1, " +
           "t.score = COALESCE(t.score, 0) + 3 " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.likeCount = CASE WHEN COALESCE(t.likeCount, 0) > 0 THEN t.likeCount - 1 ELSE 0 END, " +
           "t.score = CASE WHEN COALESCE(t.score, 0) >= 3 THEN t.score - 3 ELSE t.score END " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.commentCount = COALESCE(t.commentCount, 0) + 1, " +
           "t.score = COALESCE(t.score, 0) + 5 " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.commentCount = CASE WHEN COALESCE(t.commentCount, 0) > 0 THEN t.commentCount - 1 ELSE 0 END, " +
           "t.score = CASE WHEN COALESCE(t.score, 0) >= 5 THEN t.score - 5 ELSE t.score END " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int decrementCommentCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.downloadCount = COALESCE(t.downloadCount, 0) + 1, " +
           "t.score = COALESCE(t.score, 0) + 2 " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int incrementDownloadCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.favoriteCount = COALESCE(t.favoriteCount, 0) + 1, " +
           "t.score = COALESCE(t.score, 0) + 4 " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int incrementFavoriteCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Tool t SET t.favoriteCount = CASE WHEN COALESCE(t.favoriteCount, 0) > 0 THEN t.favoriteCount - 1 ELSE 0 END, " +
           "t.score = CASE WHEN COALESCE(t.score, 0) >= 4 THEN t.score - 4 ELSE t.score END " +
           "WHERE t.id = :id AND t.status = 'NORMAL'")
    int decrementFavoriteCount(@Param("id") Long id);
}
