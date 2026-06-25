package com.iaihub.toolbox.controller.notification;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.notification.NotificationDTO;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDTO> result = notificationService.getNotifications(
                currentUser.getId(), PageRequest.of(page, size));
        PageResponse<NotificationDTO> response = PageResponse.<NotificationDTO>builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success(notificationService.getUnreadCount(currentUser.getId())));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAsRead(id, currentUser.getId());
        return ApiResponse.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ApiResponse.success("已全部标记为已读", null);
    }
}
