-- V6: Add tool description, unified tag system, and tag associations

-- 1. Tool: add description field
ALTER TABLE tool ADD COLUMN description VARCHAR(200) DEFAULT NULL;

-- 2. Unified tag table
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    tag_type VARCHAR(20) NOT NULL COMMENT 'TOOL, FORUM, VIDEO',
    usage_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_type (name, tag_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tool-tag association
CREATE TABLE IF NOT EXISTS tool_tag (
    tool_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (tool_id, tag_id),
    CONSTRAINT fk_tool_tag_tool FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
    CONSTRAINT fk_tool_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Video-tag association
CREATE TABLE IF NOT EXISTS video_tag (
    video_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (video_id, tag_id),
    CONSTRAINT fk_video_tag_video FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Forum post-tag association (new, pointing to unified tag table)
-- Drop old foreign keys first, then recreate pointing to new tag table
ALTER TABLE forum_post_tag DROP FOREIGN KEY fk_forum_post_tag_tag;
ALTER TABLE forum_post_tag CHANGE tag_id tag_id BIGINT NOT NULL;
-- Add new FK pointing to unified tag table
ALTER TABLE forum_post_tag ADD CONSTRAINT fk_forum_post_tag_new_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE;
