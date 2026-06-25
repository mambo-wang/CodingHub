package com.iaihub.toolbox.model.video;

import com.iaihub.toolbox.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "danmaku", indexes = {
    @Index(name = "idx_danmaku_video", columnList = "video_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Danmaku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(name = "time_seconds", nullable = false)
    @Builder.Default
    private Double timeSeconds = 0.0;

    @Column(length = 10)
    @Builder.Default
    private String color = "#FFFFFF";

    @Column(name = "danmaku_type", length = 10)
    @Builder.Default
    private String danmakuType = "SCROLL"; // SCROLL, TOP, BOTTOM

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
