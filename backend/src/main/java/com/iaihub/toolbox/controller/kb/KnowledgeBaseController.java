package com.iaihub.toolbox.controller.kb;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.kb.*;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.kb.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    // ── Knowledge Base CRUD ──────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<KbResponse>>> listKnowledgeBases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(required = false) Long ownerId) {
        Page<KbResponse> result = knowledgeBaseService.listKnowledgeBases(page, size, sortBy, ownerId);
        PageResponse<KbResponse> pageResponse = PageResponse.<KbResponse>builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KbResponse>> getKnowledgeBase(@PathVariable Long id) {
        KbResponse response = knowledgeBaseService.getKnowledgeBase(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KbResponse>> createKnowledgeBase(
            @Valid @RequestBody KbCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        KbResponse response = knowledgeBaseService.createKnowledgeBase(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KbResponse>> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody KbUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        KbResponse response = knowledgeBaseService.updateKnowledgeBase(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        knowledgeBaseService.deleteKnowledgeBase(id, currentUser);
        return ResponseEntity.ok().build();
    }

    // ── Search (via Java proxy) ────────────────────────────────

    @PostMapping("/{id}/search")
    public ResponseEntity<ApiResponse<List<KbSearchResultResponse>>> search(
            @PathVariable Long id,
            @Valid @RequestBody KbSearchRequest request) {
        List<KbSearchResultResponse> results = knowledgeBaseService.search(id, request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
