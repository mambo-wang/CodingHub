# Quickstart: AI 工具广场

**Feature**: 001-ai-tool-square
**Date**: 2026-05-29

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17+ | LTS version recommended |
| Node.js | 20+ | For frontend development |
| npm / pnpm | latest | npm comes with Node.js |
| MySQL | 8.0 | Running on localhost:3306, user/pass: root/root |
| Gradle | 8.x | Or use `./gradlew` wrapper (included) |
| Vue CLI / Vite | 5.x | `npm install -g vite` |

## Database Setup

```bash
# Login to MySQL
mysql -u root -proot

# Create database
CREATE DATABASE IF NOT EXISTS ai_tool_square CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

The database schema will be created by Spring Boot JPA on first run (using `spring.jpa.hibernate.ddl-auto=update` in dev mode). For production, use Flyway migrations.

## Backend Setup

```bash
# Navigate to backend directory
cd backend

# Build the project (downloads dependencies)
./gradlew build

# Run the application
./gradlew bootRun
```

The backend starts on `http://localhost:8081`.

### Useful Backend Commands

```bash
./gradlew test          # Run unit tests
./gradlew bootRun      # Start in dev mode with hot reload
./gradlew build -x test # Build without running tests
```

### Key Backend Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/register` | Register new user |
| `POST /api/v1/auth/login` | Login and get JWT |
| `GET /api/v1/tools` | List tools |
| `GET /api/v1/tools/{id}` | Get tool detail |
| `POST /api/v1/tools` | Create tool (auth required) |
| `PUT /api/v1/tools/{id}` | Update tool (auth required) |
| `DELETE /api/v1/tools/{id}` | Delete tool (auth required) |
| `GET /api/v1/users/me/tools` | Get my tools (auth required) |
| `GET /api/v1/categories` | List categories |

### Backend Configuration

`backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_tool_square?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

app:
  jwt:
    secret: <your-256-bit-secret-key-here-min-32-chars>
    access-token-expiration: 900000    # 15 min in ms
    refresh-token-expiration: 604800000 # 7 days in ms

logging:
  level:
    com.iaihub.toolbox: INFO
    org.springframework.security: WARN
```

## Frontend Setup

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

The frontend starts on `http://localhost:5173` (Vite default).

### Useful Frontend Commands

```bash
npm run dev       # Start dev server with HMR
npm run build     # Build for production
npm run preview   # Preview production build
npm run lint      # Run ESLint
npm run type-check # Run Vue TypeScript checker
```

### Frontend Configuration

Environment variables (create `.env` in frontend root):

```env
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

## Running the Full Stack

1. **Start MySQL** (if not running):
   ```bash
   brew services start mysql  # macOS
   # or: systemctl start mysql  # Linux
   ```

2. **Start Backend** (terminal 1):
   ```bash
   cd backend && ./gradlew bootRun
   ```

3. **Start Frontend** (terminal 2):
   ```bash
   cd frontend && npm run dev
   ```

4. **Open browser**: http://localhost:5173

## Project Directory Structure

```
ai-tool-square/
├── backend/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── src/main/java/com/iaihub/toolbox/
│   │   ├── ToolSquareApplication.java
│   │   ├── config/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── exception/
│   │   └── util/
│   └── src/main/resources/
│       └── application.yml
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── index.html
    └── src/
        ├── main.ts
        ├── App.vue
        ├── assets/
        ├── components/
        ├── pages/
        ├── stores/
        ├── services/
        └── router/
```

## Verification

After starting both services, verify:

1. **API Health**: `curl http://localhost:8081/api/v1/categories` returns category list
2. **Frontend**: http://localhost:5173 shows the tool square homepage
3. **Registration**: POST to `/api/v1/auth/register` creates a user
4. **Login**: POST to `/api/v1/auth/login` returns JWT tokens