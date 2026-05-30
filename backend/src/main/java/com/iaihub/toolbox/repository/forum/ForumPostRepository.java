package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    Page<ForumPost> findByStatusOrderByCreatedAtDesc(ForumPostStatus status, Pageable pageable);

    Page<ForumPost> findByCategoryIdAndStatus(Long categoryId, ForumPostStatus status, Pageable pageable);

    Page<ForumPost> findByAuthorIdAndStatus(Long authorId, ForumPostStatus status, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.status = :status AND p.title LIKE %:keyword%")
    Page<ForumPost> searchByTitle(@Param("keyword") String keyword,
                                   @Param("status") ForumPostStatus status,
                                   Pageable pageable);
}