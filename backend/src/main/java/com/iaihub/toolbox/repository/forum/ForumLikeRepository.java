package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Deprecated
public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {

    Optional<ForumLike> findByUserIdAndPostId(Long userId, Long postId);

    Optional<ForumLike> findByIpHashAndPostId(String ipHash, Long postId);

    Optional<ForumLike> findByUserIdAndCommentId(Long userId, Long commentId);

    Optional<ForumLike> findByIpHashAndCommentId(String ipHash, Long commentId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    boolean existsByIpHashAndPostId(String ipHash, Long postId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByIpHashAndCommentId(String ipHash, Long commentId);
}