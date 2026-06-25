-- V7: Notification, Danmaku, User Bio

-- Notification table
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    actor_id BIGINT,
    actor_name VARCHAR(100),
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_read (is_read),
    FOREIGN KEY (user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Danmaku table
CREATE TABLE IF NOT EXISTS danmaku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    time_seconds DOUBLE NOT NULL DEFAULT 0,
    color VARCHAR(10) DEFAULT '#FFFFFF',
    danmaku_type VARCHAR(10) DEFAULT 'SCROLL',
    created_at DATETIME NOT NULL,
    INDEX idx_danmaku_video (video_id),
    FOREIGN KEY (video_id) REFERENCES video(id),
    FOREIGN KEY (user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add bio column to user table
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS bio VARCHAR(500) DEFAULT NULL;

-- ============================================================
-- Migrate old interaction data to unified tables
-- ============================================================

-- Migrate tool_like -> unified_like
INSERT IGNORE INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'TOOL', tool_id, user_id, NULL, created_at
FROM tool_like
WHERE NOT EXISTS (
    SELECT 1 FROM unified_like
    WHERE unified_like.target_type = 'TOOL'
    AND unified_like.target_id = tool_like.tool_id
    AND unified_like.user_id = tool_like.user_id
);

-- Migrate tool_comment -> unified_comment
INSERT IGNORE INTO unified_comment (target_type, target_id, user_id, content, parent_id, created_at)
SELECT 'TOOL', tool_id, user_id, content, NULL, created_at
FROM tool_comment
WHERE NOT EXISTS (
    SELECT 1 FROM unified_comment
    WHERE unified_comment.target_type = 'TOOL'
    AND unified_comment.target_id = tool_comment.tool_id
    AND unified_comment.id = tool_comment.id
);

-- Migrate forum_like -> unified_like
INSERT IGNORE INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'FORUM_POST', post_id, COALESCE(user_id, 0), ip_hash, created_at
FROM forum_like
WHERE post_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM unified_like
    WHERE unified_like.target_type = 'FORUM_POST'
    AND unified_like.target_id = forum_like.post_id
    AND unified_like.ip_hash = forum_like.ip_hash
);

-- Migrate forum_comment -> unified_comment
INSERT IGNORE INTO unified_comment (target_type, target_id, user_id, content, parent_id, created_at)
SELECT 'FORUM_POST', post_id, author_id, content, parent_id, created_at
FROM forum_comment
WHERE NOT EXISTS (
    SELECT 1 FROM unified_comment
    WHERE unified_comment.target_type = 'FORUM_POST'
    AND unified_comment.target_id = forum_comment.post_id
    AND unified_comment.id = forum_comment.id
);

-- Migrate video_like -> unified_like
INSERT IGNORE INTO unified_like (target_type, target_id, user_id, ip_hash, created_at)
SELECT 'VIDEO', video_id, user_id, NULL, created_at
FROM video_like
WHERE NOT EXISTS (
    SELECT 1 FROM unified_like
    WHERE unified_like.target_type = 'VIDEO'
    AND unified_like.target_id = video_like.video_id
    AND unified_like.user_id = video_like.user_id
);

-- Migrate video_comment -> unified_comment
INSERT IGNORE INTO unified_comment (target_type, target_id, user_id, content, parent_id, created_at)
SELECT 'VIDEO', video_id, user_id, content, NULL, created_at
FROM video_comment
WHERE NOT EXISTS (
    SELECT 1 FROM unified_comment
    WHERE unified_comment.target_type = 'VIDEO'
    AND unified_comment.target_id = video_comment.video_id
    AND unified_comment.id = video_comment.id
);

-- Migrate video_favorite -> unified_favorite
INSERT IGNORE INTO unified_favorite (target_type, target_id, user_id, created_at)
SELECT 'VIDEO', video_id, user_id, created_at
FROM video_favorite
WHERE NOT EXISTS (
    SELECT 1 FROM unified_favorite
    WHERE unified_favorite.target_type = 'VIDEO'
    AND unified_favorite.target_id = video_favorite.video_id
    AND unified_favorite.user_id = video_favorite.user_id
);

-- Migrate post_favorite -> unified_favorite
INSERT IGNORE INTO unified_favorite (target_type, target_id, user_id, created_at)
SELECT 'FORUM_POST', post_id, user_id, created_at
FROM post_favorite
WHERE NOT EXISTS (
    SELECT 1 FROM unified_favorite
    WHERE unified_favorite.target_type = 'FORUM_POST'
    AND unified_favorite.target_id = post_favorite.post_id
    AND unified_favorite.user_id = post_favorite.user_id
);
