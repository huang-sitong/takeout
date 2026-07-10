# CLAUDE.md

Guidance for Claude Code working in this repo. **Spring Boot 3.5.0 + Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0**（原 Boot 2.7.3 已升级；`javax.*`→`jakarta.*`，JDK 17 起步）。

## Environment

- **JDK 17** required — JDK 24 breaks Lombok（`@Data`/`@Builder` 注解处理失败）；`JAVA_HOME` 必须指向 JDK 17，跑 Maven 前先 `java -version` 核对。
- Maven 3.6+（无 `mvnw`，用系统 `mvn`）、MySQL 5.7+（Windows 原生服务）、Docker Desktop。

## Build & Run

```bash
mvn install -DskipTests                        # build all 8 modules
mvn install -DskipTests -pl sky-order-service -am  # 单服务 + 依赖

# ★ 仅 Windows 原生 JDK：Nacos YAML 为 UTF-8，需设 JVM 编码否则解析失败（Linux/WSL/macOS 免此步；PowerShell 用 $env:）
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# 本地开发（逐个启动）—— 先启动基础设施
docker compose up -d nacos redis seata namesrv broker sentinel
mvn -pl sky-admin-service spring-boot:run   # :8082
mvn -pl sky-gateway spring-boot:run         # :8081

# 生产 JAR 需手动带 --add-opens（否则 Seata 反射抛 InaccessibleObjectException）：
# java --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar sky-order-service/target/sky-order-service-1.0-SNAPSHOT.jar

# Docker 一键部署（推荐）：先打 JAR 再起容器
mvn package -DskipTests && docker compose up -d --build
docker compose ps                     # 状态（12 容器）
docker compose logs -f sky-order-service
docker compose down                   # 停止
```

> **本地 dev**（不加载 `.env`）运行前需 `export NACOS_PASSWORD=SkyNacos@2026`，否则登录 Nacos 失败。

## Infrastructure (Docker Compose)

### 端口与凭据

| Service | Port | Credentials |
|---------|------|-------------|
| Nacos | `:8848`(API/gRPC 9848/9849)，控制台 `:8849`(容器 8080) | 认证已开启 `nacos`/`SkyNacos@2026` |
| Redis | `:6379` | `123456` |
| Seata | `:8091`(TC)、`:7091`(console) | console `seata`/`seata` |
| Sentinel Dashboard | `:8858` | `sentinel`/`sentinel` |
| RocketMQ | NameServer `:9876`、Broker `:10911`(VIP 10909)、Console `:8082` | — |
| MySQL | `:3306`（Windows 原生） | root/`123456` |
| sky-gateway | `:8081` | — |
| sky-admin-service | `:8092`→容器`:8082`（宿主机端口回避 RocketMQ Console） | — |
| sky-user-service | `:8083` | — |
| sky-menu-service | `:8084` | — |
| sky-cart-service | `:8085` | — |
| sky-order-service | `:8086` | — |

容器镜像：nacos `v3.0.3`（单机，MySQL 后端 `nacos_config` 库，`NACOS_AUTH_ENABLE=true`，控制台 8080→主机 8849）、redis `7-alpine`(AOF)、seata `2.5.0`、sentinel 本地构建 `1.8.9`、rocketmq `apache/rocketmq:5.3.1`(namesrv+broker) + `apacherocketmq/rocketmq-dashboard`。sky-gateway 及各业务服务本地构建，`SPRING_PROFILES_ACTIVE=docker`。

### Nacos 初始化（认证已开启）

Nacos ≥2.4 无内置 `nacos/nacos`，`users`/`roles` 默认空，首次部署需初始化管理员：

```bash
docker compose up -d nacos
# Nacos 3.x 用 v3 端点，v1 已废弃返回 410
curl -X POST 'http://localhost:8848/nacos/v3/auth/user/admin' -d 'password=SkyNacos@2026'
```

控制台 **`http://localhost:8849/index.html`**（3.x 控制台独立在容器 8080→主机 8849，旧 `/nacos/` 已废弃），`nacos`/`SkyNacos@2026` 登录后导入 12 份配置（源于 `nacos_config_example/`）：

