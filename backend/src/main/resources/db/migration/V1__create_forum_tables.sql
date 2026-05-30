-- Forum Module Database Schema
-- 帖子分类
CREATE TABLE forum_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 标签（系统预设 + 用户自创）
CREATE TABLE forum_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    post_count INT DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 帖子表
CREATE TABLE forum_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    status ENUM('NORMAL', 'DELETED', 'HIDDEN') DEFAULT 'NORMAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_forum_post_author (author_id),
    INDEX idx_forum_post_category (category_id),
    INDEX idx_forum_post_created (created_at)
);

-- 帖子-标签关联
CREATE TABLE forum_post_tag (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id)
);

-- 评论（楼中楼）
CREATE TABLE forum_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT,
    author_name VARCHAR(50),
    parent_id BIGINT,
    root_id BIGINT,
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_forum_comment_post (post_id),
    INDEX idx_forum_comment_root (root_id)
);

-- 点赞
CREATE TABLE forum_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT,
    comment_id BIGINT,
    user_id BIGINT,
    ip_hash VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);