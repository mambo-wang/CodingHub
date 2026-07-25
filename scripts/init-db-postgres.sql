-- CodingHub PostgreSQL 种子脚本（仅插入种子数据）
-- 说明：
--   Schema（所有表/列）由应用启动时 Hibernate(ddl-auto:update) 自动生成，
--   故本脚本只负责写入初始分类数据，不建表，避免与实体 Schema 不一致。
--   配合全局引号策略(globally_quoted_identifiers=true)，user 等保留字以双引号引用。
-- 用法：先启动应用完成建表，再执行：
--   PGPASSWORD=codinghub psql -U codinghub -h localhost -p 5432 -d ai_tool_square -f scripts/init-db-postgres.sql

-- 工具分类
-- 注意：category 实体无 updated_at 列（有 logo_url），故仅插入 name/icon/sort_order/created_at
INSERT INTO category (name, icon, sort_order, created_at) VALUES
('AI对话', 'message-square', 1, CURRENT_TIMESTAMP),
('AI绘画', 'palette', 2, CURRENT_TIMESTAMP),
('AI写作', 'pen-tool', 3, CURRENT_TIMESTAMP),
('AI编程', 'code', 4, CURRENT_TIMESTAMP),
('AI音频', 'music', 5, CURRENT_TIMESTAMP),
('AI视频', 'video', 6, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 论坛分类
-- 注意：forum_category 实体无 icon/updated_at 列，故仅插入 name/description/sort_order/created_at
INSERT INTO forum_category (name, description, sort_order, created_at) VALUES
('技术交流', '讨论 AI 工具与技术实现', 1, CURRENT_TIMESTAMP),
('资源分享', '分享优质 AI 资源与教程', 2, CURRENT_TIMESTAMP),
('问题求助', '遇到问题时来这里求助', 3, CURRENT_TIMESTAMP),
('经验心得', '分享你的使用经验与心得', 4, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