| Data ID | 用途 |
|---------|------|
| `sky-admin-service-dev.yaml` | admin 服务本地开发 |
| `sky-admin-service-docker.yaml` | admin 服务 Docker 部署 |
| `sky-user-service-dev.yaml` | user 服务本地开发 |
| `sky-user-service-docker.yaml` | user 服务 Docker 部署 |
| `sky-menu-service-dev.yaml` | menu 服务本地开发 |
| `sky-menu-service-docker.yaml` | menu 服务 Docker 部署 |
| `sky-cart-service-dev.yaml` | cart 服务本地开发 |
| `sky-cart-service-docker.yaml` | cart 服务 Docker 部署 |
| `sky-order-service-dev.yaml` | order 服务本地开发 |
| `sky-order-service-docker.yaml` | order 服务 Docker 部署 |
| `sky-order-service-flow-rules.json` | Sentinel 流控规则 |
| `sky-order-service-degrade-rules.json` | Sentinel 熔断规则 |
| `sky-gateway-flow-rules.json` | Gateway 限流规则 |

> 敏感字段用 `${ENV_VAR:默认值}` 占位符，真实值只存 gitignored 的 `.env`，应用启动时用容器环境变量解析。配置持久化在 MySQL `nacos_config` 库，容器重建不丢。OpenAPI 写配置需带 `accessToken`（先 `POST /nacos/v3/auth/user/login`）。

## Architecture

8-module Maven 多模块（Phase 8 微服务拆分后）：

| Module | Purpose |
|--------|---------|
| `sky-common` | 工具/常量/异常/JWT/OSS/properties/GlobalExceptionHandler（无 controller） |
| `sky-pojo` | DTO/entity/VO（纯数据，按 5 个限界上下文拆分子包） |
| `sky-feign-common` | OpenFeign 公共配置：`FeignConfig`（Jackson）、`FeignInterceptor`（Header 透传）、`FeignErrorDecoder` |
| `sky-admin-service` | 员工管理（端口 8082，DB `sky_admin_db`） |
| `sky-user-service` | 用户 + 地址（端口 8083，DB `sky_user_db`） |
| `sky-menu-service` | 分类/菜品/套餐 + OSS（端口 8084，DB `sky_menu_db`） |
| `sky-cart-service` | 购物车（端口 8085，DB `sky_cart_db`，依赖 menu-service） |
| `sky-order-service` | 订单/报表/店铺 + MQ + Seata（端口 8086，DB `sky_order_db`，依赖所有其他服务） |
| `sky-gateway` | Spring Cloud Gateway(WebFlux)，路由 + JWT 认证 + CORS |

```
                               ┌────────────────────┐
                               │   sky-gateway      │
                               │      :8081          │
                               │  JWT 认证 + 限流    │
                               └───┬──┬──┬──┬──┬────┘
                                   │  │  │  │  │
              ┌────────────────────┼──┼──┼──┼──┼───────────┐
              │                    │  │  │  │  │            │
              ▼                    ▼  ▼  ▼  ▼  ▼            │
   ┌─────────────────┐  ┌──────────┬──┬──┬──┬───────────┐  │
   │ sky-admin-service│  │ sky-user │sky-menu│sky-cart │   │
   │    :8082         │  │ :8083    │ :8084  │ :8085   │   │
   │   employee       │  │ user     │category│cart     │   │
   └────────┬─────────┘  │ address  │dish    │         │   │
            │             └────┬─────┘setmeal └───┬─────┘   │
            │                  │       │          │         │
            │    Feign 调用     ▼       ▼          ▼         │
            └──────────────►┌──────────────────────────┐    │
                            │    sky-order-service     │◄───┘
                            │         :8086            │
                            │  order/report/shop/Seata │
                            │      RocketMQ/MQ         │
                            └──────┬───────┬───────────┘
                                   │       │
                                   ▼       ▼
                              ┌─────────┐ ┌───────────┐
                              │  Seata  │ │  RocketMQ  │
                              │  :8091  │ │:9876/10911│
                              └─────────┘ └───────────┘
```

JWT 认证在 **sky-gateway**（`JwtAuthGlobalFilter`），校验后通过 `X-User-Id` / `X-User-Role` Header 注入下游；各服务的 `UserContextFilter` 读取 Header → `BaseContext`。登录接口的 JWT 签发在 `sky-admin-service`（员工）和 `sky-user-service`（用户）。

## Key Patterns

