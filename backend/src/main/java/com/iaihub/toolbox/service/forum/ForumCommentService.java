package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumCommentDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.ForumComment;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.forum.ForumCommentRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumCommentService {

    private final ForumCommentRepository commentRepository;
    private final ForumPostRepository postRepository;
    private final UserRepository userRepository;

    public List<ForumCommentDTO> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public ForumCommentDTO createComment(Long postId, Long authorId, String authorName, String content) {
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setContent(content);

        comment = commentRepository.save(comment);

        postRepository.findById(postId).ifPresent(p -> {
            p.setCommentCount(p.getCommentCount() + 1);
            postRepository.save(p);
        });

        return toDTO(comment);
    }

    @Transactional
    public ForumCommentDTO createReply(Long postId, Long authorId, String authorName, String content, Long parentId) {
        ForumComment parent = commentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + parentId));

        ForumComment reply = new ForumComment();
        reply.setPostId(postId);
        reply.setAuthorId(authorId);
        reply.setAuthorName(authorName);
        reply.setContent(content);
        reply.setParentId(parentId);
        reply.setRootId(parent.getRootId() != null ? parent.getRootId() : parentId);

        reply = commentRepository.save(reply);

        return toDTO(reply);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ForumComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + commentId));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权删除此评论");
        }

        commentRepository.delete(comment);

        postRepository.findById(comment.getPostId()).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            postRepository.save(p);
        });
    }

    private ForumCommentDTO toDTO(ForumComment comment) {
        String authorNickname = null;
        if (comment.getAuthorId() != null) {
            authorNickname = userRepository.findById(comment.getAuthorId())
                .map(u -> u.getNickname())
                .orElse(null);
        }
        return new ForumCommentDTO(
            comment.getId(), comment.getPostId(),
            comment.getAuthorId(), comment.getAuthorName(),
            authorNickname,
            comment.getParentId(), comment.getRootId(),
            comment.getContent(), comment.getLikeCount(),
            comment.getCreatedAt()
        );
    }
}