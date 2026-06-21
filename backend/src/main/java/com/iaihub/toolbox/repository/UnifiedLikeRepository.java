package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.UnifiedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnifiedLikeRepository extends JpaRepository<UnifiedLike, Long> {

    // Logged-in user queries
    boolean existsByTargetTypeAndTargetIdAndUserId(String targetType, Long targetId, Long userId);

    Optional<UnifiedLike> findByTargetTypeAndTargetIdAndUserId(String targetType, Long targetId, Long userId);

    void deleteByTargetTypeAndTargetIdAndUserId(String targetType, Long targetId, Long userId);

    // Anonymous user queries
    boolean existsByTargetTypeAndTargetIdAndIpHash(String targetType, Long targetId, String ipHash);

    Optional<UnifiedLike> findByTargetTypeAndTargetIdAndIpHash(String targetType, Long targetId, String ipHash);

    void deleteByTargetTypeAndTargetIdAndIpHash(String targetType, Long targetId, String ipHash);
}
