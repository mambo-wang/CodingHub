package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.service.forum.ForumPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.iaihub.toolbox.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostController {

    private final ForumPostService postService;

    @GetMapping
    public ResponseEntity<Page<ForumPostDTO>> getPostList(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hot") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ForumPostDTO> posts = postService.getPostList(category, keyword, sortBy, pageable);

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ForumPostDTO>> getMyPosts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ForumPostDTO> posts = postService.getMyPosts(user.getId(), pageable);

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumPostDTO> getPostById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ForumPostDTO post = postService.getPostById(id, user);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<ForumPostDTO> createPost(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ForumPostCreateRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Long authorId = user.getId();
        ForumPostDTO created = postService.createPost(authorId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForumPostDTO> updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody @Valid ForumPostCreateRequest request) {

        ForumPostDTO updated = postService.updatePost(id, user, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        postService.deletePost(id, user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> pinPost(@PathVariable Long id) {
        postService.pinPost(id);
        return ResponseEntity.ok(ApiResponse.success("置顶成功", null));
    }

    @DeleteMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpinPost(@PathVariable Long id) {
        postService.unpinPost(id);
        return ResponseEntity.ok(ApiResponse.success("取消置顶成功", null));
    }

    @GetMapping("/hot-top5")
    public ResponseEntity<ApiResponse<List<Long>>> getHotTop5() {
        List<Long> top5 = postService.getHotTop5();
        return ResponseEntity.ok(ApiResponse.success(top5));
    }
}