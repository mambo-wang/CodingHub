package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumTagDTO;
import com.iaihub.toolbox.service.forum.ForumTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.iaihub.toolbox.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/tags")
@RequiredArgsConstructor
public class ForumTagController {

    private final ForumTagService tagService;

    @GetMapping
    public ResponseEntity<List<ForumTagDTO>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @GetMapping("/hot")
    public ResponseEntity<List<ForumTagDTO>> getHotTags() {
        return ResponseEntity.ok(tagService.getHotTags());
    }

    @PostMapping
    public ResponseEntity<ForumTagDTO> createTag(
            @AuthenticationPrincipal User user,
            @RequestBody TagCreateRequest request) {

        ForumTagDTO created = tagService.createTag(request.name(), request.isSystem() != null ? request.isSystem() : false);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public record TagCreateRequest(String name, Boolean isSystem) {}
}