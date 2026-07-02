一个基于 Spring Boot + Spring Cloud Alibaba 的外卖点餐系统，包含管理端和用户端，支持微信小程序点餐、微信支付等功能。

## 项目简介

苍穹外卖是一个完整的外卖点餐系统，采用前后端分离 + 微服务架构。后端提供 RESTful API，支持管理端（商家管理后台）和用户端（微信小程序）两种客户端。

## 技术栈

### 后端技术
- **核心框架**: Spring Boot 2.7.3
- **微服务治理**: Spring Cloud 2021.0.9 + Spring Cloud Alibaba 2021.0.6.1
- **注册中心 & 配置中心**: Nacos 2.2.3
- **API 网关**: Spring Cloud Gateway
- **流量控制 & 熔断降级**: Sentinel 1.8.6 (两层限流 + 慢调用/异常比例熔断)
- **持久层**: MyBatis 2.2.0
- **数据库**: MySQL
- **缓存**: Redis
- **连接池**: Druid 1.2.1
- **分页插件**: PageHelper 1.3.0
- **认证授权**: JWT (jjwt 0.9.1)
- **接口文档**: Knife4j 3.0.2
- **对象存储**: 阿里云 OSS
- **支付**: 微信支付
- **其他**: Lombok, Fastjson, Apache POI

### 微服务架构

```
浏览器 (:7999) ──▶ Nginx ──▶ sky-gateway (:8081) ──▶ sky-server (:8080)
                                │        │              │
                                │        └─ Sentinel 规则 ─┤
                                └──── Nacos (:8848) ──────┘
                                         │      ↑
                                  注册+配置+规则  Sentinel Dashboard (:8858)
```

| 服务 | 端口 | 说明 |
|------|:----:|------|
| Nginx (前端) | 7999 | 静态资源 + API 反向代理 |
| sky-gateway | 8081 | API 网关，路由 + CORS + 负载均衡 + 限流 |
| sky-server | 8080 | 业务服务 (Controller/Service/Mapper) + 限流熔断 |
| Nacos Server | 8848 | 注册中心 + 配置中心 + Sentinel 规则存储 |
| Sentinel Dashboard | 8858 | 流控规则推送 + 实时监控 (可选，非强依赖) |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

### 项目结构

```
sky-take-out
├── sky-common          # 公共模块
│   ├── constant        # 常量类
│   ├── context         # 上下文（ThreadLocal）
│   ├── enumeration     # 枚举类
│   ├── exception       # 自定义异常
│   ├── json            # JSON配置
│   ├── properties      # 配置属性类 (@RefreshScope 支持动态刷新)
│   ├── result          # 统一返回结果
│   └── utils           # 工具类
├── sky-pojo            # 实体类模块
│   ├── dto             # 数据传输对象
│   ├── entity          # 实体类
│   └── vo              # 视图对象
├── sky-server          # 业务服务模块
│   ├── config          # 配置类
│   ├── controller      # 控制器
│   │   ├── admin       # 管理端接口
│   │   ├── user        # 用户端接口
│   │   └── notify      # 支付回调接口
│   ├── handler         # 统一异常处理 + Sentinel BlockHandler
│   ├── interceptor     # 拦截器
│   ├── mapper          # MyBatis Mapper接口
│   ├── service         # 业务逻辑层
│   └── websocket       # WebSocket服务 (已停用，后续用RocketMQ替代)
└── sky-gateway         # API 网关模块
    ├── config          # Gateway 配置 (Sentinel 限流 + CORS)
    └── GatewayApplication.java
```

## 功能特性

### 管理端功能
- **员工管理**: 员工登录/退出、新增/编辑/删除员工、员工分页查询、状态设置
- **分类管理**: 菜品分类和套餐分类的增删改查
- **菜品管理**: 菜品新增/编辑/删除/启售/停售、菜品分页查询、菜品口味管理
- **套餐管理**: 套餐新增/编辑/删除/启售/停售、套餐分页查询
- **订单管理**: 订单查询/统计/导出、订单状态管理（接单/拒单/取消/完成）
- **数据统计**: 营业额统计、用户统计、订单统计、销量排名
- **工作台**: 今日数据概览、订单管理快捷入口
- **通用功能**: 文件上传（阿里云OSS）

### 用户端功能
- **微信登录**: 基于微信小程序的用户登录
- **浏览功能**: 分类浏览、菜品浏览、套餐浏览
- **购物车**: 添加/删除/清空购物车
- **下单功能**: 用户下单、订单支付（微信支付）
- **订单管理**: 历史订单查询、订单详情、取消订单、再来一单、催单
- **地址管理**: 收货地址的增删改查

