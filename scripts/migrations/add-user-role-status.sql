-- Migration: Add role and status columns to user table
-- Run this migration to support user role management and approval workflow

ALTER TABLE `user`
  ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER `avatar_url`,
  ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER `role`;

-- All existing users get USER role and ACTIVE status by default
