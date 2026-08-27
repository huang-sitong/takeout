A Spring Boot + Spring Cloud Alibaba takeout ordering system with admin management and WeChat mini-program user side.

## About

Cang Qiong Take-Out is a complete takeout ordering system using front-end/back-end separation + microservice architecture. The backend provides RESTful APIs for both admin (merchant management) and user (WeChat mini-program) clients.

## Tech Stack

> Upgraded to Spring Boot 3.x (from Spring Boot 2.7.3). `javax.*` to `jakarta.*`, JDK 21+ (virtual threads enabled).

### Backend
- **Runtime**: JDK 21 (Jakarta EE namespace, virtual threads; JDK 24 incompatible with Lombok)
- **Core Framework**: Spring Boot 3.5.0
- **Microservice Governance**: Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0
- **Registry & Config**: Nacos 3.0.3 (authentication enabled, console on port 8849)
- **API Gateway**: Spring Cloud Gateway (WebFlux/Netty)
- **Rate Limiting & Circuit Breaking**: Sentinel 1.8.9 (two-tier flow control + degrade rules)
- **Distributed Transactions**: Seata 2.5.0 (AT mode, SCA 2025 auto-proxy DataSource + undo_log)
- **Message Queue**: RocketMQ 5.3.1 (rocketmq-spring-boot-starter 2.3.4)
- **Persistence**: MyBatis (mybatis-spring-boot-starter 3.0.5)
- **Database**: MySQL
- **Cache**: Redis (Lettuce, config key `spring.data.redis.*`)
- **Connection Pool**: Druid 1.2.23 (`druid-spring-boot-3-starter`, Boot 3.x compatible)
- **Pagination**: PageHelper 2.1.1
- **Auth**: JWT (jjwt 0.12.6)
- **API Docs**: OpenAPI Spec (see `openAPI_Spec/`)
- **Object Storage**: Alibaba Cloud OSS (aliyun-sdk-oss 3.17.4)
- **Payment**: WeChat Pay (test-friendly mock implementation)
- **Other**: Lombok 1.18.38, Fastjson 2.0.53, Apache POI 5.2.5

### Microservice Architecture (Phase 8)

```
                    sky-gateway (:8081)
               JWT Auth + Routing + Rate Limit
              /       |       |       |       \
             /        |       |       |        \
    sky-admin    sky-user   sky-menu  sky-cart  sky-order
     (:8082)      (:8083)    (:8084)   (:8085)   (:8086)
    employee     user       category   cart      order
                 address    dish                 report
                            setmeal              shop
                            OSS                  Seata + MQ
         |          |          |        |          |
         +----------+----------+--------+----> Feign RPC
```

| Service | Host Port | Container | Database | Description |
|----------|:---------:|:-----------:|----------|-------------|
| sky-gateway | 8081 | 8081 | -- | API Gateway, JWT auth + routing + rate limit |
| sky-admin-service | 8092 | 8082 | sky_admin_db | Employee management |
| sky-user-service | 8083 | 8083 | sky_user_db | User + address + WeChat login |
| sky-menu-service | 8084 | 8084 | sky_menu_db | Category/dish/setmeal + OSS |
| sky-cart-service | 8085 | 8085 | sky_cart_db | Shopping cart (depends on menu) |
| sky-order-service | 8086 | 8086 | sky_order_db | Order/report/shop + Seata + MQ |
| Nacos Server | 8848/8849 | 8848/8080 | nacos_config | Registry + config center |
| Redis | 6379 | 6379 | -- | Cache |
| Seata Server | 8091 | 8091 | seata | Distributed transaction TC |
| Sentinel Dashboard | 8858 | 8858 | -- | Flow control dashboard |
| RocketMQ NameServer | 9876 | 9876 | -- | Message queue routing |
| RocketMQ Broker | 10911 | 10911 | -- | Message storage & delivery |
| RocketMQ Console | 8082 | 8080 | -- | Message queue console |
| MySQL (host) | 3306 | -- | -- | Database |

### Project Structure

```
sky-take-out
├── sky-common              # Common module (utils/constants/exceptions/JWT/OSS/GlobalExceptionHandler)
├── sky-pojo                # Entity module (DTO/entity/VO, split by bounded context)
├── sky-feign-common        # OpenFeign config (FeignConfig/Interceptor/ErrorDecoder)
├── sky-admin-service       # Employee management (:8082, DB sky_admin_db)
├── sky-user-service        # User + address (:8083, DB sky_user_db)
├── sky-menu-service        # Menu management (:8084, DB sky_menu_db)
├── sky-cart-service        # Shopping cart (:8085, DB sky_cart_db, depends on menu)
├── sky-order-service       # Order core (:8086, DB sky_order_db, depends on all services)
├── sky-gateway             # API Gateway (WebFlux/Netty, JWT auth + CORS + rate limit)
```

## Features

### Admin Features
- **Employee Management**: Login/logout, CRUD, pagination, status toggle
- **Category Management**: Dish & setmeal category CRUD
- **Dish Management**: CRUD, enable/disable, pagination, flavor management
- **Setmeal Management**: CRUD, enable/disable, pagination
- **Order Management**: Query/statistics/export, status management (confirm/reject/cancel/complete)
- **Data Statistics**: Revenue, user, order stats, sales ranking
- **Workspace**: Today's overview, quick order management
- **Common**: File upload (Alibaba Cloud OSS)

