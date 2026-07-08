# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment

- **JDK 17** required — JDK 24 breaks Lombok (`@Data`/`@Builder` annotation processing fails). `JAVA_HOME` must point to JDK 17.
- Maven 3.6+, MySQL 5.7+ (Windows 原生服务)
- **Docker Desktop** required — 基础设施通过 `docker compose` 管理（见下方 Infrastructure 节）

## Build & Run

```bash
# Build all modules (skip tests)
mvn install -DskipTests

# Build a module and its dependencies
mvn install -DskipTests -pl sky-server -am

# ★ 仅 Windows 原生 JDK 需要：设置 JVM 编码为 UTF-8（Linux/WSL/macOS 默认就是 UTF-8，无需此步骤）
#   Git Bash / PowerShell：
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
#   PowerShell:
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# Start sky-server (port 8080)
# JVM args (--add-opens for Seata) are configured in sky-server/pom.xml spring-boot-maven-plugin
mvn -pl sky-server spring-boot:run

# Start sky-gateway (port 8081)
mvn -pl sky-gateway spring-boot:run

# ============================================================
# Docker 容器化部署（一键启动全部服务）
# ============================================================

# 1. 先构建 JAR 包
mvn package -DskipTests

# 2. 构建 Docker 镜像 + 启动全部服务
docker compose up -d --build

# 3. 查看所有容器状态
docker compose ps

# 4. 查看应用日志
docker compose logs -f sky-server
docker compose logs -f sky-gateway

# 5. 停止全部服务
docker compose down

# 注意: Docker 部署时 sky-server 使用 Nacos Data ID "sky-server-docker.yaml"，
# 而非本地开发的 "sky-server-dev.yaml"。首次部署前需在 Nacos 控制台导入此配置。
# 模板文件: nacos_config_example/nacos-config-sky-server-docker.yaml

# Start sky-server as JAR (production):
# java --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar

# Dependency tree
mvn dependency:tree -pl sky-server
```

## Infrastructure (Docker Compose)

```bash
# Start all infrastructure containers
docker compose up -d

# Stop all
docker compose down

# Status
docker compose ps

# View logs of a specific service
docker compose logs -f nacos
docker compose logs -f seata
```

### Service ports

| Service | Port | Credentials |
|---------|------|-------------|
| Nacos | `:8848` (API/gRPC 9848/9849), 控制台 `:8849` (容器 8080) | **已开启认证** `nacos` / `SkyNacos@2026` |
| Redis | `:6379` | password: `123456` |
| Seata | `:8091` (TC), `:7091` (console) | console: `seata` / `seata` |
| Sentinel Dashboard | `:8858` | `sentinel` / `sentinel` |
| RocketMQ NameServer | `:9876` | — |
| RocketMQ Broker | `:10911` (remoting), `:10909` (VIP) | — |
| RocketMQ Console | `:8082` | — |
| MySQL | `:3306` (Windows 原生) | root / `123456` |

### Container details

| Container | Image | Notes |
|-----------|-------|-------|
| `sky-nacos` | `nacos/nacos-server:v3.0.3` | 单机模式，MySQL 后端 (`nacos_config` 库)，**认证开启** (`NACOS_AUTH_ENABLE=true`)，控制台端口 8080→主机 8849 |
| `sky-redis` | `redis:7-alpine` | AOF 持久化，命名卷 `redis-data` |
| `sky-seata` | `seataio/seata-server:2.5.0` | AT 模式，注册到 Nacos，配置挂载 `docker/seata-server/application.yml` |
| `sky-sentinel` | `sky-sentinel-dashboard:1.8.9` | 本地构建 (`docker/sentinel-dashboard/Dockerfile`) |
| `sky-namesrv` | `apache/rocketmq:5.3.1` | NameServer 路由注册，`autoCreateTopicEnable=true` |
| `sky-broker` | `apache/rocketmq:5.3.1` | Broker 消息存储，连接 namesrv:9876 |
| `sky-rmq-console` | `apacherocketmq/rocketmq-dashboard` | 管理控制台 (`http://localhost:8082`) |
| `sky-server` | `sky-server:1.0-SNAPSHOT` | 本地构建 (`sky-server/Dockerfile`)，`SPRING_PROFILES_ACTIVE=docker` |
| `sky-gateway` | `sky-gateway:1.0-SNAPSHOT` | 本地构建 (`sky-gateway/Dockerfile`)，`SPRING_PROFILES_ACTIVE=docker` |

### Nacos config initialization

Nacos 配置需要在 MySQL 中预先创建 `nacos_config` 数据库并执行 `.sql/nacos-mysql.sql`。

