-- V12: 修复 chat_reaction.emoji 列 collation
-- 问题：建表时继承库默认 collation utf8mb4_unicode_ci，该 collation 下大量 emoji
--       排序权重相同（'👍'='🎉' 判等），导致 toggleReaction 的 exists 查询误命中、
--       DELETE 误删其他 emoji 的行——表现为"点第二个表情会取消第一个表情"。
-- 修复：emoji 改用 utf8mb4_bin（二进制精确比较），emoji 精确匹配场景语义正确。

ALTER TABLE chat_reaction
    MODIFY emoji VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

-- 注：本项目 Flyway 未启用（schema 由 Hibernate ddl-auto:update 生成），本文件作为
--     MySQL 手动迁移参考；Hibernate update 不会自动修正已存在列的 collation，
--     已有环境须手动执行本语句。PostgreSQL 默认 collation 按 codepoint 比较，
--     不存在本问题。
