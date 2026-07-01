# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment

- **JDK 17** required — JDK 24 breaks Lombok (`@Data`/`@Builder` annotation processing fails). `JAVA_HOME` must point to JDK 17.
- Maven 3.6+, MySQL 5.7+, Redis 6.0+, Nacos 2.2.3 (Docker standalone)

## Build & Run

```bash
# Build all modules (skip tests)
mvn install -DskipTests

# Build a module and its dependencies
mvn install -DskipTests -pl sky-server -am

# Start sky-server (port 8080)
mvn -pl sky-server spring-boot:run

# Start sky-gateway (port 8081)
mvn -pl sky-gateway spring-boot:run

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
                   Nacos (:8848) ←── 注册 + 配置 ──┘
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

## Constraints & Warnings

- **Gateway must NOT depend on `spring-boot-starter-web`** (Tomcat) — it's WebFlux/Netty only. Gateway module is self-contained; it does not depend on sky-server.
- **WebSocket is disabled** (`WebSocketConfiguration`, `WebSocketServer`, and `OrderServiceImpl` calls are commented out). It will be replaced by RocketMQ in a future phase. Do not re-enable it.
- **Nacos startup order**: Nacos Server must be running with configs created before sky-server starts, or bootstrap will fail.
- **JDK 24 is incompatible** — always verify `java -version` before Maven commands.
- Sensitive config (credentials, AKSK) belongs in Nacos or the local `docs/` template — never committed. `application-dev.yml` and `docs/` are gitignored.
- `spring-cloud-starter-loadbalancer` is an **explicit** dependency in sky-gateway (optional in Gateway 3.1.x, but `lb://` breaks without it).

## Dependencies Managed by BOM

Do not specify versions for Spring Cloud, Spring Cloud Alibaba, or their transitive deps — they are managed by:
- `org.springframework.cloud:spring-cloud-dependencies:2021.0.9`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2021.0.6.1`

## TODO: Future Phases

See `.others/后续阶段TODO.md` for the roadmap: Sentinel (#2) → Seata (#3) → RocketMQ (#4) → Docker (#5) → K8s (#6).
