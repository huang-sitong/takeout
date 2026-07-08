# CLAUDE.md

Guidance for Claude Code working in this repo. **Spring Boot 3.5.0 + Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0**（原 Boot 2.7.3 已升级；`javax.*`→`jakarta.*`，JDK 17 起步）。

## Environment

- **JDK 17** required — JDK 24 breaks Lombok（`@Data`/`@Builder` 注解处理失败）；`JAVA_HOME` 必须指向 JDK 17，跑 Maven 前先 `java -version` 核对。
- Maven 3.6+（无 `mvnw`，用系统 `mvn`）、MySQL 5.7+（Windows 原生服务）、Docker Desktop。

## Build & Run

```bash
mvn install -DskipTests                        # build all
mvn install -DskipTests -pl sky-server -am     # 单模块 + 依赖
mvn dependency:tree -pl sky-server

# ★ 仅 Windows 原生 JDK：Nacos YAML 为 UTF-8，需设 JVM 编码否则解析失败（Linux/WSL/macOS 免此步；PowerShell 用 $env:）
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

mvn -pl sky-server spring-boot:run    # :8080（--add-opens for Seata 已配在 pom 的 spring-boot-maven-plugin）
mvn -pl sky-gateway spring-boot:run   # :8081

# 生产 JAR 需手动带 --add-opens（否则 Seata 反射抛 InaccessibleObjectException）：
# java --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar

# Docker 一键部署：先打 JAR 再起容器
mvn package -DskipTests && docker compose up -d --build
docker compose ps                     # 状态
docker compose logs -f sky-server
docker compose down                   # 停止
```

> Docker 部署 sky-server 用 Nacos Data ID `sky-server-docker.yaml`（非本地 `sky-server-dev.yaml`），首次需在 Nacos 导入，模板 `nacos_config_example/nacos-config-sky-server-docker.yaml`。
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

容器镜像：nacos `v3.0.3`（单机，MySQL 后端 `nacos_config` 库，`NACOS_AUTH_ENABLE=true`，控制台 8080→主机 8849）、redis `7-alpine`(AOF)、seata `2.5.0`、sentinel 本地构建 `1.8.9`、rocketmq `apache/rocketmq:5.3.1`(namesrv+broker) + `apacherocketmq/rocketmq-dashboard`。sky-server / sky-gateway 本地构建，`SPRING_PROFILES_ACTIVE=docker`。

### Nacos 初始化（认证已开启）

Nacos ≥2.4 无内置 `nacos/nacos`，`users`/`roles` 默认空，首次部署需初始化管理员：

```bash
docker compose up -d nacos
# Nacos 3.x 用 v3 端点，v1 已废弃返回 410
curl -X POST 'http://localhost:8848/nacos/v3/auth/user/admin' -d 'password=SkyNacos@2026'
```

控制台 **`http://localhost:8849/index.html`**（3.x 控制台独立在容器 8080→主机 8849，旧 `/nacos/` 已废弃），`nacos`/`SkyNacos@2026` 登录后导入 5 份配置（源于 `nacos_config_example/`）：`sky-server-dev.yaml`、`sky-server-docker.yaml`、`sky-server-flow-rules.json`、`sky-server-degrade-rules.json`、`sky-gateway-flow-rules.json`。

> 敏感字段用 `${ENV_VAR:默认值}` 占位符，真实值只存 gitignored 的 `.env`，应用启动时用容器环境变量解析。配置持久化在 MySQL `nacos_config` 库，容器重建不丢。OpenAPI 写配置需带 `accessToken`（先 `POST /nacos/v3/auth/user/login`）。

## Architecture

4-module Maven 多模块：

| Module | Purpose |
|--------|---------|
| `sky-common` | 工具/常量/异常/JWT/OSS/properties（无 controller） |
| `sky-pojo` | DTO/entity/VO（纯数据） |
| `sky-server` | 可部署业务服务：controller/service/mapper/interceptor |
| `sky-gateway` | Spring Cloud Gateway(WebFlux)，路由 `lb://sky-server`，CORS |

```
Nginx(:7999) → sky-gateway(:8081) → lb://sky-server(:8080)
                    ↓                       ↓
                 Nacos(:8848) ← 注册 + 配置 + Sentinel 规则
       Sentinel Dashboard(:8858)   Seata Server(:8091, TC)
```

JWT 认证在 **sky-server**（拦截器），网关只做路由 / CORS / 负载均衡。

## Key Patterns

- **双端结构**：`controller/admin/`（管理端，`JwtTokenAdminInterceptor`，放行 `/admin/employee/login`）与 `controller/user/`（小程序，`JwtTokenUserInterceptor`，放行 `/user/user/login`、`/user/shop/status`）。
- **用户上下文**：`BaseContext` 用 `ThreadLocal<Long>` 存当前用户 id，JWT 拦截器从 token 提取并 set，不依赖 HTTP session。
- **统一返回**：`Result<T>{code,msg,data}`（sky-common），`PageResult` 扩展分页。
- **Nacos Config**：环境配置（datasource/redis/OSS/WeChat）全在 Nacos，本地 `application-dev.yml` 空壳。Data ID `sky-server-${profile}.yaml`，group `DEFAULT_GROUP`，sky-server 启动前必须存在。`application.yml` 用 `spring.config.import: optional:nacos:sky-server-${spring.profiles.active}.yaml` 拉取（替代 Boot 2.x bootstrap.yml）；server-addr 用 `${NACOS_SERVER_ADDR:127.0.0.1:8848}`（Docker 注入 `nacos:8848`）。`AliOssProperties`/`JwtProperties`/`WeChatProperties` 带 `@RefreshScope` 热更新。**Redis key**：Boot 3.x 从 `spring.redis.*` 改 `spring.data.redis.*`（旧键被静默忽略）。

