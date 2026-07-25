package com.iaihub.toolbox.model.feedback;

import com.iaihub.toolbox.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_message", indexes = {
    @Index(name = "idx_feedback_status_created", columnList = "status, created_at DESC"),
    @Index(name = "idx_feedback_category_status", columnList = "category, status, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackMessage {

    public enum Status {
        NORMAL, DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 50)
    private String nickname;

    @Column(length = 100)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FeedbackCategory category = FeedbackCategory.SUGGESTION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NORMAL;

    @Column(name = "admin_reply", columnDefinition = "text")
    private String adminReply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by")
    private User repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = Status.NORMAL;
        }
        if (category == null) {
            category = FeedbackCategory.SUGGESTION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
