package com.iaihub.toolbox.service;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.repository.PostFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostFavoriteService {

    private final PostFavoriteRepository repository;

    public PostFavoriteService(PostFavoriteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PostFavorite addFavorite(Long userId, Long postId) {
        Optional<PostFavorite> existing = repository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            return existing.get();
        }
        PostFavorite favorite = PostFavorite.builder()
                .userId(userId)
                .postId(postId)
                .build();
        return repository.save(favorite);
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long postId) {
        Optional<PostFavorite> existing = repository.findByUserIdAndPostId(userId, postId);
        if (existing.isEmpty()) {
            return false;
        }
        repository.deleteByUserIdAndPostId(userId, postId);
        return true;
    }

    public List<PostFavorite> getUserFavorites(Long userId) {
        return repository.findByUserId(userId);
    }

    public boolean isFavorited(Long userId, Long postId) {
        return repository.findByUserIdAndPostId(userId, postId).isPresent();
    }
}
