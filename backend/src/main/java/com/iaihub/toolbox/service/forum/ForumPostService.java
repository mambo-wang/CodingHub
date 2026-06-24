package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.forum.*;
import com.iaihub.toolbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumPostService {

    private final ForumPostRepository postRepository;
    private final ForumCategoryRepository categoryRepository;
    private final ForumPostTagRepository postTagRepository;
    private final UserRepository userRepository;

    public Page<ForumPostDTO> getPostList(Long categoryId, String keyword, String sortBy, Pageable pageable) {
        Page<ForumPost> posts;

        if ("latest".equalsIgnoreCase(sortBy)) {
            // 最新排序：createdAt DESC，忽略 pinned
            if (keyword != null && !keyword.isBlank()) {
                posts = postRepository.searchByTitle(keyword, ForumPostStatus.NORMAL, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatus(categoryId, ForumPostStatus.NORMAL, pageable);
            } else {
                posts = postRepository.findByStatusOrderByCreatedAtDesc(ForumPostStatus.NORMAL, pageable);
            }
        } else {
            // 默认 hot：pinned DESC, score DESC
            if (keyword != null && !keyword.isBlank()) {
                posts = postRepository.searchByTitleOrderByHot(keyword, ForumPostStatus.NORMAL, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatusOrderByHot(categoryId, ForumPostStatus.NORMAL, pageable);
            } else {
                posts = postRepository.findByStatusOrderByHot(ForumPostStatus.NORMAL, pageable);
            }
        }

        return posts.map(this::toDTO);
    }

    public void pinPost(Long id) {
        postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));
        postRepository.pinById(id);
    }

    public void unpinPost(Long id) {
        postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));
        postRepository.unpinById(id);
    }

    public List<Long> getHotTop5() {
        return postRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
    }

    public Page<ForumPostDTO> getMyPosts(Long userId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByAuthorIdAndStatus(userId, ForumPostStatus.NORMAL, pageable);
        return posts.map(this::toDTO);
    }

    public ForumPostDTO getPostById(Long id) {
        ForumPost post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));

        post.setViewCount(post.getViewCount() + 1);
        post.updateScore();
        postRepository.save(post);

        return toDTO(post);
    }

    @Transactional
    public ForumPostDTO createPost(Long authorId, ForumPostCreateRequest request) {
        ForumPost post = new ForumPost();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setAuthorId(authorId);
        post.setCategoryId(request.categoryId());
        post.setStatus(ForumPostStatus.NORMAL);

        post = postRepository.save(post);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            for (Long tagId : request.tagIds()) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(post.getId());
                pt.setTagId(tagId);
                postTagRepository.save(pt);
            }
        }

        return toDTO(post);
    }

    @Transactional
    public ForumPostDTO updatePost(Long postId, User user, ForumPostCreateRequest request) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        boolean isOwner = post.getAuthorId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        post.setTitle(request.title());
        post.setContent(request.content());
        post.setCategoryId(request.categoryId());

        post = postRepository.save(post);
        return toDTO(post);
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        boolean isOwner = post.getAuthorId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        post.setStatus(ForumPostStatus.DELETED);
        postRepository.save(post);
    }

    private ForumPostDTO toDTO(ForumPost post) {
        String categoryName = categoryRepository.findById(post.getCategoryId())
            .map(ForumCategory::getName).orElse("未分类");

        String authorName = userRepository.findById(post.getAuthorId())
            .map(u -> u.getUsername())
            .orElse("用户" + post.getAuthorId());

        String authorNickname = userRepository.findById(post.getAuthorId())
            .map(u -> u.getNickname())
            .orElse(null);

        return new ForumPostDTO(
            post.getId(), post.getTitle(), post.getContent(),
            post.getAuthorId(), authorName,
            authorNickname,
            post.getCategoryId(), categoryName,
            post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
            post.getCreatedAt(), post.getUpdatedAt(),
            post.getScore() != null ? post.getScore() : java.math.BigDecimal.ZERO,
            post.getPinned() != null ? post.getPinned() : false
        );
    }
}