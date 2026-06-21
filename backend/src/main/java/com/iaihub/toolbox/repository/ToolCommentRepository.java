package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Deprecated
public interface ToolCommentRepository extends JpaRepository<ToolComment, Long> {
    List<ToolComment> findByToolIdOrderByCreatedAtDesc(Long toolId);
}