-- V4__add_tool_version.sql
-- Add version field to tool table and update uniqueness constraints
-- Feature: 004-tool-version-management

ALTER TABLE tool
ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0' AFTER content;

-- Drop old unique constraint (uploader_id, name, status)
ALTER TABLE tool
DROP INDEX uk_tool_uploader_name;

-- Add new unique constraint (uploader_id, name, category_id, status)
ALTER TABLE tool
ADD UNIQUE INDEX uk_tool_uploader_name_category (uploader_id, name, category_id, status);

-- Add index on version for faster lookups
ALTER TABLE tool
ADD INDEX idx_tool_version (version);
