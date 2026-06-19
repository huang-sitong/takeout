一个基于 Spring Boot 的外卖点餐系统，包含管理端和用户端，支持微信小程序点餐、微信支付等功能。

## 项目简介

苍穹外卖是一个完整的外卖点餐系统，采用前后端分离架构。后端提供 RESTful API，支持管理端（商家管理后台）和用户端（微信小程序）两种客户端。

## 技术栈

### 后端技术
- **核心框架**: Spring Boot 2.7.3
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

### 项目结构

```
sky-take-out
├── sky-common          # 公共模块
│   ├── constant        # 常量类
│   ├── context         # 上下文（ThreadLocal）
│   ├── enumeration     # 枚举类
│   ├── exception       # 自定义异常
│   ├── json            # JSON配置
│   ├── properties      # 配置属性类
│   ├── result          # 统一返回结果
│   └── utils           # 工具类
├── sky-pojo            # 实体类模块
│   ├── dto             # 数据传输对象
│   ├── entity          # 实体类
│   └── vo              # 视图对象
└── sky-server          # 服务端模块
    ├── config          # 配置类
    ├── controller      # 控制器
    │   ├── admin       # 管理端接口
    │   └── user        # 用户端接口
    ├── interceptor     # 拦截器
    ├── mapper          # MyBatis Mapper接口
    ├── service         # 业务逻辑层
    └── websocket       # WebSocket服务
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

## 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis 6.0+

### 数据库配置
1. 创建数据库：
```sql
CREATE DATABASE sky_take_out;
```

2. 执行 SQL 脚本：
```bash
mysql -u root -p sky_take_out < .sql/sky.sql
```

### 应用配置
在 `application-dev.yml` 中配置以下参数：

```yaml
sky:
  datasource:
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 0
  alioss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
    bucket-name: your_bucket_name
  wechat:
    appid: your_wechat_appid
    secret: your_wechat_secret
  shop:
    address: your_shop_address
  baidu:
    ak: your_baidu_ak
```

### 运行项目
```bash
# 克隆项目
git clone https://github.com/your-username/sky-take-out.git

# 进入项目目录
cd sky-take-out

# 编译打包
mvn clean package

# 运行项目
java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

### 访问接口文档
启动项目后，访问 Knife4j 接口文档：
- 管理端接口: http://localhost:8080/doc.html

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

## 联系方式

如有问题或建议，请提交 Issue 或联系项目维护者。
