-- V3__add_user_nickname.sql
-- Add nickname field to user table for user nickname feature

ALTER TABLE user 
ADD COLUMN nickname VARCHAR(50) NULL AFTER username,
ADD UNIQUE INDEX idx_user_nickname (nickname);