### Sentinel 流控熔断（#2）
- **两层限流**：Gateway 按路由资源 `sky-server-route` 全局 100 QPS；sky-server 用 `@SentinelResource` 标 7 个核心写接口各 5 QPS，resource 名 `submitOrder`/`payOrder`/`userCancelOrder`/`confirmOrder`/`rejectOrder`/`adminCancelOrder`/`completeOrder`。
- **熔断**：慢调用(RT>300ms) + 异常比例(>50%) 各 1 条 degrade 规则，窗口 10s，minRequestAmount=5。
- **统一返回**：`com.sky.handler.SentinelBlockHandler`（sky-server，**方法必须 static**，否则 BlockException 当 500 上抛）区分 Flow/Degrade；gateway `SentinelGatewayConfiguration` 返回 HTTP 429 + `Result{code:0,msg:...}`。
- **规则持久化 Nacos**：4 份 JSON（模板 `nacos_config_example/`）客户端启动拉到内存；**Dashboard 非强依赖**，仅用于监控/推送。

### Seata 分布式事务（#3）
- **AT 模式**：SCA 2025 + Seata 2.5.0 **自动代理 DataSource**（无需手动 `DataSourceProxy`），自动 undo_log，异常 TC 协调回滚。Server：`seataio/seata-server:2.5.0`，`config.type: file` + `registry.type: nacos`（`docker/seata-server/application.yml`）。
- **⚠️ Seata 注册 Nacos 需带凭据**：认证已开启，`registry.nacos` **必须**配 `username`/`password`（`${NACOS_USERNAME:nacos}`/`${NACOS_PASSWORD:SkyNacos@2026}`，compose 给 seata 加了 `env_file: .env`）。缺凭据 → 注册返回 `401 User not found` → `ServerRunner` 抛 `Server start failed` → 进程退出 → `restart:always` 崩溃循环。**仅在 seata 容器重建 / 宿主重启时暴露**，旧容器靠注册残留掩盖。
- **⚠️ healthcheck 探针别写字节**：seata/namesrv/broker 的 compose healthcheck 用 `bash -c '< /dev/tcp/localhost/8091'`（只读建连、零字节），**不要** `echo > /dev/tcp/...`——`echo` 的换行 `\n`(0x0A) 被 Seata TC `ProtocolDetectHandler` 当未知协议首字节，每探测周期打一条 `Can not recognize protocol ... preface = [10]` ERROR 刷屏（探活其实成功，纯噪声）。
- **客户端**：`seata.tx-service-group=sky-server-group`（`application.yml`），`@GlobalTransactional` 在 `OrderServiceImpl.submitOrder()`/`payment()`（Service 层，与 Controller 层 `@SentinelResource` 分层避 AOP 冲突）。**表**：`seata` 库 4 张 + `sky_take_out` 库 undo_log（含 `ext` 列）。**非强依赖**：Server 不可用时降级为本地事务。**启动顺序**：Nacos → MySQL → Seata → sky-server → sky-gateway。

## Constraints & Warnings

- **Gateway 不得依赖 `spring-boot-starter-web`**（Tomcat）——它是 WebFlux/Netty，自包含，不依赖 sky-server。`spring-cloud-starter-loadbalancer` 必须显式声明（否则 `lb://` 失效）；`spring-cloud-alibaba-sentinel-gateway` 适配包必须显式声明（starter 不传递，缺则网关限流编译失败）。
- **RocketMQ（#4）**：已替代 WebSocket。`OrderServiceImpl` 经 `RocketMQProducerService` 异步发订单通知到 Topic `order-notification`，`RocketMQConsumerService` 消费记日志；发送失败不阻断主流程。
- **Druid starter**：用 `druid-spring-boot-3-starter`（Boot 3.x 兼容版），非旧版 `druid-spring-boot-starter`（后者自动配置在 Boot 3.x 不生效，会 fallback Hikari）。
- **Nacos client**：版本由 SCA 2025 BOM 管理（**3.0.3**），对齐 server v3.0.3，**不再 pin**。此前"pin 2.5.1 避免 403"已废弃——`Handle API Compatibility failed` 真实根因是服务端**关闭认证**致 v1 登录端点未激活，非客户端版本；开启认证后正常。
- **Nacos 认证**：`NACOS_AUTH_ENABLE=true`，客户端凭据由 `.env` 的 `NACOS_USERNAME`/`NACOS_PASSWORD` 提供（`application.yml` 与两份 Nacos 模板的 Sentinel datasource 用 `${NACOS_PASSWORD:...}` 占位符）。
- **敏感配置**：credentials/AKSK 放 Nacos 用 `${ENV_VAR}` 占位符或本地模板，绝不提交；`application-dev.yml` 与 `docs/` 已 gitignore。
- **Windows 编码坑**：Windows 原生 JDK 17 默认 GBK，Nacos UTF-8 YAML 会解析失败——仅 Windows 需启动前设 `JAVA_TOOL_OPTIONS`（见 Build & Run）。

## Dependencies Managed by BOM

不要给 Spring Cloud / SCA 及其传递依赖指定版本，由 BOM 管理：
- Spring Boot `3.5.0`（父 POM 统一）
- `org.springframework.cloud:spring-cloud-dependencies:2025.0.0`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.0.0.0`

## TODO: Future Phases

见 `.others/后续阶段TODO.md`。已完成 #1 Gateway+Nacos、#2 Sentinel、#3 Seata、#4 RocketMQ、#5 Docker 容器化；剩 #6 K8s 编排。
