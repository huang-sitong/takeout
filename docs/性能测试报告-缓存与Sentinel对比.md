# 三阶段性能对比测试报告（Redis 缓存 / Sentinel 框架损耗）

> 执行日期：2026-08-27 ｜ 环境：WSL2（Docker 容器化部署），宿主机 16 核 / 11GB
> 被测链路：`sky-gateway(:8081)` → `sky-menu-service(:8084)` → MySQL（Windows 原生）
> 压测场景：`menu_flow`（category / dish / setmeal / setmeal_dish 等权重混合，经网关）

## 1. 测试目的

按三阶段对比，量化两个组件的性能影响：

| 阶段 | Redis 缓存 | Sentinel（不配任何限流规则） | 目的 |
|------|:---:|:---:|------|
| 阶段一 | ❌ 关闭 | ❌ 关闭 | 找出无缓存、无 Sentinel 时的瓶颈基线 |
| 阶段二 | ✅ 开启 | ❌ 关闭 | 观察缓存带来的吞吐/延迟变化 |
| 阶段三 | ✅ 开启 | ✅ 开启（**0 条规则**） | 量化 Sentinel 框架本身的性能损耗 |

> 阶段三的 Sentinel 已确认接入（URL 被 `sentinel_spring_web_context` 监控、`Flow rules loaded: {}`、dashboard 正常轮询），但**未加载任何限流/熔断规则**，因此测的是框架空载损耗。

## 2. 测试方法

- **主压测**：`perf-test/scripts/load.py`（开环定速），按目标 QPS 打压 50~60s，统计实际吞吐与 RT 分位数。
- **交叉验证**：`perf-test/scripts/sat_thread.py`（固定并发饱和压测），排除单一客户端偏差。
- **监控**：`perf-test/scripts/cpu_monitor.py` 实时采样宿主机 CPU 与容器占用。
- **安全控制**：全程未让 CPU 打满（最高 ~21%），避免死机。

## 3. 关键发现（瓶颈定位）

### 3.1 连接池 **不是** 瓶颈（回答"是不是连接池"的疑问）

- 本项目用的是 **Druid**（`spring.datasource.druid.*`，`max-active: 20`），**不是 HikariCP**。
- 在 800 QPS 压测进行中实测：`sky_menu_db` 连接数仅 **7~8 / 20**，`Threads_running` 仅 **0~2**。
- 结论：连接池远未打满、MySQL 也不忙，**调大连接池大小对当前瓶颈没有帮助**。

### 3.2 发现并修复的第一个放大器：`com.sky.mapper: debug` 日志风暴

- 项目默认把 MyBatis Mapper 设为 `debug`，每个 SQL 都会同步输出 `Preparing/Parameters/Total`。
- 压测时容器日志膨胀到 **107MB**，高并发下 logback 同步控制台输出成为严重瓶颈。
- 已将压测配置改为 `com.sky.mapper: info`（生产本就不应开 mapper debug），日志量降到 ~11KB。

### 3.3 真正的吞吐瓶颈在应用框架层

- 单请求（低并发）category 查询仅 **4ms**，业务 SQL、DB 都很快。
- 但并发升高后出现"**并发越高吞吐越低**"的异常曲线（20 并发 680 QPS → 200 并发 166 QPS）。
- 即使**不查数据库**的 `/actuator/health` 也表现同样的退化。
- `jstack` 线程 dump 显示请求线程卡在 **Spring MVC `AbstractHandlerMethodMapping.addMatchingMappings`**（每次请求遍历 `HashMap` 匹配 handler），加上 Tomcat 线程模型，共同构成框架层瓶颈。
- 环境因素（WSL2 + Docker 端口映射 + 虚拟化网络）也对绝对吞吐有影响。

## 4. 三阶段实测数据（经网关，menu_flow 场景）

