package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Deprecated
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<ForumComment> findByRootId(Long rootId);

    List<ForumComment> findByParentId(Long parentId);

    long countByPostId(Long postId);
}