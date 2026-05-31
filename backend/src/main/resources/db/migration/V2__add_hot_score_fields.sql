-- Hot Rankings Feature: Add score fields and tool_like table
-- This migration adds hot ranking support to tool and forum_post tables

-- 1. Add score fields to tool table (if not exists)
ALTER TABLE tool ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN IF NOT EXISTS comment_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN IF NOT EXISTS score DECIMAL(10,2) DEFAULT 0;

-- 2. Add score field to forum_post table (if not exists)
ALTER TABLE forum_post ADD COLUMN IF NOT EXISTS score DECIMAL(10,2) DEFAULT 0;

-- 3. Create tool_like table for tool likes
CREATE TABLE IF NOT EXISTS tool_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tool_like_tool_user UNIQUE (tool_id, user_id),
    CONSTRAINT fk_tool_like_tool FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE,
    CONSTRAINT fk_tool_like_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- 4. Create indexes for score fields (for ranking queries)
CREATE INDEX IF NOT EXISTS idx_tool_score ON tool(score DESC);
CREATE INDEX IF NOT EXISTS idx_forum_post_score ON forum_post(score DESC);
CREATE INDEX IF NOT EXISTS idx_tool_like_tool_id ON tool_like(tool_id);
CREATE INDEX IF NOT EXISTS idx_tool_like_user_id ON tool_like(user_id);

-- 5. Initialize existing data score values
-- For tools: score = viewCount * 1 + likeCount * 3 + commentCount * 5
UPDATE tool SET score = (COALESCE(view_count, 0) * 1 + COALESCE(like_count, 0) * 3 + COALESCE(comment_count, 0) * 5) WHERE score = 0 OR score IS NULL;

-- For forum posts: score = viewCount * 1 + likeCount * 3 + commentCount * 5
UPDATE forum_post SET score = (COALESCE(view_count, 0) * 1 + COALESCE(like_count, 0) * 3 + COALESCE(comment_count, 0) * 5) WHERE score = 0 OR score IS NULL;