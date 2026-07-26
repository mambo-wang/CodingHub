-- V11: 聊天室 P1 增强 - chat_message 加列 + 新建 chat_reaction 表
-- 注意：本项目 Flyway 未启用，schema 由 Hibernate ddl-auto:update 生成；本文件作为 MySQL 手动迁移参考。

-- 1. chat_message 新增列
ALTER TABLE chat_message ADD COLUMN reply_to BIGINT NULL;
ALTER TABLE chat_message ADD COLUMN edited TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE chat_message ADD COLUMN deleted_type VARCHAR(10) NULL;

-- 2. 新建 chat_reaction 表
CREATE TABLE chat_reaction (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    message_id  BIGINT NOT NULL,
    owner_key   VARCHAR(64) NOT NULL,
    emoji       VARCHAR(16) NOT NULL,
    created_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reaction_msg_owner_emoji UNIQUE (message_id, owner_key, emoji),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES chat_message (id) ON DELETE CASCADE
);
