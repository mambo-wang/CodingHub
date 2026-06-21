package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.UnifiedFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnifiedFavoriteRepository extends JpaRepository<UnifiedFavorite, Long> {

    Optional<UnifiedFavorite> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    Page<UnifiedFavorite> findByUserIdAndTargetTypeOrderByCreatedAtDesc(
            Long userId, String targetType, Pageable pageable);
}
