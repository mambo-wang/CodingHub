-- V13: 插件市场增强（收藏数 + 置顶 + 标签）相关 schema 修正
-- 注：本项目 Flyway 未启用（schema 主要由 Hibernate ddl-auto:update 生成）。
--     plugin.favorite_count / plugin.pinned 列与 plugin_tag 表由 Hibernate 自动创建；
--     本文件为存量环境须【手动执行】的迁移参考（ddl-auto 不会修改已存在的 CHECK 约束）。

-- ============ MySQL ============
-- tag_type CHECK 约束放开 PLUGIN（MySQL 8.0.16+ CHECK 生效；旧版本为注释级约束，可跳过）
ALTER TABLE tag DROP CHECK tag_tag_type_check;
ALTER TABLE tag ADD CONSTRAINT tag_tag_type_check
    CHECK (tag_type IN ('TOOL', 'FORUM', 'VIDEO', 'PLUGIN'));

-- ============ PostgreSQL ============
-- ALTER TABLE tag DROP CONSTRAINT IF EXISTS tag_tag_type_check;
-- ALTER TABLE tag ADD CONSTRAINT tag_tag_type_check
--     CHECK (tag_type IN ('TOOL', 'FORUM', 'VIDEO', 'PLUGIN'));