- **双端结构**：`controller/admin/`（管理端）与 `controller/user/`（小程序端），JWT 校验统一在 Gateway `JwtAuthGlobalFilter` 完成（白名单放行 `/admin/employee/login`、`/user/user/login`、`/user/shop/status`、`/ws/`）。
- **用户上下文**：`BaseContext` 用 `ThreadLocal<Long>` 存当前用户 id + `ThreadLocal<String>` 存角色（ADMIN/USER）。Gateway 校验 JWT 后注入 `X-User-Id` / `X-User-Role` Header，各服务的 `UserContextFilter` 读取并 set，finally 块 `removeAll()` 防泄漏。
- **统一返回**：`Result<T>{code,msg,data}`（sky-common），`PageResult` 扩展分页。
- **Nacos Config**：环境配置（datasource/redis/OSS/WeChat）全在 Nacos，本地 `application-dev.yml` 空壳。5 服务各配 `dev` + `docker` 两份 Nacos YAML，group `DEFAULT_GROUP`。`application.yml` 用 `spring.config.import: optional:nacos:sky-<service>-${spring.profiles.active}.yaml` 拉取；server-addr 用 `${NACOS_SERVER_ADDR:127.0.0.1:8848}`（Docker 注入 `nacos:8848`）。`AliOssProperties`/`JwtProperties`/`WeChatProperties` 带 `@RefreshScope` 热更新。**Redis key**：Boot 3.x 从 `spring.redis.*` 改 `spring.data.redis.*`（旧键被静默忽略）。
- **OpenFeign 跨服务调用**：`FeignInterceptor` 从 `RequestContextHolder` 获取当前请求的 `X-User-Id`/`X-User-Role` Header，注入 Feign 请求，确保用户上下文跨服务传递。`FeignErrorDecoder` 将 Feign 错误转业务异常。order-service 通过 Feign 调用 user-service（用户/地址）、cart-service（购物车）、menu-service（菜品/套餐）。
- **支付绕过**：`OrderServiceImpl.payment()` 为测试友好实现，直接构造 `ORDERPAID` 伪响应 → `paySuccess()`，不调用微信支付 API。上线需替换为真实 `WeChatPayUtil` 调用。

### Sentinel 流控熔断（#2）
- **两层限流**：Gateway 按路由资源全局 100 QPS；sky-order-service 用 `@SentinelResource` 标 7 个核心写接口各 5 QPS，resource 名 `submitOrder`/`payOrder`/`userCancelOrder`/`confirmOrder`/`rejectOrder`/`adminCancelOrder`/`completeOrder`。
- **熔断**：慢调用(RT>300ms) + 异常比例(>50%) 各 1 条 degrade 规则，窗口 10s，minRequestAmount=5。
- **统一返回**：`com.sky.handler.SentinelBlockHandler`（sky-order-service，**方法必须 static**，否则 BlockException 当 500 上抛）区分 Flow/Degrade；gateway `SentinelGatewayConfiguration` 返回 HTTP 429 + `Result{code:0,msg:...}`。
- **规则持久化 Nacos**：13 份 JSON（模板 `nacos_config_example/`）客户端启动拉到内存；**Dashboard 非强依赖**，仅用于监控/推送。

### Seata 分布式事务（#3）
- **AT 模式**：SCA 2025 + Seata 2.5.0 **自动代理 DataSource**（无需手动 `DataSourceProxy`），自动 undo_log，异常 TC 协调回滚。Server：`seataio/seata-server:2.5.0`，`config.type: file` + `registry.type: nacos`（`docker/seata-server/application.yml`）。
- **⚠️ Seata 注册 Nacos 需带凭据**：认证已开启，`registry.nacos` **必须**配 `username`/`password`（`${NACOS_USERNAME:nacos}`/`${NACOS_PASSWORD:SkyNacos@2026}`，compose 给 seata 加了 `env_file: .env`）。缺凭据 → 注册返回 `401 User not found` → `ServerRunner` 抛 `Server start failed` → 进程退出 → `restart:always` 崩溃循环。**仅在 seata 容器重建 / 宿主重启时暴露**，旧容器靠注册残留掩盖。
- **⚠️ healthcheck 探针别写字节**：seata/namesrv/broker 的 compose healthcheck 用 `bash -c '< /dev/tcp/localhost/8091'`（只读建连、零字节），**不要** `echo > /dev/tcp/...`——`echo` 的换行 `\n`(0x0A) 被 Seata TC `ProtocolDetectHandler` 当未知协议首字节，每探测周期打一条 `Can not recognize protocol ... preface = [10]` ERROR 刷屏（探活其实成功，纯噪声）。
- **客户端**：`seata.tx-service-group=sky-order-group`（`application.yml`），`@GlobalTransactional` 在 `OrderServiceImpl.submitOrder()`/`payment()`（Service 层，与 Controller 层 `@SentinelResource` 分层避 AOP 冲突）。**表**：`seata` 库 4 张 + `sky_order_db` 库 undo_log（含 `ext` 列）。**非强依赖**：Server 不可用时降级为本地事务。**启动顺序**：Nacos → MySQL → Seata → sky-order-service → 其他服务 → sky-gateway。

