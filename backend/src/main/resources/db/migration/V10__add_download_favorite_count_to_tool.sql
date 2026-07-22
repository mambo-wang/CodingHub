-- V10: 优化热门计算 - 添加 download_count / favorite_count 到 tool 表并回填存量数据

-- 1. 新增列
ALTER TABLE tool ADD COLUMN download_count INT NOT NULL DEFAULT 0;
ALTER TABLE tool ADD COLUMN favorite_count INT NOT NULL DEFAULT 0;

-- 2. 回填 download_count（从 tool_file 聚合）
UPDATE tool t
    SET t.download_count = (
        SELECT COALESCE(SUM(tf.download_count), 0)
        FROM tool_file tf
        WHERE tf.tool_id = t.id
    );

-- 3. 回填 favorite_count（从 unified_favorite 聚合）
UPDATE tool t
    SET t.favorite_count = (
        SELECT COUNT(*)
        FROM unified_favorite uf
        WHERE uf.target_type = 'TOOL' AND uf.target_id = t.id
    );

-- 4. 用新公式重算 score = viewCount×1 + downloadCount×2 + likeCount×3 + favoriteCount×4 + commentCount×5
UPDATE tool
    SET score = (
        COALESCE(view_count, 0) * 1
        + COALESCE(download_count, 0) * 2
        + COALESCE(like_count, 0) * 3
        + COALESCE(favorite_count, 0) * 4
        + COALESCE(comment_count, 0) * 5
    );
