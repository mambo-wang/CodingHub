package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumCommentCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumCommentDTO;
import com.iaihub.toolbox.service.forum.ForumCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.iaihub.toolbox.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Deprecated
@RestController
@RequiredArgsConstructor
public class ForumCommentController {

    private final ForumCommentService commentService;

    @GetMapping("/api/forum/posts/{postId}/comments")
    public ResponseEntity<List<ForumCommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<ForumCommentDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/api/forum/posts/{postId}/comments")
    public ResponseEntity<ForumCommentDTO> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid ForumCommentCreateRequest request,
            @AuthenticationPrincipal User user) {

        Long authorId = null;
        String authorName = request.authorName();

        if (user != null) {
            authorId = user.getId();
            authorName = null;
        }

        ForumCommentDTO created;
        if (request.parentId() != null) {
            created = commentService.createReply(postId, authorId, authorName,
                request.content(), request.parentId());
        } else {
            created = commentService.createComment(postId, authorId, authorName, request.content());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/api/forum/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = user.getId();
        commentService.deleteComment(id, userId);

        return ResponseEntity.noContent().build();
    }
}