package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_post", indexes = {
    @Index(name = "idx_forum_post_author", columnList = "author_id"),
    @Index(name = "idx_forum_post_category", columnList = "category_id"),
    @Index(name = "idx_forum_post_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ForumPostStatus status = ForumPostStatus.NORMAL;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ForumPostVisibility visibility = ForumPostVisibility.PUBLIC;

    // 更新 score 的方法：score = viewCount * 1 + likeCount * 3 + commentCount * 5
    public void updateScore() {
        this.score = BigDecimal.valueOf(this.viewCount)
            .multiply(BigDecimal.valueOf(1))
            .add(BigDecimal.valueOf(this.likeCount).multiply(BigDecimal.valueOf(3)))
            .add(BigDecimal.valueOf(this.commentCount).multiply(BigDecimal.valueOf(5)));
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ForumPostStatus.NORMAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}