## Constraints & Warnings

- **Gateway 不得依赖 `spring-boot-starter-web`**（Tomcat）——它是 WebFlux/Netty，自包含，不依赖任何业务服务。`spring-cloud-starter-loadbalancer` 必须显式声明（否则 `lb://` 失效）；`spring-cloud-alibaba-sentinel-gateway` 适配包必须显式声明（starter 不传递，缺则网关限流编译失败）。
- **RocketMQ（#4）**：已替代 WebSocket。`OrderServiceImpl` 经 `RocketMQProducerService` 异步发订单通知到 Topic `order-notification`，`RocketMQConsumerService` 消费记日志；发送失败不阻断主流程。
- **Druid starter**：用 `druid-spring-boot-3-starter`（Boot 3.x 兼容版），非旧版 `druid-spring-boot-starter`（后者自动配置在 Boot 3.x 不生效，会 fallback Hikari）。
- **Nacos client**：版本由 SCA 2025 BOM 管理（**3.0.3**），对齐 server v3.0.3，**不再 pin**。此前"pin 2.5.1 避免 403"已废弃——`Handle API Compatibility failed` 真实根因是服务端**关闭认证**致 v1 登录端点未激活，非客户端版本；开启认证后正常。
- **Nacos 认证**：`NACOS_AUTH_ENABLE=true`，客户端凭据由 `.env` 的 `NACOS_USERNAME`/`NACOS_PASSWORD` 提供（`application.yml` 与 Nacos 模板的 Sentinel datasource 用 `${NACOS_PASSWORD:...}` 占位符）。
- **敏感配置**：credentials/AKSK 放 Nacos 用 `${ENV_VAR}` 占位符或本地模板，绝不提交；`application-dev.yml` 与 `docs/` 已 gitignore。
- **Windows 编码坑**：Windows 原生 JDK 17 默认 GBK，Nacos UTF-8 YAML 会解析失败——仅 Windows 需启动前设 `JAVA_TOOL_OPTIONS`（见 Build & Run）。
- **每个微服务需 `@ComponentScan(basePackages = "com.sky")`**：`GlobalExceptionHandler` 已移至 `sky-common`，各服务必须显式扫描才能发现该 Bean，否则业务异常直接 500。
- **`@RestController` 需显式命名**：同路径的 Controller 在多个服务中可能共处一个 classpath（如 admin/ShopController 和 user/ShopController 都在 order-service），一个不加 `@RestController("beanName")` → Spring 容器冲突 → 启动失败。
- **Feign 客户端需 `spring-cloud-starter-loadbalancer`**：cart/order 服务的 `pom.xml` 必须显式声明，否则 `No Feign Client for loadBalancing defined`。
- **address_book 更新需省市区字段**：`AddressBookMapper.xml` 的 `update` SQL 必须包含 `province_code/name`、`city_code/name`、`district_code/name` 6 个字段（原 bug，已修复）。

## Dependencies Managed by BOM

不要给 Spring Cloud / SCA 及其传递依赖指定版本，由 BOM 管理：
- Spring Boot `3.5.0`（父 POM 统一）
- `org.springframework.cloud:spring-cloud-dependencies:2025.0.0`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.0.0.0`

## TODO: Future Phases

见 `.others/微服务深化改造TODO.md`。已完成 #1 Gateway+Nacos、#2 Sentinel、#3 Seata、#4 RocketMQ、#5 Docker 容器化、#7 JWT 认证中心化、#8 微服务拆分（5 服务 + 全链路测试通过）；剩 #9 WebSocket 实时推送、#6 K8s 编排。
