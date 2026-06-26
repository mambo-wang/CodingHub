package com.iaihub.toolbox.controller.feedback;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.feedback.FeedbackCreateRequest;
import com.iaihub.toolbox.dto.feedback.FeedbackDTO;
import com.iaihub.toolbox.dto.feedback.FeedbackReplyRequest;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.feedback.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackDTO>>> getFeedbacks(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<FeedbackDTO> response = feedbackService.list(category, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackDTO>> createFeedback(
            @Valid @RequestBody FeedbackCreateRequest request,
            HttpServletRequest httpRequest) {
        FeedbackDTO feedback = feedbackService.submit(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("留言提交成功", feedback));
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackDTO>> replyFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackReplyRequest request,
            @AuthenticationPrincipal User currentUser) {
        FeedbackDTO feedback = feedbackService.reply(id, request.adminReply(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("回复成功", feedback));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