### User Features
- **WeChat Login**: WeChat mini-program based user login
- **Browsing**: Category/dishes/setmeals browsing
- **Shopping Cart**: Add/remove/clear cart
- **Ordering**: Place order, payment (WeChat Pay mock)
- **Order Management**: History, detail, cancel, reorder, reminder
- **Address Management**: Address CRUD

### Sentinel Flow Control & Circuit Breaking
- **Gateway-level**: Global route 100 QPS, excess returns HTTP 429 + `Result{code:0,msg:"System busy"}`
- **Service-level**: 7 core write APIs (submit/pay/cancel/confirm/reject/cancel/complete) at 5 QPS each
- **Degrade**: Slow call (RT>300ms) + exception ratio (>50%) dual strategy, 10s window
- **Rule Persistence**: JSON rules stored in Nacos, survive Dashboard restart
- **Unified Response**: Flow/degrade returns same `Result{code:0,msg:...}` structure as normal responses

## Quick Start

### Prerequisites
- JDK 21+
- Maven 3.6+
- MySQL 5.7+
- Docker Desktop

### 1. Docker Compose (Recommended)

```bash
# Initialize databases (first time)
mysql -u root -p < .sql/nacos-mysql.sql          # nacos_config
mysql -u root -p < .sql/seata-server.sql          # seata
mysql -u root -p < .sql/split/00-run-all.sql      # 5 microservice databases

# Build all JARs
mvn package -DskipTests

# Start all services
docker compose up -d --build

# Check status (12 containers)
docker compose ps

# Stop
docker compose down
```

### 2. Local Development Mode

```bash
# Start infrastructure
docker compose up -d nacos redis seata namesrv broker sentinel

# Source env vars (local dev doesn't read .env automatically)
set -a; source .env; set +a

# Start individual services
mvn -pl sky-admin-service spring-boot:run
mvn -pl sky-user-service spring-boot:run
mvn -pl sky-menu-service spring-boot:run
mvn -pl sky-cart-service spring-boot:run
mvn -pl sky-order-service spring-boot:run
mvn -pl sky-gateway spring-boot:run
```

> Note: Windows native JDK requires UTF-8 encoding before startup:
> `export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"`

### Nacos Config Initialization

Nacos 3.x has authentication enabled. Console is on independent port **8849**.

**First-time admin init** (Nacos >=2.4 has no built-in `nacos/nacos`):

```bash
docker compose up -d nacos
curl -X POST 'http://localhost:8848/nacos/v3/auth/user/admin' -d 'password=SkyNacos@2026'
```

**Import configs** at `http://localhost:8849/index.html` (login: `nacos` / `SkyNacos@2026`):

| Data ID | Format | Source | Purpose |
|---------|--------|--------|---------|
| `sky-admin-service-dev.yaml` | YAML | `nacos_config_example/` | Admin service local dev |
| `sky-admin-service-docker.yaml` | YAML | `nacos_config_example/` | Admin service Docker |
| `sky-user-service-dev.yaml` | YAML | `nacos_config_example/` | User service local dev |
| `sky-user-service-docker.yaml` | YAML | `nacos_config_example/` | User service Docker |
| `sky-menu-service-dev.yaml` | YAML | `nacos_config_example/` | Menu service local dev |
| `sky-menu-service-docker.yaml` | YAML | `nacos_config_example/` | Menu service Docker |
| `sky-cart-service-dev.yaml` | YAML | `nacos_config_example/` | Cart service local dev |
| `sky-cart-service-docker.yaml` | YAML | `nacos_config_example/` | Cart service Docker |
| `sky-order-service-dev.yaml` | YAML | `nacos_config_example/` | Order service local dev |
| `sky-order-service-docker.yaml` | YAML | `nacos_config_example/` | Order service Docker |
| `sky-order-service-flow-rules.json` | JSON | `nacos_config_example/` | Sentinel flow rules |
| `sky-order-service-degrade-rules.json` | JSON | `nacos_config_example/` | Sentinel degrade rules |
| `sky-gateway-flow-rules.json` | JSON | `nacos_config_example/` | Gateway rate limit |

> Configs are persisted in MySQL `nacos_config` database. Secrets use `${ENV_VAR:default}` placeholders resolved from gitignored `.env` file at runtime.

## API Reference

### Admin APIs (/admin)
| Module | Path Prefix | Description |
|--------|-------------|-------------|
| Employee | /admin/employee | Login, CRUD |
| Category | /admin/category | Category CRUD |
| Dish | /admin/dish | Dish CRUD |
| Setmeal | /admin/setmeal | Setmeal CRUD |
| Order | /admin/order | Order query, status management |
| Report | /admin/report | Data statistics |
| Workspace | /admin/workspace | Today's overview |
| Shop | /admin/shop | Shop status |

### User APIs (/user)
| Module | Path Prefix | Description |
|--------|-------------|-------------|
| Login | /user/user/login | WeChat login |
| Category | /user/category | Category browsing |
| Dish | /user/dish | Dish browsing |
| Setmeal | /user/setmeal | Setmeal browsing |
| Shopping Cart | /user/shoppingCart | Cart operations |
| Order | /user/order | Order, payment, query |
| Address | /user/addressBook | Address management |
| Shop | /user/shop/status | Shop status |

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file
