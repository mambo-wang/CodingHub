package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.ForumLike;
import com.iaihub.toolbox.repository.forum.ForumCommentRepository;
import com.iaihub.toolbox.repository.forum.ForumLikeRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumLikeService {

    private final ForumLikeRepository likeRepository;
    private final ForumPostRepository postRepository;
    private final ForumCommentRepository commentRepository;

    @Transactional
    public void likePost(Long postId, Long userId, String ipHash) {
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        if (userId != null && likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new BusinessException("已点赞");
        }
        if (ipHash != null && likeRepository.existsByIpHashAndPostId(ipHash, postId)) {
            throw new BusinessException("已点赞");
        }

        ForumLike like = new ForumLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setIpHash(ipHash);
        likeRepository.save(like);

        postRepository.findById(postId).ifPresent(p -> {
            p.setLikeCount(p.getLikeCount() + 1);
            postRepository.save(p);
        });
    }

    @Transactional
    public void unlikePost(Long postId, Long userId, String ipHash) {
        ForumLike like = null;

        if (userId != null) {
            like = likeRepository.findByUserIdAndPostId(userId, postId).orElse(null);
        }
        if (like == null && ipHash != null) {
            like = likeRepository.findByIpHashAndPostId(ipHash, postId).orElse(null);
        }

        if (like != null) {
            likeRepository.delete(like);

            postRepository.findById(postId).ifPresent(p -> {
                p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
                postRepository.save(p);
            });
        }
    }

    @Transactional
    public void likeComment(Long commentId, Long userId, String ipHash) {
        commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + commentId));

        if (userId != null && likeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new BusinessException("已点赞");
        }
        if (ipHash != null && likeRepository.existsByIpHashAndCommentId(ipHash, commentId)) {
            throw new BusinessException("已点赞");
        }

        ForumLike like = new ForumLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setIpHash(ipHash);
        likeRepository.save(like);

        commentRepository.findById(commentId).ifPresent(c -> {
            c.setLikeCount(c.getLikeCount() + 1);
            commentRepository.save(c);
        });
    }
}