# AI Burst — 项目介绍

## 项目概述

AI Burst 是一套前后端分离的业务系统：后端基于 **Java 24** 与 **Spring Boot 3.5** 提供 API 与业务逻辑；前端基于 **Vue 3** 与 **Vite** 构建单页应用。数据层使用 **MySQL** 持久化、**Redis** 做缓存与会话/限流等、**MinIO** 承载对象存储（文件、图片等）。系统**内置权限体系**（用户、角色、菜单/资源、接口鉴权），支撑多角色、细粒度访问控制。

---

## 技术栈

### 后端

| 类别 | 技术 |
|------|------|
| 语言与运行时 | Java 24 |
| 框架 | Spring Boot 3.5 |
| 持久层 | MyBatis |
| 关系型数据库 | MySQL |
| 缓存 | Redis |
| 对象存储 | MinIO |

### 前端

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3（Composition API） |
| 构建工具 | Vite |
| 状态管理 | Pinia（Vue 3 官方推荐的 Store 方案，用于登录态、权限路由、用户信息等） |

---

## 架构要点

- **前后端分离**：RESTful（或统一响应封装）API 通信；前端独立部署，后端可水平扩展。
- **MyBatis**：SQL 与映射可控，适合复杂查询与存量表结构；可配合代码生成或统一分页规范。
- **Redis**：缓存热点数据、分布式会话（若采用）、验证码、接口限流等；与权限相关的 token 黑名单、在线用户等可按需接入。
- **MinIO**：S3 兼容 API，用于上传下载、私有桶策略；与业务附件表（MySQL）关联存储元数据。

---

## 权限系统（内置能力）

系统需自带完整权限能力，建议包含：

1. **身份认证**：登录、令牌（如 JWT）或 Session + Redis；登出与安全策略（密码策略、失败锁定等可按产品要求扩展）。
2. **授权模型**：用户 — 角色 — 权限（菜单/按钮/API）；支持多角色叠加。
3. **前端**：路由守卫 + 菜单/按钮级 `v-permission` 或指令控制；Store（Pinia）中维护 `roles`、`permissions`、`routes`，与后端返回的权限树对齐。
4. **后端**：Spring Security（或等价方案）拦截器/注解保护接口；与数据库中的权限标识一致，避免仅前端控制。

---

## 目录说明（文档）

| 目录 | 用途 |
|------|------|
| `产品/` | 产品需求、原型说明、用户故事等；**界面操作**见 [产品/AI-Burst界面操作手册.md](产品/AI-Burst界面操作手册.md) |
| `技术方案/` | 详细设计、库表设计、接口约定、部署与运维说明等 |

**工程与协作**

- 开发规范（对齐《阿里巴巴 Java 开发手册》体系）：[技术方案/开发规范.md](技术方案/开发规范.md)
- 编辑器缩进与换行：仓库根目录 [.editorconfig](.editorconfig)

**权限专题**

- 产品线需求：[产品/权限产品线需求.md](产品/权限产品线需求.md)
- 技术方案：[技术方案/权限系统技术方案.md](技术方案/权限系统技术方案.md)

**大模型与 API 对接**

- 产品线需求：[产品/大模型与API对接产品线需求.md](产品/大模型与API对接产品线需求.md)
- 技术方案：[技术方案/大模型接入技术方案.md](技术方案/大模型接入技术方案.md)

**多 Agent 协作与项目管理**

- **界面操作（侧栏菜单与按钮）**：[产品/AI-Burst界面操作手册.md](产品/AI-Burst界面操作手册.md)；MAG 补充说明：[产品/MAG功能使用指南.md](产品/MAG功能使用指南.md)
- 产品线需求：[产品/多Agent协作与项目管理产品线需求.md](产品/多Agent协作与项目管理产品线需求.md)
- 技术方案（含 DDL、实现附录 §19）：[技术方案/多Agent协作技术方案.md](技术方案/多Agent协作技术方案.md)  
  - **Temporal**：根目录 [docker-compose.temporal.yml](docker-compose.temporal.yml)（UI 常见为 `http://localhost:8088`）；需求 **MEDIUMTEXT**；成员**不设**仅拍板角色。（后端 **JDK 24 / Boot 3.5**。）  
  - OpenAPI 草案：[技术方案/mag-openapi-stub.yaml](技术方案/mag-openapi-stub.yaml)；业务错误码见技术方案 **§19.7**。

---

