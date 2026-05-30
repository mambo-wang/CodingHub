package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.repository.forum.*;
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

    public Page<ForumPostDTO> getPostList(Long categoryId, String keyword, Pageable pageable) {
        Page<ForumPost> posts;

        if (keyword != null && !keyword.isBlank()) {
            posts = postRepository.searchByTitle(keyword, ForumPostStatus.NORMAL, pageable);
        } else if (categoryId != null) {
            posts = postRepository.findByCategoryIdAndStatus(categoryId, ForumPostStatus.NORMAL, pageable);
        } else {
            posts = postRepository.findByStatusOrderByCreatedAtDesc(ForumPostStatus.NORMAL, pageable);
        }

        return posts.map(this::toDTO);
    }

    public ForumPostDTO getPostById(Long id) {
        ForumPost post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));

        post.setViewCount(post.getViewCount() + 1);
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
    public ForumPostDTO updatePost(Long postId, Long userId, ForumPostCreateRequest request) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        if (!post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权修改此帖子");
        }

        post.setTitle(request.title());
        post.setContent(request.content());
        post.setCategoryId(request.categoryId());

        post = postRepository.save(post);
        return toDTO(post);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        if (!post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权删除此帖子");
        }

        post.setStatus(ForumPostStatus.DELETED);
        postRepository.save(post);
    }

    private ForumPostDTO toDTO(ForumPost post) {
        String categoryName = categoryRepository.findById(post.getCategoryId())
            .map(ForumCategory::getName).orElse("未分类");

        return new ForumPostDTO(
            post.getId(), post.getTitle(), post.getContent(),
            post.getAuthorId(), "用户" + post.getAuthorId(),
            post.getCategoryId(), categoryName,
            post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
            post.getCreatedAt(), post.getUpdatedAt()
        );
    }
}