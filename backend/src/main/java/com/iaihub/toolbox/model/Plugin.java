package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CodeBuddy 插件实体（独立插件市场）。
 *
 * <p>设计决策见 docs/adr/0002-independent-plugin-entity-and-page.md：
 * 独立实体 + 独立市场页，不寄生工具分类。source 为 GitHub owner/repo 或绝对 URL
 * （HTTP 市场 source 不能是相对路径）；zip 原件持久化，组件摘要在上传时生成并落库。</p>
 *
 * <p>点赞/评论/收藏复用统一互动模块（TargetType.PLUGIN），互动计数由
 * UnifiedLikeService / UnifiedCommentService / UnifiedFavoriteService 驱动。</p>
 */
@Entity
@Table(name = "plugin", indexes = {
    @Index(name = "idx_plugin_status_score", columnList = "status, score"),
    @Index(name = "idx_plugin_author", columnList = "author_id, status"),
    @Index(name = "idx_plugin_name_status", columnList = "name, status"),
    @Index(name = "idx_plugin_version", columnList = "version")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_plugin_name", columnNames = {"name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plugin {

    public enum Status {
        /** 正常发布（已完成 zip 补全，可被市场拉取）。 */
        NORMAL,
        /** 草稿：已保存元数据但尚未上传 zip，不进入市场、不参与列表展示。 */
        DRAFT,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 50)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    /** 市场引用：GitHub owner/repo 或绝对 URL（HTTP 市场 source 不能为相对路径）。 */
    @Column(nullable = false, length = 512)
    private String source;

    /** 持久化 zip 文件相对 baseDir 的路径，如 plugins/12/source.zip。 */
    @Column(name = "source_zip_path", length = 512)
    private String sourceZipPath;

    /** 上传时解析出的 .codebuddy-plugin/plugin.json 原始内容（JSON 字符串）。 */
    @Column(name = "plugin_json", columnDefinition = "text")
    private String pluginJson;

    /** 组件摘要 JSON（skills/agents/commands/hooks/.mcp.json/.lsp.json/bin/settings.json）。 */
    @Column(columnDefinition = "text")
    private String components;

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

    @Column(name = "favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    /** 管理员置顶标记（hot 排序优先；存量行可能为 null，读取用 Boolean.TRUE.equals 兜底） */
    @Column(name = "pinned")
    @Builder.Default
    private Boolean pinned = false;

    // 综合热度分：score = viewCount×1 + likeCount×3 + favoriteCount×4 + commentCount×5（对齐工具广场，插件无下载量）
    public void updateScore() {
        int view = this.viewCount != null ? this.viewCount : 0;
        int like = this.likeCount != null ? this.likeCount : 0;
        int favorite = this.favoriteCount != null ? this.favoriteCount : 0;
        int comment = this.commentCount != null ? this.commentCount : 0;
        this.score = BigDecimal.valueOf(view)
            .add(BigDecimal.valueOf(like).multiply(BigDecimal.valueOf(3)))
            .add(BigDecimal.valueOf(favorite).multiply(BigDecimal.valueOf(4)))
            .add(BigDecimal.valueOf(comment).multiply(BigDecimal.valueOf(5)));
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
        updateScore();
    }

    public void incrementLikeCount() {
        this.likeCount = (this.likeCount == null ? 0 : this.likeCount) + 1;
        updateScore();
    }

    public void decrementLikeCount() {
        this.likeCount = this.likeCount == null ? 0 : this.likeCount;
        if (this.likeCount > 0) this.likeCount--;
        updateScore();
    }

    public void incrementFavoriteCount() {
        this.favoriteCount = (this.favoriteCount == null ? 0 : this.favoriteCount) + 1;
        updateScore();
    }

    public void decrementFavoriteCount() {
        this.favoriteCount = this.favoriteCount == null ? 0 : this.favoriteCount;
        if (this.favoriteCount > 0) this.favoriteCount--;
        updateScore();
    }

    public void incrementCommentCount() {
        this.commentCount = (this.commentCount == null ? 0 : this.commentCount) + 1;
        updateScore();
    }

    public void decrementCommentCount() {
        this.commentCount = this.commentCount == null ? 0 : this.commentCount;
        if (this.commentCount > 0) this.commentCount--;
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