### Sentinel 流控熔断
- **Gateway 层限流**: 全路由 `sky-server-route` 全局 100 QPS，超过返回 HTTP 429 + `Result{code:0,msg:"系统繁忙,请稍后再试"}`
- **sky-server 层限流**: 7 个核心写接口（下单/支付/取消/接单/拒单/取消/完成）各 5 QPS
- **熔断降级**: 慢调用 (RT>300ms) + 异常比例 (>50%) 双策略，窗口 10s
- **规则持久化**: 4 份规则 JSON 存于 Nacos，Dashboard 重启不丢失；模板见 `docs/` 目录
- **统一返回**: 限流/熔断时返回与正常响应结构一致的 `Result{code:0,msg:...}`，前端可无感处理

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 5.7+
- Redis 6.0+
- Nacos Server 2.2.3 (Docker)
- Sentinel Dashboard 1.8.6 (WSL 运行 jar 包，可选但推荐)

### 1. 部署 Nacos Server
```bash
docker run -d \
  --name nacos-server \
  --restart=always \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  -e MODE=standalone \
  -e PREFER_HOST_MODE=hostname \
  nacos/nacos-server:v2.2.3
```

启动后访问 http://127.0.0.1:8848/nacos，默认账号 `nacos/nacos`。

### 2. 部署 Sentinel Dashboard
本项目 Sentinel Dashboard 通过 `docker/sentinel-dashboard/` 目录的脚本在 WSL 中直接运行 jar 包。

在 WSL 中执行：
```bash
cd /mnt/e/java/sky-take-out/docker/sentinel-dashboard
./start.sh         # 启动 (后台运行，日志写入 /tmp/sentinel.log)
./status.sh        # 查看运行状态
./stop.sh          # 停止
```
启动后访问 http://localhost:8858/，默认账号 `sentinel/sentinel`。
> Dashboard 仅用于实时监控和规则推送，**非强依赖** — 规则持久化在 Nacos，Dashboard 不启动也能限流。

### 3. 创建 Nacos 配置
在 Nacos 控制台 → 配置管理 → 配置列表 → 新建/编辑以下配置（模板见 `docs/` 目录）：

| Data ID | Group | 格式 | 说明 |
|---------|-------|------|------|
| `sky-server-dev.yaml` | `DEFAULT_GROUP` | YAML | 数据库/Redis/OSS/微信/Sentinel 配置 |
| `sky-server-flow-rules.json` | `DEFAULT_GROUP` | JSON | sky-server 流控规则 (7 个核心接口各 5 QPS) |
| `sky-server-degrade-rules.json` | `DEFAULT_GROUP` | JSON | sky-server 熔断规则 (慢调用 + 异常比例) |
| `sky-gateway-flow-rules.json` | `DEFAULT_GROUP` | JSON | gateway 全局限流规则 (路由 100 QPS) |

### 4. 数据库配置
```sql
CREATE DATABASE sky_take_out;
```
执行 SQL 脚本：
```bash
mysql -u root -p sky_take_out < .sql/sky.sql
```

### 5. 启动服务

确保 `JAVA_HOME` 指向 JDK 17。**Windows 上必须加 `-Dfile.encoding=UTF-8`**（否则 Nacos 配置中的中文会被按 GBK 解码成乱码，导致 YAML 解析失败）：

```bash
# 终端1：启动 sky-server
mvn -pl sky-server spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# 终端2：启动 sky-gateway
mvn -pl sky-gateway spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
```

启动顺序：Nacos → MySQL → Redis → (可选 Sentinel Dashboard) → sky-server → sky-gateway → Nginx。

### 6. 配置前端 Nginx
将 Nginx 反向代理目标指向 gateway 端口 `8081`：
```nginx
upstream webservers {
    server 127.0.0.1:8081 weight=90;
}
```

### 7. 验证
- Nacos 控制台 → 服务列表：`sky-server` 和 `sky-gateway` 均已注册
- Sentinel Dashboard (若已启动) → 可看到两个客户端实时监控
- 浏览器访问 `http://localhost:7999` 测试前端功能
- 接口文档: http://localhost:8080/doc.html
- 限流验证: 高频请求核心接口，超过 5 QPS 会返回 `{"code":0,"msg":"请求过于频繁,请稍后再试","data":null}`

## 接口说明

### 管理端接口 (/admin)
| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 员工管理 | /admin/employee | 员工登录、CRUD操作 |
| 分类管理 | /admin/category | 分类增删改查 |
| 菜品管理 | /admin/dish | 菜品增删改查 |
| 套餐管理 | /admin/setmeal | 套餐增删改查 |
| 订单管理 | /admin/order | 订单查询、状态管理 |
| 数据统计 | /admin/report | 各类数据统计 |
| 工作台 | /admin/workspace | 今日数据概览 |

### 用户端接口 (/user)
| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 用户登录 | /user/user/login | 微信登录 |
| 分类浏览 | /user/category | 分类查询 |
| 菜品浏览 | /user/dish | 菜品查询 |
| 套餐浏览 | /user/setmeal | 套餐查询 |
| 购物车 | /user/shoppingCart | 购物车操作 |
| 订单 | /user/order | 下单、支付、查询 |
| 地址 | /user/addressBook | 地址管理 |

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件
