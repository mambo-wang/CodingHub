-- =====================================================
-- unify-interactions: Database Migration Script
-- Creates 3 unified tables, migrates data from 10 old tables,
-- then renames old tables to *_deprecated
-- =====================================================

-- Step 1: Create unified tables
-- -------------------------------------------------

-- unified_like
CREATE TABLE IF NOT EXISTS unified_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL COMMENT 'TOOL / FORUM_POST / VIDEO',
    target_id BIGINT NOT NULL,
    user_id BIGINT NULL COMMENT '登录用户ID，匿名时为NULL',
    ip_hash VARCHAR(64) NULL COMMENT 'SHA256(IP)，登录时为NULL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_like_user (target_type, target_id, user_id),
    UNIQUE KEY uk_like_anon (target_type, target_id, ip_hash),
    INDEX idx_like_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- unified_comment
CREATE TABLE IF NOT EXISTS unified_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL COMMENT 'TOOL / FORUM_POST / VIDEO',
    target_id BIGINT NOT NULL,
    user_id BIGINT NULL COMMENT '登录用户ID，匿名时为NULL',
    user_name VARCHAR(50) NULL COMMENT '匿名用户显示名',
    parent_id BIGINT NULL COMMENT '父评论ID，顶层为NULL',
    root_id BIGINT NULL COMMENT '根评论ID，顶层为NULL',
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comment_target (target_type, target_id, created_at),
    INDEX idx_comment_root (root_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- unified_favorite
CREATE TABLE IF NOT EXISTS unified_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL COMMENT 'TOOL / FORUM_POST / VIDEO',
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '收藏必须登录',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fav (user_id, target_type, target_id),
    INDEX idx_fav_user (user_id, target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Migrate data into unified tables
-- -------------------------------------------------

-- 2.1 Migrate tool_like -> unified_like (target_type=TOOL)
INSERT INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'TOOL', tool_id, user_id, NULL, created_at
FROM tool_like
ON DUPLICATE KEY UPDATE id = id;

-- 2.2 Migrate forum_like (post likes only, comment_id IS NULL) -> unified_like (target_type=FORUM_POST)
INSERT INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'FORUM_POST', post_id, user_id, ip_hash, created_at
FROM forum_like
WHERE post_id IS NOT NULL AND comment_id IS NULL
ON DUPLICATE KEY UPDATE id = id;

-- 2.3 Migrate video_like -> unified_like (target_type=VIDEO)
INSERT INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'VIDEO', video_id, user_id, NULL, created_at
FROM video_like
ON DUPLICATE KEY UPDATE id = id;

-- 2.4 Migrate tool_comment -> unified_comment (target_type=TOOL, flat - no parent/root)
INSERT INTO unified_comment (target_type, target_id, user_id, user_name, parent_id, root_id, content, like_count, created_at, updated_at)
SELECT 'TOOL', tool_id, user_id, NULL, NULL, NULL, content, 0, created_at, created_at
FROM tool_comment
ON DUPLICATE KEY UPDATE id = id;

-- 2.5 Migrate forum_comment -> unified_comment (target_type=FORUM_POST, preserve parent/root)
INSERT INTO unified_comment (target_type, target_id, user_id, user_name, parent_id, root_id, content, like_count, created_at, updated_at)
SELECT 'FORUM_POST', post_id, author_id, author_name, parent_id, root_id, content, like_count, created_at, updated_at
FROM forum_comment
ON DUPLICATE KEY UPDATE id = id;

-- 2.6 Migrate video_comment -> unified_comment (target_type=VIDEO, flat)
INSERT INTO unified_comment (target_type, target_id, user_id, user_name, parent_id, root_id, content, like_count, created_at, updated_at)
SELECT 'VIDEO', video_id, user_id, NULL, NULL, NULL, content, 0, created_at, created_at
FROM video_comment
ON DUPLICATE KEY UPDATE id = id;

-- 2.7 Migrate post_favorites -> unified_favorite (target_type=FORUM_POST)
INSERT INTO unified_favorite (target_type, target_id, user_id, created_at)
SELECT 'FORUM_POST', post_id, user_id, created_at
FROM post_favorites
ON DUPLICATE KEY UPDATE id = id;

-- 2.8 Migrate video_favorite -> unified_favorite (target_type=VIDEO)
INSERT INTO unified_favorite (target_type, target_id, user_id, created_at)
SELECT 'VIDEO', video_id, user_id, created_at
FROM video_favorite
ON DUPLICATE KEY UPDATE id = id;

-- Step 3: Rename old tables to *_deprecated
-- -------------------------------------------------
RENAME TABLE tool_like TO tool_like_deprecated;
RENAME TABLE tool_comment TO tool_comment_deprecated;
RENAME TABLE forum_like TO forum_like_deprecated;
RENAME TABLE forum_comment TO forum_comment_deprecated;
RENAME TABLE post_favorites TO post_favorites_deprecated;
RENAME TABLE video_like TO video_like_deprecated;
RENAME TABLE video_comment TO video_comment_deprecated;
RENAME TABLE video_favorite TO video_favorite_deprecated;

-- Step 4: Verification queries (run manually to check)
-- -------------------------------------------------
-- SELECT 'unified_like' AS tbl, COUNT(*) AS cnt FROM unified_like
-- UNION ALL
-- SELECT 'unified_comment', COUNT(*) FROM unified_comment
-- UNION ALL
-- SELECT 'unified_favorite', COUNT(*) FROM unified_favorite;
