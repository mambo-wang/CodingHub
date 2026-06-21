package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.UnifiedComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnifiedCommentRepository extends JpaRepository<UnifiedComment, Long> {

    Page<UnifiedComment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
            String targetType, Long targetId, Pageable pageable);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);
}
