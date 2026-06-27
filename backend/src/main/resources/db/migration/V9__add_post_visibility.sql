-- Add visibility field to forum_post table
ALTER TABLE forum_post ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Create index for visibility filtering
CREATE INDEX idx_forum_post_visibility ON forum_post (visibility);
