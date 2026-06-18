-- Migration: add-video-tables.sql
-- 添加微视频相关表结构

-- video table
CREATE TABLE IF NOT EXISTS video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    duration INT DEFAULT 0,
    cover_url VARCHAR(500),
    uploader_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_video_uploader (uploader_id, status),
    INDEX idx_video_status_created (status, created_at DESC),
    CONSTRAINT fk_video_uploader FOREIGN KEY (uploader_id) REFERENCES user(id)
);

-- video_comment table
CREATE TABLE IF NOT EXISTS video_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_video_comment_video (video_id, created_at DESC),
    CONSTRAINT fk_vcomment_video FOREIGN KEY (video_id) REFERENCES video(id),
    CONSTRAINT fk_vcomment_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- video_like table
CREATE TABLE IF NOT EXISTS video_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_video_like (video_id, user_id),
    CONSTRAINT fk_vlike_video FOREIGN KEY (video_id) REFERENCES video(id),
    CONSTRAINT fk_vlike_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- video_favorite table
CREATE TABLE IF NOT EXISTS video_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_video_favorite (video_id, user_id),
    CONSTRAINT fk_vfav_video FOREIGN KEY (video_id) REFERENCES video(id),
    CONSTRAINT fk_vfav_user FOREIGN KEY (user_id) REFERENCES user(id)
);
