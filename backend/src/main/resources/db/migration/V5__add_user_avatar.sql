-- V5__add_user_avatar.sql
-- Add avatar_url field to user table for user avatar feature

ALTER TABLE user
    ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT '头像URL, 格式: /api/v1/static/avatars/{userId}.{ext}';

-- 老用户 avatar_url 默认为 NULL, 不需要 backfill
