package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.forum.*;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
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
    private final TagRepository tagRepository;

    public Page<ForumPostDTO> getPostList(Long categoryId, Long tagId, String keyword, String sortBy, Pageable pageable) {
        Page<ForumPost> posts;
        ForumPostVisibility visibility = ForumPostVisibility.PUBLIC;

        boolean latest = "latest".equalsIgnoreCase(sortBy);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (tagId != null) {
            if (latest) {
                posts = hasKeyword
                    ? postRepository.searchByTagIdAndTitle(tagId, keyword, ForumPostStatus.NORMAL, visibility, pageable)
                    : postRepository.findByTagIdAndStatusAndVisibilityOrderByCreatedAtDesc(tagId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = hasKeyword
                    ? postRepository.searchByTagIdAndTitleOrderByHot(tagId, keyword, ForumPostStatus.NORMAL, visibility, pageable)
                    : postRepository.findByTagIdAndStatusAndVisibilityOrderByHot(tagId, ForumPostStatus.NORMAL, visibility, pageable);
            }
        } else if (latest) {
            if (hasKeyword) {
                posts = postRepository.searchByTitle(keyword, ForumPostStatus.NORMAL, visibility, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatusAndVisibility(categoryId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = postRepository.findByStatusAndVisibilityOrderByCreatedAtDesc(ForumPostStatus.NORMAL, visibility, pageable);
            }
        } else {
            if (hasKeyword) {
                posts = postRepository.searchByTitleOrderByHot(keyword, ForumPostStatus.NORMAL, visibility, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatusAndVisibilityOrderByHot(categoryId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = postRepository.findByStatusAndVisibilityOrderByHot(ForumPostStatus.NORMAL, visibility, pageable);
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
        Page<ForumPost> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, ForumPostStatus.NORMAL, pageable);
        return posts.map(this::toDTO);
    }

    public ForumPostDTO getPostById(Long id, User currentUser) {
        ForumPost post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));

        // Private posts: only author and admin can view
        if (post.getVisibility() == ForumPostVisibility.PRIVATE) {
            if (currentUser == null) {
                throw new ForbiddenException("该帖子为私有帖子，请登录后查看");
            }
            boolean isOwner = post.getAuthorId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
            if (!isOwner && !isAdmin) {
                throw new ForbiddenException("该帖子为私有帖子，无权查看");
            }
        }

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

        // Set visibility from request, default to PUBLIC
        if (request.visibility() != null && !request.visibility().isBlank()) {
            post.setVisibility(ForumPostVisibility.valueOf(request.visibility()));
        } else {
            post.setVisibility(ForumPostVisibility.PUBLIC);
        }

        post = postRepository.save(post);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            for (Long tagId : request.tagIds()) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(post.getId());
                pt.setTagId(tagId);
                postTagRepository.save(pt);
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
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

        // Update visibility if provided
        if (request.visibility() != null && !request.visibility().isBlank()) {
            post.setVisibility(ForumPostVisibility.valueOf(request.visibility()));
        }

        // Handle tag replacement
        if (request.tagIds() != null) {
            // Remove old tag associations and decrement usage
            List<ForumPostTag> oldTags = postTagRepository.findByPostId(postId);
            for (ForumPostTag pt : oldTags) {
                tagRepository.findById(pt.getTagId()).ifPresent(Tag::decrementUsage);
            }
            postTagRepository.deleteByPostId(postId);

            // Add new tag associations and increment usage
            for (Long tagId : request.tagIds()) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(postId);
                pt.setTagId(tagId);
                postTagRepository.save(pt);
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

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

        List<TagDTO> tags = postTagRepository.findByPostId(post.getId()).stream()
            .map(pt -> tagRepository.findById(pt.getTagId()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
            .toList();

        return new ForumPostDTO(
            post.getId(), post.getTitle(), post.getContent(),
            post.getAuthorId(), authorName,
            authorNickname,
            post.getCategoryId(), categoryName,
            post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
            post.getCreatedAt(), post.getUpdatedAt(),
            post.getScore() != null ? post.getScore() : java.math.BigDecimal.ZERO,
            post.getPinned() != null ? post.getPinned() : false,
            post.getVisibility() != null ? post.getVisibility().name() : "PUBLIC",
            tags
        );
    }
}