**⚠️ Nacos 3.x 已开启认证，首次部署需初始化管理员**（`users`/`roles` 表默认为空，无内置 `nacos/nacos`）：

```bash
# 1. 启动 Nacos（compose 已设 NACOS_AUTH_ENABLE=true）
docker compose up -d nacos

# 2. 一次性初始化管理员（Nacos 3.x 用 v3 端点，v1 已废弃返回 410）
curl -X POST 'http://localhost:8848/nacos/v3/auth/user/admin' -d 'password=SkyNacos@2026'

# 3. 之后客户端凭 .env 中 NACOS_USERNAME/NACOS_PASSWORD 自动登录
```

控制台地址为 **`http://localhost:8849/index.html`**（Nacos 3.x 控制台独立在容器 8080 端口，映射到主机 8849；旧的 `/nacos/` 路径已废弃）。首次登录用 `nacos` / `SkyNacos@2026`，导入 5 个配置文件：

| Data ID | 格式 | 来源 |
|---------|------|------|
| `sky-server-dev.yaml` | YAML | `nacos_config_example/nacos-config-sky-server-dev.yaml` |
| `sky-server-docker.yaml` | YAML | `nacos_config_example/nacos-config-sky-server-docker.yaml` (Docker 部署用) |
| `sky-server-flow-rules.json` | JSON | `nacos_config_example/nacos-config-sky-server-flow-rules.json` |
| `sky-server-degrade-rules.json` | JSON | `nacos_config_example/nacos-config-sky-server-degrade-rules.json` |
| `sky-gateway-flow-rules.json` | JSON | `nacos_config_example/nacos-config-sky-gateway-flow-rules.json` |

> 模板中敏感字段用 `${ENV_VAR:默认值}` 占位符，真实值只存于 gitignored 的 `.env`，导入 Nacos 后由应用环境变量解析。
> 配置内容持久化在 MySQL `nacos_config` 库，容器重建不丢失。认证开启后用 OpenAPI 写配置需带 `accessToken`（先 `POST /nacos/v3/auth/user/login` 获取）。

No Maven wrapper (`mvnw`) — use system `mvn`.

## Architecture

4-module Maven multi-module project. **Spring Boot 2.7.3 + Spring Cloud 2021.0.9 + Spring Cloud Alibaba 2021.0.6.1.**

| Module | Purpose |
|--------|---------|
| `sky-common` | Shared utils, constants, exceptions, JWT, OSS, properties (no controllers) |
| `sky-pojo` | DTOs, entities, VOs — data objects only, no logic |
| `sky-server` | The deployable business service: controllers, services, mappers, interceptors |
| `sky-gateway` | Spring Cloud Gateway (WebFlux), routes to `lb://sky-server`, CORS |

```
Nginx (:7999) → sky-gateway (:8081) → lb://sky-server (:8080)
                      ↓                        ↓
                   Nacos (:8848) ←── 注册 + 配置 + Sentinel 规则 ──┘
                      ↑                        │
              Sentinel Dashboard (:8858)       │
                                        Seata Server (:8091)
                                        分布式事务协调 (TC)
```

JWT authentication stays in **sky-server** (interceptors), not in the gateway. The gateway only does routing, CORS, and load balancing.

## Key Patterns

### Two-endpoint API structure
Each controller package has two facets:
- `controller/admin/` — management backend (employees, dishes, orders, reports). Interceptor: `JwtTokenAdminInterceptor`, login excluded at `/admin/employee/login`.
- `controller/user/` — WeChat mini-program (shopping cart, orders, address). Interceptor: `JwtTokenUserInterceptor`, login excluded at `/user/user/login` and `/user/shop/status`.

### User context via ThreadLocal
`BaseContext` holds the current user ID in a `ThreadLocal<Long>`. The JWT interceptor extracts it from the token and sets it. Do not rely on HTTP session.

### Result wrapper
`Result<T>` in sky-common wraps all API responses: `{code, msg, data}`. `PageResult` extends it for paginated list responses.

### Nacos Config
- All environment-specific config (datasource, redis, OSS, WeChat) lives in **Nacos Config Center**. Local `application-dev.yml` is intentionally empty (tombstone).
- Config Data ID: `sky-server-dev.yaml` (dev) / `sky-server-docker.yaml` (docker), group `DEFAULT_GROUP`. Must exist in Nacos before sky-server starts.
- sky-server uses `spring.config.import: optional:nacos:sky-server-${spring.profiles.active}.yaml` in `application.yml` to pull config from Nacos (Boot 2.x bootstrap.yml approach has been replaced).
- Nacos server-addr is resolved via `${NACOS_SERVER_ADDR:127.0.0.1:8848}` placeholder — set env var `NACOS_SERVER_ADDR` to override (used in Docker: `NACOS_SERVER_ADDR=nacos:8848`).
- Properties beans (`AliOssProperties`, `JwtProperties`, `WeChatProperties`) are annotated with `@RefreshScope` for hot reload.
- **Redis key注意**: Spring Boot 3.x 将配置键从 `spring.redis.*` 改为 `spring.data.redis.*`，Nacos 模板已同步更新。

