package com.iaihub.toolbox.service.notification;

import com.iaihub.toolbox.dto.notification.NotificationDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.notification.Notification;
import com.iaihub.toolbox.model.notification.NotificationType;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<NotificationDTO> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("通知不存在"));
        if (!n.getUser().getId().equals(userId)) {
            throw new ForbiddenException("无权操作此通知");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // --- Internal methods to create notifications ---

    @Transactional
    public void createCommentNotification(Long targetOwnerId, String targetType, Long targetId,
                                           Long actorId, String actorName, String preview) {
        if (targetOwnerId.equals(actorId)) return; // don't notify self
        User targetUser = userRepository.findById(targetOwnerId).orElse(null);
        if (targetUser == null) return;
        Notification n = Notification.builder()
                .user(targetUser)
                .type(NotificationType.COMMENT_REPLY)
                .targetType(targetType)
                .targetId(targetId)
                .message(actorName + " 评论了: " + truncate(preview, 80))
                .actorId(actorId)
                .actorName(actorName)
                .build();
        notificationRepository.save(n);
    }

    @Transactional
    public void createLikeNotification(Long targetOwnerId, String targetType, Long targetId,
                                       Long actorId, String actorName) {
        if (targetOwnerId.equals(actorId)) return;
        User targetUser = userRepository.findById(targetOwnerId).orElse(null);
        if (targetUser == null) return;
        Notification n = Notification.builder()
                .user(targetUser)
                .type(NotificationType.LIKE)
                .targetType(targetType)
                .targetId(targetId)
                .message(actorName + " 赞了你的内容")
                .actorId(actorId)
                .actorName(actorName)
                .build();
        notificationRepository.save(n);
    }

    @Transactional
    public void createAdminNotification(Long userId, NotificationType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        String msg = type == NotificationType.ADMIN_APPROVED
                ? "你的注册申请已通过" : "你的注册申请已被拒绝";
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .targetType("USER")
                .targetId(userId)
                .message(msg)
                .build();
        notificationRepository.save(n);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .message(n.getMessage())
                .actorId(n.getActorId())
                .actorName(n.getActorName())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