## 代码工程

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot 3.5 + Java 24、MyBatis、Redis、JWT（JJWT 0.12）、Flyway；含 **大模型**（`V2__`）、**多 Agent 表与菜单种子**（`V3__mag_module.sql`、`V4__mag_permission_seed.sql`） |
| `frontend/` | Vue 3 + Vite + Pinia + Element Plus；含 **通道配置**、**对话体验** 页面 |

### 环境要求

- **JDK 24**（`mvn compile` 与 IDE 均须使用 24）、Maven 3.9+
- MySQL 8（需先创建库 `CREATE DATABASE ai_burst DEFAULT CHARACTER SET utf8mb4;`）
- Redis 6+
- Node.js 18+（前端）

### 启动后端

```bash
cd backend
# 按需修改 src/main/resources/application.yml 中的数据源与 Redis
mvn spring-boot:run
```

默认在应用首次启动且无 `admin` 用户时，会写入用户 **admin / admin123** 并绑定内置超级管理员角色（权限数据由 Flyway 迁移 `V1__init.sql` 初始化）。**大模型菜单与表结构**见 `V2__llm_module.sql`；**多 Agent 表与菜单**见 `V3__`、`V4__`。升级后请 **重新登录** 或等待权限缓存过期以加载新菜单。

**OpenAPI / Swagger UI**（匿名可访问）：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)；分组 **mag** 的 JSON：[http://localhost:8080/v3/api-docs/mag](http://localhost:8080/v3/api-docs/mag)。**MAG 探活**（需登录）：`GET /api/mag/ping`。

大模型通道的 **API Key** 使用 `aiburst.llm.crypto.secret` 加密存储，生产环境务必修改。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 [http://localhost:5173](http://localhost:5173)，通过 Vite 代理将 `/api` 转发到 `http://127.0.0.1:8080`。

### Temporal（多 Agent 开发用）

```bash
docker compose -f docker-compose.temporal.yml up -d
```

应用侧在 `application.yml` 中配置 **`aiburst.mag.temporal`**：`enabled`、`target`（如 `127.0.0.1:7233`）、`namespace`（默认 `default`）、**`task-queue`**（默认 `mag-orchestration`，须与 Worker 一致）。当 **`enabled=true`** 且能连上 Temporal 时，Spring Boot 进程内会启动 **Temporal Worker**，并在「触发 Agent run / 触发线程编排」时 **`start` 对应 Workflow**，由 **Activity**（`MagOrchestrationActivitiesImpl`）执行（当前为日志占位，可在此接入 LangChain4j 等）。`enabled=false` 时接口返回明确提示，不启动 Worker。单测 profile `test` 中默认 **`enabled=false`** 以免依赖 Docker。业务库仍为 **MySQL**；Temporal 自带 PostgreSQL 仅服务 Temporal Server。

### MinIO

对象存储未在本骨架中接入业务代码，可按需在 `backend` 增加 MinIO 客户端与配置。

### 编译报错 `TypeTag :: UNKNOWN` / Lombok 与 JDK 不匹配

1. **确认 JDK 24**：`java -version`；`Project Structure` → **Project SDK** 选 **24**，与 `backend/pom.xml` 中 `java.version` 一致。  
2. **重新加载 Maven**：右键 `backend/pom.xml` → Maven → Reload Project。  
3. **开启注解处理**：Settings → Compiler → Annotation Processors → 勾选 **Enable annotation processing**。  
4. **更新 Lombok 插件**（IDEA）到与当前 Lombok 依赖兼容的版本。

命令行验证（示例，按本机 `JAVA_HOME` 指向 JDK 24）：`cd backend && mvn -q -DskipTests compile`。

### Flyway：`Validate failed` / V2（llm module）失败

多为 **V2 执行中断** 或历史表中有 **失败记录**。请先 **停止应用**，按 [技术方案/Flyway故障恢复说明.md](技术方案/Flyway故障恢复说明.md) 清理半成品并执行 `mvn flyway:repair`（或手工修复 `flyway_schema_history`）后再启动。`backend/pom.xml` 已配置 `flyway-maven-plugin`，密码需与 `application.yml` 一致（可用 `-Dflyway.password=` 覆盖）。

若日志里还有 **`permissionMapper` / `sqlSessionTemplate`** 报错，通常是 Flyway 先失败导致容器未初始化完成，**先修 Flyway** 即可。仅本地应急可照 `application-local.example.yml` 暂时关闭 Flyway。

---

## 后续落地建议

- 在 `技术方案/` 中补充：ER 图、核心表结构、Redis Key 规范、MinIO 桶与路径约定、统一错误码与日志规范。

---

*本文档随项目演进持续更新。*
