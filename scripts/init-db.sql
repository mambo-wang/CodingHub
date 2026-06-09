-- --------------------------------------------------------
-- 主机:                           127.0.0.1
-- 服务器版本:                        8.4.9 - MySQL Community Server - GPL
-- 服务器操作系统:                      Win64
-- HeidiSQL 版本:                  12.10.0.7000
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- 导出 ai_tool_square 的数据库结构
CREATE DATABASE IF NOT EXISTS `ai_tool_square` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ai_tool_square`;

-- 导出  表 ai_tool_square.category 结构
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.category 的数据：~5 rows (大约)
INSERT IGNORE INTO `category` (`id`, `name`, `icon`, `sort_order`, `created_at`) VALUES
	(1, 'Skill', '', 1, '2026-06-09 15:56:34'),
	(2, 'MCP', '', 2, '2026-06-09 15:56:34'),
	(3, 'API', '', 3, '2026-06-09 15:56:34'),
	(4, 'Prompt', '', 4, '2026-06-09 15:56:34'),
	(5, '其他', '', 5, '2026-06-09 15:56:34');

-- 导出  表 ai_tool_square.forum_category 结构
CREATE TABLE IF NOT EXISTS `forum_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_category 的数据：~4 rows (大约)
INSERT IGNORE INTO `forum_category` (`id`, `name`, `description`, `sort_order`, `created_at`) VALUES
	(1, '技术交流', 'AI 相关技术讨论', 1, '2026-06-09 15:56:34'),
	(2, '工具分享', 'AI 工具使用心得', 2, '2026-06-09 15:56:34'),
	(3, '问题求助', '遇到问题寻求帮助', 3, '2026-06-09 15:56:34'),
	(4, '心得体会', '使用 AI 工具的感悟', 4, '2026-06-09 15:56:34');

-- 导出  表 ai_tool_square.forum_comment 结构
CREATE TABLE IF NOT EXISTS `forum_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `author_id` bigint DEFAULT NULL,
  `author_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `root_id` bigint DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `like_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_comment_post` (`post_id`),
  KEY `idx_forum_comment_root` (`root_id`),
  CONSTRAINT `fk_forum_comment_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_comment 的数据：~0 rows (大约)
INSERT IGNORE INTO `forum_comment` (`id`, `post_id`, `author_id`, `author_name`, `parent_id`, `root_id`, `content`, `like_count`, `created_at`, `updated_at`) VALUES
	(1, 1, 1, NULL, NULL, NULL, 'asdfasfd', 0, '2026-06-09 17:13:23', '2026-06-09 17:13:23'),
	(2, 1, 1, NULL, 1, 1, 'asdfasdf', 0, '2026-06-09 17:13:26', '2026-06-09 17:13:26'),
	(3, 1, 1, NULL, NULL, NULL, 'asdfsadf', 0, '2026-06-09 17:13:28', '2026-06-09 17:13:28'),
	(4, 1, 1, NULL, 1, 1, 'asdfasdf', 0, '2026-06-09 17:13:33', '2026-06-09 17:13:33');

-- 导出  表 ai_tool_square.forum_like 结构
CREATE TABLE IF NOT EXISTS `forum_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `ip_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_forum_like_post` (`post_id`),
  CONSTRAINT `fk_forum_like_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_like 的数据：~0 rows (大约)
INSERT IGNORE INTO `forum_like` (`id`, `post_id`, `comment_id`, `user_id`, `ip_hash`, `created_at`) VALUES
	(1, 1, NULL, 1, NULL, '2026-06-09 17:13:19');

-- 导出  表 ai_tool_square.forum_post 结构
CREATE TABLE IF NOT EXISTS `forum_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `view_count` int DEFAULT '0',
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `score` decimal(10,2) DEFAULT '0.00',
  `status` enum('NORMAL','DELETED','HIDDEN') COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_author` (`author_id`),
  KEY `idx_forum_post_category` (`category_id`),
  KEY `idx_forum_post_created` (`created_at`),
  KEY `idx_forum_post_score` (`score` DESC),
  CONSTRAINT `fk_forum_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_forum_post_category` FOREIGN KEY (`category_id`) REFERENCES `forum_category` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_post 的数据：~0 rows (大约)
INSERT IGNORE INTO `forum_post` (`id`, `title`, `content`, `author_id`, `category_id`, `view_count`, `like_count`, `comment_count`, `score`, `status`, `created_at`, `updated_at`) VALUES
	(1, 'asdfa', 'asdfdsf', 1, 2, 4, 1, 2, 0.00, 'NORMAL', '2026-06-09 17:13:12', '2026-06-09 17:14:26');

-- 导出  表 ai_tool_square.forum_post_tag 结构
CREATE TABLE IF NOT EXISTS `forum_post_tag` (
  `post_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`post_id`,`tag_id`),
  KEY `fk_forum_post_tag_tag` (`tag_id`),
  CONSTRAINT `fk_forum_post_tag_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_post_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `forum_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_post_tag 的数据：~0 rows (大约)

