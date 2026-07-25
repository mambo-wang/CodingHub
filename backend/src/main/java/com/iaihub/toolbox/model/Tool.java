package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tool", indexes = {
    @Index(name = "idx_tool_category", columnList = "category_id, status"),
    @Index(name = "idx_tool_uploader", columnList = "uploader_id, status"),
    @Index(name = "idx_tool_name_status", columnList = "name, status"),
    @Index(name = "idx_tool_version", columnList = "version")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tool_uploader_name_category", columnNames = {"uploader_id", "name", "category_id", "status"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool {

    public enum Status {
        NORMAL,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 200)
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String version = "1.0.0";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NORMAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    // 综合热度分：score = viewCount×1 + downloadCount×2 + likeCount×3 + favoriteCount×4 + commentCount×5
    public void updateScore() {
        this.score = BigDecimal.valueOf(this.viewCount)
            .add(BigDecimal.valueOf(this.downloadCount).multiply(BigDecimal.valueOf(2)))
            .add(BigDecimal.valueOf(this.likeCount).multiply(BigDecimal.valueOf(3)))
            .add(BigDecimal.valueOf(this.favoriteCount).multiply(BigDecimal.valueOf(4)))
            .add(BigDecimal.valueOf(this.commentCount).multiply(BigDecimal.valueOf(5)));
    }

    public void incrementViewCount() {
        this.viewCount++;
        updateScore();
    }

    public void incrementLikeCount() {
        this.likeCount++;
        updateScore();
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
        updateScore();
    }

    public void incrementCommentCount() {
        this.commentCount++;
        updateScore();
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
        updateScore();
    }

    public void incrementFavoriteCount() {
        this.favoriteCount++;
        updateScore();
    }

    public void decrementFavoriteCount() {
        if (this.favoriteCount > 0) this.favoriteCount--;
        updateScore();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = Status.NORMAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
