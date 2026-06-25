.PHONY: help backend frontend db init install run stop build test clean lint lint-arch lint-quality lint-deps

help:
	@echo "CodingHub - Makefile"
	@echo ""
	@echo "可用命令:"
	@echo "  make db       - 创建数据库并初始化表结构"
	@echo "  make install  - 安装前端依赖"
	@echo "  make backend  - 启动后端服务 (8082端口)"
	@echo "  make frontend - 启动前端服务 (5173端口)"
	@echo "  make run      - 同时启动后端和前端"
	@echo "  make stop     - 停止所有服务"
	@echo ""
	@echo "构建命令:"
	@echo "  make build    - 编译后端（不运行测试）"
	@echo "  make test     - 运行后端测试"
	@echo "  make clean    - 清理构建产物"
	@echo ""
	@echo "Lint 命令 (Agent 基础设施):"
	@echo "  make lint         - 运行所有 lint 检查"
	@echo "  make lint-arch    - 检查架构层级依赖"
	@echo "  make lint-quality - 检查代码质量"
	@echo "  make lint-deps    - 检查循环依赖"

db:
	mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS ai_tool_square CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
	mysql -u root -proot ai_tool_square -e " \
		-- 用户表 \
		CREATE TABLE IF NOT EXISTS user ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			email VARCHAR(255) NOT NULL UNIQUE, \
			password VARCHAR(255) NOT NULL, \
			username VARCHAR(100) NOT NULL, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, \
			updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, \
			last_login_at DATETIME NULL, \
			INDEX idx_user_email (email) \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 工具分类表 \
		CREATE TABLE IF NOT EXISTS category ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			name VARCHAR(50) NOT NULL UNIQUE, \
			icon VARCHAR(255) NULL, \
			sort_order INT NOT NULL DEFAULT 0, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 工具表 \
		CREATE TABLE IF NOT EXISTS tool ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			name VARCHAR(100) NOT NULL, \
			category_id BIGINT NOT NULL, \
			content TEXT NOT NULL, \
			description VARCHAR(200) DEFAULT NULL, \
			uploader_id BIGINT NOT NULL, \
			status ENUM('NORMAL', 'DELETED') NOT NULL DEFAULT 'NORMAL', \
			view_count INT DEFAULT 0, \
			like_count INT DEFAULT 0, \
			comment_count INT DEFAULT 0, \
			score DECIMAL(10,2) DEFAULT 0, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, \
			updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, \
			INDEX idx_tool_category (category_id, status), \
			INDEX idx_tool_uploader (uploader_id, status), \
			INDEX idx_tool_name_status (name, status), \
			INDEX idx_tool_score (score DESC), \
			UNIQUE INDEX uk_tool_uploader_name (uploader_id, name, status), \
			CONSTRAINT fk_tool_category FOREIGN KEY (category_id) REFERENCES category(id), \
			CONSTRAINT fk_tool_uploader FOREIGN KEY (uploader_id) REFERENCES user(id) \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 工具文件表 \
		CREATE TABLE IF NOT EXISTS tool_file ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			tool_id BIGINT NOT NULL, \
			file_path VARCHAR(500) NOT NULL, \
			file_name VARCHAR(255) NOT NULL, \
			file_size BIGINT NOT NULL, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, \
			CONSTRAINT fk_tool_file_tool FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 工具点赞表 \
		CREATE TABLE IF NOT EXISTS tool_like ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			tool_id BIGINT NOT NULL, \
			user_id BIGINT NOT NULL, \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP, \
			CONSTRAINT uk_tool_like_tool_user UNIQUE (tool_id, user_id), \
			CONSTRAINT fk_tool_like_tool FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE, \
			CONSTRAINT fk_tool_like_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 统一标签表 \
		CREATE TABLE IF NOT EXISTS tag ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			name VARCHAR(50) NOT NULL, \
			tag_type VARCHAR(20) NOT NULL COMMENT 'TOOL, FORUM, VIDEO', \
			usage_count INT NOT NULL DEFAULT 0, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, \
			UNIQUE KEY uk_name_type (name, tag_type) \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 工具标签关联表 \
		CREATE TABLE IF NOT EXISTS tool_tag ( \
			tool_id BIGINT NOT NULL, \
			tag_id BIGINT NOT NULL, \
			PRIMARY KEY (tool_id, tag_id), \
			CONSTRAINT fk_tool_tag_tool FOREIGN KEY (tool_id) REFERENCES tool(id) ON DELETE CASCADE, \
			CONSTRAINT fk_tool_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 论坛分类表 \
		CREATE TABLE IF NOT EXISTS forum_category ( \
			id BIGINT PRIMARY KEY AUTO_INCREMENT, \
			name VARCHAR(50) NOT NULL UNIQUE, \
			description VARCHAR(255), \
			sort_order INT DEFAULT 0, \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 论坛标签表(保留兼容) \
		CREATE TABLE IF NOT EXISTS forum_tag ( \
			id BIGINT PRIMARY KEY AUTO_INCREMENT, \
			name VARCHAR(50) NOT NULL UNIQUE, \
			post_count INT DEFAULT 0, \
			is_system BOOLEAN DEFAULT FALSE, \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 论坛帖子表 \
		CREATE TABLE IF NOT EXISTS forum_post ( \
			id BIGINT PRIMARY KEY AUTO_INCREMENT, \
			title VARCHAR(200) NOT NULL, \
			content TEXT NOT NULL, \
			author_id BIGINT NOT NULL, \
			category_id BIGINT NOT NULL, \
			view_count INT DEFAULT 0, \
			like_count INT DEFAULT 0, \
			comment_count INT DEFAULT 0, \
			score DECIMAL(10,2) DEFAULT 0, \
			status ENUM('NORMAL', 'DELETED', 'HIDDEN') DEFAULT 'NORMAL', \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP, \
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, \
			INDEX idx_forum_post_author (author_id), \
			INDEX idx_forum_post_category (category_id), \
			INDEX idx_forum_post_created (created_at), \
			INDEX idx_forum_post_score (score DESC), \
			CONSTRAINT fk_forum_post_author FOREIGN KEY (author_id) REFERENCES user(id), \
			CONSTRAINT fk_forum_post_category FOREIGN KEY (category_id) REFERENCES forum_category(id) \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 帖子标签关联表（关联统一tag表） \
		CREATE TABLE IF NOT EXISTS forum_post_tag ( \
			post_id BIGINT NOT NULL, \
			tag_id BIGINT NOT NULL, \
			PRIMARY KEY (post_id, tag_id), \
			CONSTRAINT fk_forum_post_tag_post FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE, \
			CONSTRAINT fk_forum_post_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 微课标签关联表 \
		CREATE TABLE IF NOT EXISTS video_tag ( \
			video_id BIGINT NOT NULL, \
			tag_id BIGINT NOT NULL, \
			PRIMARY KEY (video_id, tag_id), \
			CONSTRAINT fk_video_tag_video FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE, \
			CONSTRAINT fk_video_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 论坛评论表 \
		CREATE TABLE IF NOT EXISTS forum_comment ( \
			id BIGINT PRIMARY KEY AUTO_INCREMENT, \
			post_id BIGINT NOT NULL, \
			author_id BIGINT, \
			author_name VARCHAR(50), \
			parent_id BIGINT, \
			root_id BIGINT, \
			content TEXT NOT NULL, \
			like_count INT DEFAULT 0, \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP, \
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, \
			INDEX idx_forum_comment_post (post_id), \
			INDEX idx_forum_comment_root (root_id), \
			CONSTRAINT fk_forum_comment_post FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 论坛点赞表 \
		CREATE TABLE IF NOT EXISTS forum_like ( \
			id BIGINT PRIMARY KEY AUTO_INCREMENT, \
			post_id BIGINT, \
			comment_id BIGINT, \
			user_id BIGINT, \
			ip_hash VARCHAR(64), \
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP, \
			CONSTRAINT fk_forum_like_post FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		-- 初始化工具分类数据 \
		INSERT IGNORE INTO category (name, icon, sort_order) VALUES \
			('Skill', 'Wrench', 1), \
			('MCP', 'Plug', 2), \
			('API', 'Globe', 3), \
			('Prompt', 'MessageSquare', 4), \
			('其他', 'Package', 5); \
		-- 初始化论坛分类数据 \
		INSERT IGNORE INTO forum_category (name, description, sort_order) VALUES \
			('技术交流', 'AI 相关技术讨论', 1), \
			('工具分享', 'AI 工具使用心得', 2), \
			('问题求助', '遇到问题寻求帮助', 3), \
			('心得体会', '使用 AI 工具的感悟', 4);"
	@echo "数据库初始化完成"

install:
	cd frontend && npm install

backend:
	cd backend && ./gradlew bootRun

frontend:
	cd frontend && npm run dev

run: db
	@echo "启动后端服务..."
	cd backend && ./gradlew bootRun &
	@echo "启动前端服务..."
	cd frontend && npm run dev &

stop:
	@pkill -f "gradlew bootRun" 2>/dev/null || true
	@pkill -f "vite" 2>/dev/null || true
	@echo "所有服务已停止"

build:
	cd backend && ./gradlew build -x test

test:
	cd backend && ./gradlew test

clean:
	cd backend && ./gradlew clean
	cd frontend && rm -rf node_modules dist

# Lint 命令 (Agent 基础设施)
lint: lint-arch lint-quality lint-deps
	@echo "✓ 所有 lint 检查通过"

lint-arch:
	@echo "检查架构层级依赖..."
	@bash scripts/lint-arch.sh

lint-quality:
	@echo "检查代码质量..."
	@bash scripts/lint-quality.sh

lint-deps:
	@echo "检查循环依赖..."
	@bash scripts/lint-deps.sh
