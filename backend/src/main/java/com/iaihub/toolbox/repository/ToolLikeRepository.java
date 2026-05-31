package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToolLikeRepository extends JpaRepository<ToolLike, Long> {

    boolean existsByToolIdAndUserId(Long toolId, Long userId);

    Optional<ToolLike> findByToolIdAndUserId(Long toolId, Long userId);

    void deleteByToolIdAndUserId(Long toolId, Long userId);
}