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

    // 热度 Top5
    @Query("SELECT t.id FROM Tool t WHERE t.status = 'NORMAL' ORDER BY t.score DESC")
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
}