### Sentinel 流控熔断 (#2 期已完成)
- **两层限流**：Gateway 层按路由资源 `sky-server-route` 全局 100 QPS；sky-server 层用 `@SentinelResource` 标 7 个核心写接口各 5 QPS。
- **熔断策略**：慢调用 (RT>300ms) + 异常比例 (>50%) 各 1 条 degrade 规则，窗口 10s，minRequestAmount=5。
- **受保护接口 resource 名**：`submitOrder`/`payOrder`/`userCancelOrder`/`confirmOrder`/`rejectOrder`/`adminCancelOrder`/`completeOrder` (在 `user.OrderController` 与 `admin.OrderController` 上)。
- **异常处理统一返回**：`com.sky.handler.SentinelBlockHandler` (sky-server, 静态方法, `@SentinelResource(blockHandlerClass=..., blockHandler=...)`) 和 `com.sky.gateway.config.SentinelGatewayConfiguration` (gateway, `BlockRequestHandler` 返回 HTTP 429 + Result JSON)。都返回 `Result{code:0, msg:...}` 与项目响应结构一致。
- **规则持久化到 Nacos**：4 份规则 JSON 在 Nacos，模板见 `docs/`：
  - `sky-server-flow-rules.json` / `sky-server-degrade-rules.json` (sky-server 通过 `spring.cloud.sentinel.datasource` 拉取)
  - `sky-gateway-flow-rules.json` (gateway 同上，`rule-type: gw-flow`)
  - sky-server 的 Sentinel 配置写在 `sky-server-dev.yaml` 的 `spring.cloud.sentinel.*`；gateway 写在 `sky-gateway/src/main/resources/application.yml`。
- **`@SentinelResource` 的 blockHandlerClass 方法必须 static**，否则 BlockException 被当 500 上抛。
- **Sentinel Dashboard 非强依赖**：规则在客户端启动时从 Nacos 拉到内存，Dashboard 仅用于实时监控与规则推送；Dashboard 进程不存在不影响限流功能。

### Seata 分布式事务 (#3 期已完成)
- **AT 模式**：SCA 2025.x + Seata 2.5.0 自动代理 DataSource，无需手动 `DataSourceProxy`/`SeataDataSourceConfig`。自动生成 undo_log（前后镜像），异常时 TC 协调自动回滚。
- **Seata Server**：Docker 部署 `seataio/seata-server:2.5.0`，`config.type: file` + `registry.type: nacos`。详见 `docker/seata-server/application.yml`。
- **⚠️ Seata 注册 Nacos 需带认证凭据**：Nacos 已开启认证，`docker/seata-server/application.yml` 的 `registry.nacos` **必须**配 `username`/`password`（用 `${NACOS_USERNAME:nacos}`/`${NACOS_PASSWORD:SkyNacos@2026}` 占位符，compose 已给 seata 加 `env_file: .env`）。**缺凭据时**：seata 向 Nacos 注册自身返回 `401 User not found` → `ServerRunner` 抛 `Server start failed` → 进程退出 → `restart:always` 崩溃循环。此坑仅在 seata 容器**重建/宿主重启**（触发全新注册）时暴露，旧容器靠注册残留可能掩盖。
- **客户端配置**：`seata.tx-service-group=sky-server-group` 在 `application.yml`；`@GlobalTransactional` 标注在 `OrderServiceImpl.submitOrder()` 和 `payment()`。
- **数据库表**：`seata` 库 4 张（global_table, branch_table, lock_table, distributed_lock）+ `sky_take_out` 库 1 张（undo_log，含 `ext` 列）。
- **与 Sentinel 分层**：`@SentinelResource` 在 Controller 层，`@GlobalTransactional` 在 Service 层，避免 AOP 代理链冲突。
- **Seata Server 非强依赖**：Seata Server 不可用时，`@Transactional` 仍可保证本地事务（但全局事务降级为本地事务）。
- **healthcheck 探针别写字节**：seata（及 namesrv/broker）的 compose healthcheck 用 `bash -c '< /dev/tcp/localhost/8091'`（只读建连、零字节），**不要**用 `echo > /dev/tcp/...`——`echo` 写入的换行符 `\n`(0x0A) 会被 Seata TC 的 `ProtocolDetectHandler` 当成未知协议首字节，每个探测周期打一条 `Can not recognize protocol ... preface = [10]` ERROR 刷屏（探活其实成功，纯噪声）。