-- 导出  表 ai_tool_square.forum_tag 结构
CREATE TABLE IF NOT EXISTS `forum_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_count` int DEFAULT '0',
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.forum_tag 的数据：~0 rows (大约)

-- 导出  表 ai_tool_square.post_favorites 结构
CREATE TABLE IF NOT EXISTS `post_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhq2fdksuab9v9p4oay12wd4pg` (`user_id`,`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.post_favorites 的数据：~0 rows (大约)
INSERT IGNORE INTO `post_favorites` (`id`, `created_at`, `post_id`, `user_id`) VALUES
	(1, '2026-06-09 17:13:19.582572', 1, 1);

-- 导出  表 ai_tool_square.tool 结构
CREATE TABLE IF NOT EXISTS `tool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploader_id` bigint NOT NULL,
  `status` enum('NORMAL','DELETED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL',
  `view_count` int DEFAULT '0',
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `score` decimal(10,2) DEFAULT '0.00',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_uploader_name` (`uploader_id`,`name`,`status`),
  UNIQUE KEY `uk_tool_uploader_name_category` (`uploader_id`,`name`,`category_id`,`status`),
  KEY `idx_tool_category` (`category_id`,`status`),
  KEY `idx_tool_uploader` (`uploader_id`,`status`),
  KEY `idx_tool_name_status` (`name`,`status`),
  KEY `idx_tool_score` (`score` DESC),
  KEY `idx_tool_version` (`version`),
  CONSTRAINT `fk_tool_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `fk_tool_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.tool 的数据：~0 rows (大约)
INSERT IGNORE INTO `tool` (`id`, `name`, `category_id`, `content`, `uploader_id`, `status`, `view_count`, `like_count`, `comment_count`, `score`, `created_at`, `updated_at`, `version`) VALUES
	(1, 'test', 1, 'asdfa', 1, 'DELETED', 0, 0, 0, 0.00, '2026-06-09 17:11:36', '2026-06-09 17:14:13', '1.0.0'),
	(2, 'test1', 1, 'asdfa', 1, 'DELETED', 0, 0, 0, 0.00, '2026-06-09 17:12:10', '2026-06-09 17:14:11', '1.0.0'),
	(3, 'test12', 1, 'asdfa', 1, 'DELETED', 0, 0, 0, 0.00, '2026-06-09 17:12:27', '2026-06-09 17:14:09', '1.0.0'),
	(4, 'test123', 1, 'asdfa', 1, 'DELETED', 0, 1, 1, 8.00, '2026-06-09 17:12:42', '2026-06-09 17:14:08', '1.0.0');

-- 导出  表 ai_tool_square.tool_comment 结构
CREATE TABLE IF NOT EXISTS `tool_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `tool_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tool_comment_tool` (`tool_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.tool_comment 的数据：~0 rows (大约)
INSERT IGNORE INTO `tool_comment` (`id`, `content`, `created_at`, `tool_id`, `user_id`) VALUES
	(1, 'asdf', '2026-06-09 17:12:51.805101', 4, 1);

-- 导出  表 ai_tool_square.tool_file 结构
CREATE TABLE IF NOT EXISTS `tool_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tool_id` bigint NOT NULL,
  `file_size` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('NORMAL','DELETED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_path` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7yhodf8qgy3tm7a5o1ap0mujr` (`stored_path`),
  KEY `idx_tool_file_tool_id` (`tool_id`),
  CONSTRAINT `fk_tool_file_tool` FOREIGN KEY (`tool_id`) REFERENCES `tool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.tool_file 的数据：~0 rows (大约)

-- 导出  表 ai_tool_square.tool_like 结构
CREATE TABLE IF NOT EXISTS `tool_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tool_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_like_tool_user` (`tool_id`,`user_id`),
  KEY `fk_tool_like_user` (`user_id`),
  CONSTRAINT `fk_tool_like_tool` FOREIGN KEY (`tool_id`) REFERENCES `tool` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tool_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.tool_like 的数据：~0 rows (大约)
INSERT IGNORE INTO `tool_like` (`id`, `tool_id`, `user_id`, `created_at`) VALUES
	(1, 4, 1, '2026-06-09 17:12:49');

-- 导出  表 ai_tool_square.user 结构
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` datetime DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_username` (`username`),
  UNIQUE KEY `idx_user_nickname` (`nickname`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  ai_tool_square.user 的数据：~0 rows (大约)
INSERT IGNORE INTO `user` (`id`, `password`, `username`, `created_at`, `updated_at`, `last_login_at`, `nickname`) VALUES
	(1, '$2a$10$NdXbmbUsauz1wst9aPxfpO8WwIKYnv0DxZQBd5wDoIWUFSOGcRj6u', 'wangbao', '2026-06-09 17:06:46', '2026-06-09 17:10:20', '2026-06-09 17:10:20', '王宝');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
