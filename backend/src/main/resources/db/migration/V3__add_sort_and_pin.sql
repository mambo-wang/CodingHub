-- V3__add_sort_and_pin.sql
-- 排序与置顶功能：给 tool、forum_post、video 表添加 pinned 和 score 字段

-- 1. tool 表：添加 pinned 字段
ALTER TABLE tool
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE AFTER score;
CREATE INDEX idx_tool_pinned ON tool (pinned DESC);

-- 2. forum_post 表：添加 pinned 字段
ALTER TABLE forum_post
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE AFTER score;
CREATE INDEX idx_forum_post_pinned ON forum_post (pinned DESC);

-- 3. video 表：添加 score 和 pinned 字段
ALTER TABLE video
    ADD COLUMN score DECIMAL(10,2) NOT NULL DEFAULT 0
        AFTER comment_count,
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE AFTER score;
CREATE INDEX idx_video_score ON video (score DESC);
CREATE INDEX idx_video_pinned ON video (pinned DESC);
