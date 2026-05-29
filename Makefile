.PHONY: help backend frontend db init install run stop lint lint-arch lint-quality lint-deps

help:
	@echo "AI 工具广场 - Makefile"
	@echo ""
	@echo "可用命令:"
	@echo "  make db       - 创建数据库并初始化表结构"
	@echo "  make install  - 安装前端依赖"
	@echo "  make backend  - 启动后端服务 (8080端口)"
	@echo "  make frontend - 启动前端服务 (5173端口)"
	@echo "  make run      - 同时启动后端和前端"
	@echo "  make stop     - 停止所有服务"
	@echo ""
	@echo "Lint 命令 (Agent 基础设施):"
	@echo "  make lint         - 运行所有 lint 检查"
	@echo "  make lint-arch    - 检查架构层级依赖"
	@echo "  make lint-quality - 检查代码质量"
	@echo "  make lint-deps    - 检查循环依赖"

db:
	mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS ai_tool_square CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
	mysql -u root -proot ai_tool_square -e " \
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
		CREATE TABLE IF NOT EXISTS category ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			name VARCHAR(50) NOT NULL UNIQUE, \
			icon VARCHAR(255) NULL, \
			sort_order INT NOT NULL DEFAULT 0, \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		CREATE TABLE IF NOT EXISTS tool ( \
			id BIGINT AUTO_INCREMENT PRIMARY KEY, \
			name VARCHAR(100) NOT NULL, \
			category_id BIGINT NOT NULL, \
			content TEXT NOT NULL, \
			uploader_id BIGINT NOT NULL, \
			status ENUM('NORMAL', 'DELETED') NOT NULL DEFAULT 'NORMAL', \
			created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, \
			updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, \
			INDEX idx_tool_category (category_id, status), \
			INDEX idx_tool_uploader (uploader_id, status), \
			INDEX idx_tool_name_status (name, status), \
			UNIQUE INDEX uk_tool_uploader_name (uploader_id, name, status), \
			CONSTRAINT fk_tool_category FOREIGN KEY (category_id) REFERENCES category(id), \
			CONSTRAINT fk_tool_uploader FOREIGN KEY (uploader_id) REFERENCES user(id) \
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; \
		INSERT IGNORE INTO category (name, icon, sort_order) VALUES \
			('Skill', '🛠️', 1), \
			('MCP', '🔌', 2), \
			('API', '🌐', 3), \
			('Prompt', '💬', 4), \
			('其他', '📦', 5);"
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
