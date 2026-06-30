package com.iaihub.toolbox.model.video;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "video", indexes = {
    @Index(name = "idx_video_uploader", columnList = "uploader_id, status"),
    @Index(name = "idx_video_status_created", columnList = "status, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column
    @Builder.Default
    private Integer duration = 0;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VideoStatus status = VideoStatus.NORMAL;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "danmaku_enabled", nullable = false)
    @Builder.Default
    private Boolean danmakuEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 更新 score 的方法：score = viewCount * 1 + likeCount * 3 + commentCount * 5
    public void updateScore() {
        this.score = BigDecimal.valueOf(this.viewCount)
            .multiply(BigDecimal.valueOf(1))
            .add(BigDecimal.valueOf(this.likeCount).multiply(BigDecimal.valueOf(3)))
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = VideoStatus.NORMAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
