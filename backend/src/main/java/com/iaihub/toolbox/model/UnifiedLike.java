package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "unified_like", uniqueConstraints = {
    @UniqueConstraint(name = "uk_like_user", columnNames = {"target_type", "target_id", "user_id"}),
    @UniqueConstraint(name = "uk_like_anon", columnNames = {"target_type", "target_id", "ip_hash"})
}, indexes = {
    @Index(name = "idx_like_target", columnList = "target_type, target_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
