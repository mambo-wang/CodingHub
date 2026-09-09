-- Add content_format field to forum_post table
-- Markdown 与 HTML 两种正文格式，存量数据全部视为 Markdown（与迁移前的渲染行为一致）
ALTER TABLE forum_post ADD COLUMN content_format VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN';
