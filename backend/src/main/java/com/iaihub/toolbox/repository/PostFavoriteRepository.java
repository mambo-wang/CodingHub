package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {

    Optional<PostFavorite> findByUserIdAndPostId(Long userId, Long postId);

    List<PostFavorite> findByUserId(Long userId);

    void deleteByUserIdAndPostId(Long userId, Long postId);
}
