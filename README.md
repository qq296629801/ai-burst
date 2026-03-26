# AI Burst — 项目介绍

## 项目概述

AI Burst 是一套前后端分离的业务系统：后端基于 **Java 8** 与 **Spring Boot** 提供 API 与业务逻辑；前端基于 **Vue 3** 与 **Vite** 构建单页应用。数据层使用 **MySQL** 持久化、**Redis** 做缓存与会话/限流等、**MinIO** 承载对象存储（文件、图片等）。系统**内置权限体系**（用户、角色、菜单/资源、接口鉴权），支撑多角色、细粒度访问控制。

---

## 技术栈

### 后端

| 类别 | 技术 |
|------|------|
| 语言与运行时 | Java 1.8 |
| 框架 | Spring Boot |
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
| `产品/` | 产品需求、原型说明、用户故事等 |
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

---

## 代码工程

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot 2.7 + Java 8、MyBatis、Redis、JWT、Flyway；含 **大模型通道** 与 **统一对话**（`V2__llm_module.sql`） |
| `frontend/` | Vue 3 + Vite + Pinia + Element Plus；含 **通道配置**、**对话体验** 页面 |

### 环境要求

- JDK 8、Maven 3.6+
- MySQL 8（需先创建库 `CREATE DATABASE ai_burst DEFAULT CHARACTER SET utf8mb4;`）
- Redis 6+
- Node.js 18+（前端）

### 启动后端

```bash
cd backend
# 按需修改 src/main/resources/application.yml 中的数据源与 Redis
mvn spring-boot:run
```

默认在应用首次启动且无 `admin` 用户时，会写入用户 **admin / admin123** 并绑定内置超级管理员角色（权限数据由 Flyway 迁移 `V1__init.sql` 初始化）。**大模型菜单与表结构**见 `V2__llm_module.sql`；升级后请 **重新登录** 或等待权限缓存过期以加载新菜单。

大模型通道的 **API Key** 使用 `aiburst.llm.crypto.secret` 加密存储，生产环境务必修改。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 [http://localhost:5173](http://localhost:5173)，通过 Vite 代理将 `/api` 转发到 `http://127.0.0.1:8080`。

### MinIO

对象存储未在本骨架中接入业务代码，可按需在 `backend` 增加 MinIO 客户端与配置。

### 编译报错 `TypeTag :: UNKNOWN` / `ExceptionInInitializerError`

多见于 **IntelliJ 使用 JDK 17+ 内置编译** 且 **Lombok 插件或依赖偏旧**。可按顺序处理：

1. **重新加载 Maven**：右键 `backend/pom.xml` → Maven → Reload Project。  
2. **开启注解处理**：Settings → Build, Execution, Deployment → Compiler → Annotation Processors → 勾选 **Enable annotation processing**。  
3. **更新 Lombok 插件**：Settings → Plugins → 搜索 Lombok → 更新到最新。  
4. **统一 JDK**：Project Structure → Project SDK 选 **JDK 8** 或 **11**（与 `java.version` 1.8 一致最省事）；若必须用 JDK 21+ 编译，请保证 `pom.xml` 里 `lombok.version` 已较新（当前工程已显式指定并与 `maven-compiler-plugin` 的 `annotationProcessorPaths` 对齐）。

命令行验证：`cd backend && mvn -q -DskipTests compile`。

### Flyway：`Validate failed` / V2（llm module）失败

多为 **V2 执行中断** 或历史表中有 **失败记录**。请先 **停止应用**，按 [技术方案/Flyway故障恢复说明.md](技术方案/Flyway故障恢复说明.md) 清理半成品并执行 `mvn flyway:repair`（或手工修复 `flyway_schema_history`）后再启动。`backend/pom.xml` 已配置 `flyway-maven-plugin`，密码需与 `application.yml` 一致（可用 `-Dflyway.password=` 覆盖）。

若日志里还有 **`permissionMapper` / `sqlSessionTemplate`** 报错，通常是 Flyway 先失败导致容器未初始化完成，**先修 Flyway** 即可。仅本地应急可照 `application-local.example.yml` 暂时关闭 Flyway。

---

## 后续落地建议

- 在 `技术方案/` 中补充：ER 图、核心表结构、Redis Key 规范、MinIO 桶与路径约定、统一错误码与日志规范。

---

*本文档随项目演进持续更新。*
