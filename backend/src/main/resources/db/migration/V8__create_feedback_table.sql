-- 留言板模块：feedback_message 表
CREATE TABLE IF NOT EXISTS feedback_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL COMMENT '留言内容',
    nickname VARCHAR(50) NULL COMMENT '昵称（匿名用户自定义或已登录用户自动取）',
    contact VARCHAR(100) NULL COMMENT '联系方式（可选）',
    category VARCHAR(20) NOT NULL DEFAULT 'SUGGESTION' COMMENT '分类：SUGGESTION, BUG_REPORT, PRAISE, OTHER',
    user_id BIGINT NULL COMMENT '提交用户ID（匿名时为NULL）',
    ip_hash VARCHAR(64) NULL COMMENT '匿名用户IP的SHA-256哈希',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL, DELETED',
    admin_reply TEXT NULL COMMENT '管理员回复内容',
    replied_by BIGINT NULL COMMENT '回复管理员ID',
    replied_at DATETIME NULL COMMENT '回复时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_feedback_status_created (status, created_at DESC),
    INDEX idx_feedback_category_status (category, status, created_at DESC),
    INDEX idx_feedback_user (user_id),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    CONSTRAINT fk_feedback_replied_by FOREIGN KEY (replied_by) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
