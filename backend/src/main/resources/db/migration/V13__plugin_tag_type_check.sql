-- V13: 插件市场增强（收藏数 + 置顶 + 标签）相关 schema 修正
-- 注：本项目 Flyway 未启用（schema 主要由 Hibernate ddl-auto:update 生成）。
--     plugin.favorite_count / plugin.pinned 列与 plugin_tag 表由 Hibernate 自动创建；
--     本文件为存量环境须【手动执行】的迁移参考。
--
-- 背景：Java 枚举 TagType 含 PLUGIN，但存量 MySQL 库的 tag.tag_type 列可能停留在
--       enum('TOOL','FORUM','VIDEO')（早期手工/历史建表遗留，ddl-auto 不会给已存在的
--       ENUM 列补枚举值），导致无法写入 PLUGIN 标签——插件市场标签检索因此不可用。
--       需按当前列的实际类型二选一执行。

-- ============ MySQL：tag_type 为 ENUM（常见存量形态）============
-- 追加 PLUGIN 枚举值，保留既有数据（非破坏性）：
ALTER TABLE tag MODIFY COLUMN tag_type
    ENUM('TOOL', 'FORUM', 'VIDEO', 'PLUGIN') NOT NULL;

-- ============ MySQL：tag_type 为 VARCHAR + CHECK 约束（V6 建表形态）============
-- 若列是 VARCHAR 且带 tag_tag_type_check 约束，请改用以下语句放开 PLUGIN：
-- ALTER TABLE tag DROP CHECK tag_tag_type_check;
-- ALTER TABLE tag ADD CONSTRAINT tag_tag_type_check
--     CHECK (tag_type IN ('TOOL', 'FORUM', 'VIDEO', 'PLUGIN'));

-- ============ PostgreSQL ============
-- PG 侧 tag_type 通常为 VARCHAR（Hibernate 映射 @Enumerated(STRING)），无需改动；
-- 若历史建了 CHECK 约束，则放开：
-- ALTER TABLE tag DROP CONSTRAINT IF EXISTS tag_tag_type_check;
-- ALTER TABLE tag ADD CONSTRAINT tag_tag_type_check
--     CHECK (tag_type IN ('TOOL', 'FORUM', 'VIDEO', 'PLUGIN'));