## Constraints & Warnings

- **Gateway must NOT depend on `spring-boot-starter-web`** (Tomcat) — it's WebFlux/Netty only. Gateway module is self-contained; it does not depend on sky-server.
- **RocketMQ** (`#4 期`): 已替代 WebSocket。`OrderServiceImpl` 通过 `RocketMQProducerService` 异步发送订单通知消息到 Topic `order-notification`；`RocketMQConsumerService` 消费并记录日志。消息发送失败不阻断主流程。NameServer 端口 9876，Broker 10911，Console 8082。
- **Nacos startup order**: Nacos Server must be running with configs created before sky-server starts, or `spring.config.import` will fail to pull Nacos config.
- **Windows JVM `file.encoding` pitfall**: Windows 原生 JDK 17 默认编码为 GBK，而 Nacos 中的 YAML 配置为 UTF-8，编码不一致会导致 YAML 解析失败。**仅 Windows 原生 JDK 需要**在启动前设置 `JAVA_TOOL_OPTIONS`（Git Bash / PowerShell: `$env:`），Linux / WSL / macOS 默认已是 UTF-8，无需此步骤。`application-dev.yml` in the repo is intentionally empty — content lives in Nacos.
- **JDK 24 is incompatible** — always verify `java -version` before Maven commands.
- **Seata requires JDK module opens**: `sky-server/pom.xml` 的 `spring-boot-maven-plugin` 已配置 `--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED`。生产环境 `java -jar` 部署时需手动添加这两个 JVM 参数，否则 Seata 反射访问 `java.lang.reflect.Proxy.h` 会抛 `InaccessibleObjectException`。
- **Druid starter**: 使用 `druid-spring-boot-3-starter`（Boot 3.x 兼容版），非旧版 `druid-spring-boot-starter`（后者在 Boot 3.x 下自动配置不生效）。
- **Nacos client**: 版本由 SCA 2025.0.0.0 BOM 管理（**3.0.3**），与 Nacos Server v3.0.3 对齐，**不再 pin**。此前"覆盖为 2.5.1 避免 403"的做法已废弃——那个 403/500 (`Handle API Compatibility failed`) 的真实根因是 Nacos 服务端**关闭了认证**导致 v1 登录端点未激活，而非客户端版本不匹配。**开启认证**后登录正常，客户端版本回归 BOM 默认即可。
- **Nacos 认证**: 服务端 `NACOS_AUTH_ENABLE=true`。客户端凭据由 `.env` 的 `NACOS_USERNAME`/`NACOS_PASSWORD` 提供（`application.yml` 与两份 Nacos 模板中的 Sentinel datasource 均用 `${NACOS_PASSWORD:...}` 占位符）。**本地 dev 运行**（非 Docker，不加载 `.env`）需先 `export NACOS_PASSWORD=SkyNacos@2026` 再 `mvn spring-boot:run`，否则登录 Nacos 失败。
- Sensitive config (credentials, AKSK) belongs in Nacos with `${ENV_VAR}` placeholders or the local `docs/` template — never committed. `application-dev.yml` and `docs/` are gitignored.
- `spring-cloud-starter-loadbalancer` is an **explicit** dependency in sky-gateway (optional in Gateway 3.1.x, but `lb://` breaks without it).
- **`spring-cloud-alibaba-sentinel-gateway` 适配包必须显式声明** in sky-gateway — the starter `spring-cloud-starter-alibaba-sentinel` does NOT pull it transitively. Without it, `com.alibaba.csp.sentinel.adapter.gateway.sc.callback.*` classes are missing and Gateway 限流编译失败.
- **Seata Server 启动顺序**：Seata Server 应在 sky-server 之前启动（否则 `@GlobalTransactional` 事务会降级为本地事务）。启动顺序：Nacos → MySQL → Seata Server → sky-server → sky-gateway。

## Dependencies Managed by BOM

Do not specify versions for Spring Cloud, Spring Cloud Alibaba, or their transitive deps — they are managed by:
- `org.springframework.cloud:spring-cloud-dependencies:2021.0.9`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2021.0.6.1`

## TODO: Future Phases

See `.others/后续阶段TODO.md` for the roadmap. Completed:
- #1 期 (Gateway + Nacos) ✅
- #2 期 (Sentinel 流控熔断) ✅
- #3 期 (Seata 分布式事务) ✅
- #4 期 (RocketMQ 消息队列) ✅
- #5 期 (Docker 容器化 — 基础设施 + 应用) ✅

Remaining: K8s 编排部署 (#6).
