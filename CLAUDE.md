# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment

- **JDK 17** required — JDK 24 breaks Lombok (`@Data`/`@Builder` annotation processing fails). `JAVA_HOME` must point to JDK 17.
- Maven 3.6+, MySQL 5.7+, Redis 6.0+, Nacos 2.2.3 (Docker standalone), Sentinel Dashboard 1.8.6 (WSL jar，port 8858，脚本见 `docker/sentinel-dashboard/`)

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

# Start sky-server as JAR (production):
# java --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar

# Dependency tree
mvn dependency:tree -pl sky-server
```

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
- All environment-specific config (datasource, redis, OSS, WeChat) lives in **Nacos Config Center**, NOT in `application-dev.yml` (that file is intentionally empty and gitignored).
- Config Data ID: `sky-server-dev.yaml`, group `DEFAULT_GROUP`. Must exist in Nacos before sky-server starts.
- sky-server uses `bootstrap.yml` to connect to Nacos (requires the explicit `spring-cloud-starter-bootstrap` dependency — Spring Boot 2.7.x disables bootstrap by default).
- Properties beans (`AliOssProperties`, `JwtProperties`, `WeChatProperties`) are annotated with `@RefreshScope` for hot reload.

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
- **AT 模式**：`DataSourceProxy` 包装 Druid DataSource，自动生成 undo_log（前后镜像），异常时 TC 协调自动回滚。
- **Seata Server**：Docker 部署 `seataio/seata-server:1.5.2`，store.mode=db（数据库 `seata`），注册到 Nacos（group: `SEATA_GROUP`）。
- **客户端配置**：`seata.tx-service-group=sky-server-group` 在 `bootstrap.yml`；`@GlobalTransactional` 标注在 `OrderServiceImpl.submitOrder()` 和 `payment()`。
- **数据库表**：`seata` 库 4 张（global_table, branch_table, lock_table, distributed_lock）+ `sky_take_out` 库 1 张（undo_log，含 `ext` 列）。
- **与 Sentinel 分层**：`@SentinelResource` 在 Controller 层，`@GlobalTransactional` 在 Service 层，避免 AOP 代理链冲突。
- **Seata Server 非强依赖**：Seata Server 不可用时，`@Transactional` 仍可保证本地事务（但全局事务降级为本地事务）。

## Constraints & Warnings

- **Gateway must NOT depend on `spring-boot-starter-web`** (Tomcat) — it's WebFlux/Netty only. Gateway module is self-contained; it does not depend on sky-server.
- **WebSocket is disabled** (`WebSocketConfiguration`, `WebSocketServer`, and `OrderServiceImpl` calls are commented out). It will be replaced by RocketMQ in a future phase. Do not re-enable it.
- **Nacos startup order**: Nacos Server must be running with configs created before sky-server starts, or bootstrap will fail.
- **Windows JVM `file.encoding` pitfall**: Windows 原生 JDK 17 默认编码为 GBK，而 Nacos 中的 YAML 配置为 UTF-8，编码不一致会导致 YAML 解析失败。**仅 Windows 原生 JDK 需要**在启动前设置 `JAVA_TOOL_OPTIONS`（Git Bash / PowerShell: `$env:`），Linux / WSL / macOS 默认已是 UTF-8，无需此步骤。`application-dev.yml` in the repo is intentionally empty — content lives in Nacos.
- **JDK 24 is incompatible** — always verify `java -version` before Maven commands.
- **Seata requires JDK module opens**: `sky-server/pom.xml` 的 `spring-boot-maven-plugin` 已配置 `--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED`。生产环境 `java -jar` 部署时需手动添加这两个 JVM 参数，否则 Seata 反射访问 `java.lang.reflect.Proxy.h` 会抛 `InaccessibleObjectException`。
- Sensitive config (credentials, AKSK) belongs in Nacos or the local `docs/` template — never committed. `application-dev.yml` and `docs/` are gitignored.
- `spring-cloud-starter-loadbalancer` is an **explicit** dependency in sky-gateway (optional in Gateway 3.1.x, but `lb://` breaks without it).
- **`spring-cloud-alibaba-sentinel-gateway` 适配包必须显式声明** in sky-gateway — the starter `spring-cloud-starter-alibaba-sentinel` does NOT pull it transitively. Without it, `com.alibaba.csp.sentinel.adapter.gateway.sc.callback.*` classes are missing and Gateway 限流编译失败.
- **Seata Server 启动顺序**：Seata Server 应在 sky-server 之前启动（否则 `@GlobalTransactional` 事务会降级为本地事务）。启动顺序：Nacos → MySQL → Seata Server → sky-server → sky-gateway。
- **DataSourceProxy 不可重复代理**：`SeataDataSourceConfig` 用 `@Primary` 包装 Druid DataSource，确保 MyBatis 使用代理后的连接；不要在别处再次包装 DataSourceProxy。

## Dependencies Managed by BOM

Do not specify versions for Spring Cloud, Spring Cloud Alibaba, or their transitive deps — they are managed by:
- `org.springframework.cloud:spring-cloud-dependencies:2021.0.9`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2021.0.6.1`

## TODO: Future Phases

See `.others/后续阶段TODO.md` for the roadmap. Completed:
- #1 期 (Gateway + Nacos) ✅
- #2 期 (Sentinel 流控熔断) ✅
- #3 期 (Seata 分布式事务) ✅

Remaining: RocketMQ (#4) → Docker (#5) → K8s (#6).
