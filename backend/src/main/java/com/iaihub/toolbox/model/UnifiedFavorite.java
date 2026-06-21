package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "unified_favorite", uniqueConstraints = {
    @UniqueConstraint(name = "uk_fav", columnNames = {"user_id", "target_type", "target_id"})
}, indexes = {
    @Index(name = "idx_fav_user", columnList = "user_id, target_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