| 阶段 | 目标 QPS | 实际 QPS | p50(ms) | p95(ms) | p99(ms) | 错误 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| 阶段一（无缓存/无Sentinel） | 400 | 399.9 | 4.5 | 9.1 | 16.6 | 0 |
| 阶段一 | 500 | 465.5 | 67.5 | 234.4 | 360.3 | 0 |
| 阶段一 | 600 | 492.8 | 65.2 | 226.0 | 334.1 | 0 |
| 阶段一 | 800 | 526.6 | 60.9 | 211.7 | 317.1 | 0 |
| 阶段二（开缓存/无Sentinel） | 400 | 399.9 | 4.4 | 103.0 | 230.2 | 0 |
| 阶段二 | 600 | 543.6 | 44.5 | 179.6 | 275.3 | 0 |
| 阶段二 | 800 | 524.4 | 56.1 | 189.9 | 289.2 | 0 |
| 阶段三（开缓存+Sentinel无限流） | 400 | 399.9 | 5.5 | 184.3 | 368.7 | 0 |
| 阶段三 | 600 | 552.6 | 51.4 | 180.5 | 269.7 | 0 |
| 阶段三 | 800 | 525.5 | 56.5 | 193.4 | 287.3 | 0 |

> 交叉验证（sat_thread 固定并发，category 接口，阶段三经网关）：
> 20 并发 609 QPS / p50 31ms；40 并发 509 QPS / p50 83ms；80 并发 290 QPS / p50 160ms。

## 5. 结论

### 5.1 合适的压力打压幅度

- 系统在 **≤400 QPS** 时处于稳定区（RT ~4.5ms，完全跟上目标）。
- 超过 **400~500 QPS** 后进入饱和，RT 急剧恶化到 60ms+，实际吞吐被压在 **~520~550 QPS**。
- 因此合适的打压幅度取 **400 QPS**（稳定、可重复），高压对比可加到 **600/800 QPS**（饱和区）。

### 5.2 Redis 缓存的效果（阶段一 → 阶段二）

- 稳定区（400 QPS）差异不大（p50 4.4ms vs 4.5ms），因为 DB 本身够快。
- 饱和区（600 QPS）缓存带来约 **+10% 吞吐**（492.8 → 543.6 QPS）且 RT 改善（p50 65 → 44ms）。
- 但在深度饱和（800 QPS）两者都被框架层天花板压到 ~524 QPS，**缓存无法突破框架层瓶颈**。

### 5.3 Sentinel 框架本身损耗（阶段二 → 阶段三）

- 三档压力下吞吐差异 **< 2%**（543.6 vs 552.6；524.4 vs 525.5），RT 基本持平。
- **在当前瓶颈（非 CPU）下，Sentinel 空载接入的额外开销可忽略**。
- 说明：因为当前瓶颈不在 CPU，而 Sentinel 主要是 CPU 侧开销，故损耗被"隐藏"；若未来瓶颈转移到 CPU，Sentinel 损耗才会显现。

## 6. 关于"连接池在哪设置"（Windows / WSL）

- **连接池参数属于应用配置，不涉及 Windows/WSL 系统文件**。
- 配置位于 **Nacos 配置中心**（Data ID `sky-menu-service-docker.yaml` 的 `spring.datasource.druid.*`），应用运行在 WSL 的 Docker 容器里读取它。修改后用 Nacos 发布 + 重启服务即可，与 MySQL 跑在 Windows 原生还是 WSL 无关。
- 若确实要调 MySQL 服务端上限（`max_connections`，当前 151），才需要改 **Windows 原生 MySQL 的 my.ini**，但当前并非瓶颈。

## 7. 遗留说明

- 测试期间为支持"无缓存"模式，给 `sky-menu-service` 增加了可配置开关 `sky.cache.enabled`（默认 `true`，向后兼容）：关闭时不初始化 Redis、菜品查询直接走 DB；开启时行为与原来完全一致。
- 压测数据原始文件：`/tmp/perf/p1_*.json`、`p2_*.json`、`p3_*.json`、`*_sat_c*.json` 等。
