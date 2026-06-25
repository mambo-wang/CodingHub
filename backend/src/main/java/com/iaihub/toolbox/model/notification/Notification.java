package com.iaihub.toolbox.model.notification;

import com.iaihub.toolbox.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_read", columnList = "is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 30)
    private String targetType; // TOOL, FORUM_POST, VIDEO

    @Column(nullable = false)
    private Long targetId;

    @Column(length = 500, nullable = false)
    private String message;

    @Column(name = "actor_id")
    private Long actorId; // who triggered this (e.g., commenter/liker)

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
