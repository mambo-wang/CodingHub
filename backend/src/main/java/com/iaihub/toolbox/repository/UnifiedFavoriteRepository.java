package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.UnifiedFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UnifiedFavoriteRepository extends JpaRepository<UnifiedFavorite, Long> {

    Optional<UnifiedFavorite> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    Page<UnifiedFavorite> findByUserIdAndTargetTypeOrderByCreatedAtDesc(
            Long userId, String targetType, Pageable pageable);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    @Query("SELECT f.targetId, COUNT(f) FROM UnifiedFavorite f " +
            "WHERE f.targetType = :targetType AND f.targetId IN :targetIds GROUP BY f.targetId")
    List<Object[]> countByTargetTypeAndTargetIdIn(@Param("targetType") String targetType,
                                                  @Param("targetIds") Collection<Long> targetIds);
}
