package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT t FROM Tool t WHERE t.uploader.id = :uploaderId AND t.status = 'NORMAL' " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR t.name LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    Page<Tool> findByUploaderIdAndFilters(@Param("uploaderId") Long uploaderId,
                                           @Param("categoryId") Long categoryId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    boolean existsByNameAndUploaderIdAndStatus(String name, Long uploaderId, Tool.Status status);
}
