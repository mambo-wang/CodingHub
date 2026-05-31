package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tool_like", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tool_like_tool_user", columnNames = {"tool_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}