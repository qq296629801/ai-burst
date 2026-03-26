# Flyway 校验失败 / V2 迁移失败 — 恢复说明

## 现象

启动时出现类似错误：

```text
FlywayValidateException: Validate failed: Migrations have failed validation
Detected failed migration to version 2 (llm module).
Please remove any half-completed changes then run repair to fix the schema history.
```

含义：`flyway_schema_history` 里 **版本 2** 被标记为 **执行失败**（`success = 0`），或库中留有 **半成品**（例如表已建、后续 `INSERT` 失败），与 Flyway 记录不一致，校验无法通过。

### 与 `PermissionMapper` / `sqlSessionTemplate` 报错的关系

日志里若同时出现：

`Error creating bean with name 'flywayInitializer' ... FlywayValidateException`

以及：

`Error creating bean with name 'permissionMapper' ... sqlSessionTemplate`

**根因仍是 Flyway**：`FlywayMigrationInitializer` 在启动早期执行失败，Spring 容器整体初始化中断，MyBatis 相关 Bean 无法完整创建，属于 **连锁反应**，一般**不必**单独改 Mapper。先按本文修好 Flyway / 历史表即可。

---

## 0. 临时绕过 Flyway（仅本地排障，不推荐长期）

若需要先启动应用做别的事，可复制 `backend/src/main/resources/application-local.example.yml` 为 **`application-local.yml`**（该文件已在仓库 `.gitignore` 中），并启用 profile `local`，其中 `spring.flyway.enabled: false`。修库并执行 `repair` 后，**务必恢复** Flyway 或去掉该 profile，以免遗漏迁移。

---

## 处理思路（按顺序）

1. **停掉** Spring Boot 应用（避免并发改库）。  
2. **看清当前状态**：查历史表 + 看库里是否已有 `llm_channel`、是否已有 id 为 100–120 的权限数据。  
3. **清掉半成品**（仅当 V2 未完整成功时）。  
4. **修复历史表**：删除失败记录，或执行 `flyway repair`。  
5. **再次启动**，让 Flyway **重新执行** `V2__llm_module.sql`。

---

## 1. 查看 Flyway 历史

```sql
USE ai_burst;

SELECT installed_rank, version, description, script, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

- `success = 0`（或 `false`）：该版本迁移失败，需按下文修复。  
- `success = 1` 且仍报 **checksum** 类错误：属于「脚本被改过」场景，见文末 **校验和不一致**。

---

## 2. 清理半成品（V2 未跑完时）

在 **确认可以丢弃本次 V2 的中间结果** 的前提下执行（生产库请先备份）：

```sql
-- 大模型通道表（V2 会创建）
DROP TABLE IF EXISTS llm_channel;

-- 若 V2 已插入权限/角色关联，重跑前需删掉，否则会主键/唯一键冲突
DELETE FROM sys_role_permission WHERE role_id = 1 AND perm_id BETWEEN 100 AND 120;
DELETE FROM sys_permission WHERE id BETWEEN 100 AND 120;
```

若 `llm_channel` 里已有要保留的业务数据，**不要**直接 `DROP TABLE`，应联系 DBA/在测试库按实际状态单独处理。

---

## 3. 修复 `flyway_schema_history`（去掉失败记录）

### 方式 A：Maven `repair`（推荐）

在 `backend` 目录执行（**数据库 URL/用户/密码与 `application.yml` 一致**）：

```bash
cd backend
mvn flyway:repair \
  -Dflyway.url="jdbc:mysql://127.0.0.1:3306/ai_burst?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false" \
  -Dflyway.user=root \
  -Dflyway.password=你的密码
```

`repair` 会清理 **失败** 迁移在历史表中的记录（具体行为以 Flyway 8.5 为准）。  
若 `pom.xml` 里已配置 `flyway.url` / `flyway.user` / `flyway.password`，可直接：

```bash
mvn flyway:repair -Dflyway.password=你的密码
```

### 方式 B：手工删历史行（谨慎）

**仅当** 确认版本 2 对应失败记录、且已按上文清理库对象时：

```sql
DELETE FROM flyway_schema_history WHERE version = '2';
```

删错版本会导致 Flyway 重复或跳过脚本，**务必核对 `version` / `script` 列**（应为 `V2__llm_module.sql`）。

---

## 4. 再次启动应用

清理完成且历史表修复后，重新运行：

```bash
mvn spring-boot:run
```

Flyway 应会 **重新执行** `V2__llm_module.sql`。若再次失败，根据 MySQL 报错修数据或脚本后重复上述步骤。

### 若报错 `Duplicate entry '100' for key 'sys_permission.PRIMARY'`

说明 **V2 里的权限行已存在**，Flyway 又执行了一遍 `INSERT`。当前仓库中 `V2__llm_module.sql` 已改为 **`INSERT IGNORE`** + **`CREATE TABLE IF NOT EXISTS`**，重复执行不会主键冲突。

- **尚未成功跑完 V2 的环境**：按上文清理后重启即可；或直接用新版脚本重跑。  
- **本机曾成功应用过旧版 V2**：修改迁移文件后 Flyway 可能报 **checksum mismatch**，执行一次 `mvn flyway:repair -Dflyway.password=你的密码` 后再启动。

---

## 5. 校验和不一致（与「失败迁移」不同）

若错误为 **checksum mismatch**（修改过已执行过的迁移文件内容），可选：

```bash
mvn flyway:repair -Dflyway.password=...
```

在开发环境用 `repair` 把历史表中的校验和更新为当前文件；**生产环境**对已发布脚本应 **禁止改内容**，应新增 `V3__xxx.sql` 做变更。

---

## 6. 相关工程配置

- 迁移脚本目录：`backend/src/main/resources/db/migration/`  
- Maven 插件：`backend/pom.xml` 中 `flyway-maven-plugin`（与 Spring Boot 自带的 Flyway 使用同一 `locations`）

---

*若使用 Docker / 多环境，请将连接串与账号换成对应环境的值。*
