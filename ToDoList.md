# 待办与未验证事项

> 本文档记录项目交付时**尚未完成或尚未验证**的事项，按优先级排列。
> 已完成的功能与验证情况见 [docs/开发与部署文档.md](docs/开发与部署文档.md)，
> 开发过程中遇到的问题与解决办法见 [Problem.md](Problem.md)。

**更新日期**：2026-09-23

---

## 一、Chroma 向量检索接入 ⏸️ 已推迟

**状态**：代码已完成，但 **`VECTOR` 分支从未在任何环境运行过**。

### 现状

`ChromaClient`（[backend/src/main/java/com/defect/platform/utils/ChromaClient.java](backend/src/main/java/com/defect/platform/utils/ChromaClient.java)）已实现完成，包括：
- 心跳探测（带 30 秒缓存，避免每次检索都打）
- 集合自动创建（`get_or_create`）
- 向量写入 / 删除 / 检索 / 计数
- 兼容 Chroma 0.5+/1.x 的 `/api/v2` 与 0.4.x 的 `/api/v1` 两种路径

但由于本机未部署 Chroma，RAG 检索实际生效的一直是降级路径（MySQL ngram 全文索引）。

### 接入步骤

```bash
# 1. 启动 Chroma（Docker 方式，宿主机映射 8000 端口）
docker run -d --name chroma -p 8000:8000 chromadb/chroma:latest

# 2. 配置连接（.env 或环境变量）
CHROMA_HOST=localhost
CHROMA_PORT=8000
# 若你的 Chroma 是 0.4.x，需要改成：
# CHROMA_API_PATH=/api/v1

# 3. 重启后端

# 4. 用管理员账号建索引（知识库已有数据必须先跑这一步）
curl -X POST http://localhost:8080/api/ai/index/rebuild \
  -H "Authorization: Bearer <管理员token>"

# 5. 确认打通
curl http://localhost:8080/api/ai/status -H "Authorization: Bearer <管理员token>"
# 期望：chromaReachable=true, retrievalMode=VECTOR, vectorCount>0
```

也可以在浏览器「AI 能力状态」页（管理员可见）点击按钮完成 4、5 两步，并直观看到连通状态。

### 接入时必须重点验证的点

| # | 风险点 | 说明 |
|---|---|---|
| 1 | **API 路径版本** | 代码默认 `/api/v2`。若 Chroma 是 0.4.x 必须改 `CHROMA_API_PATH=/api/v1`，否则心跳失败、静默降级回全文索引 |
| 2 | **服务端 embedding 函数** | 本实现走 Chroma 服务端的默认向量化（写入时只传 `documents`，查询时传 `query_texts`）。若你的 Chroma 未启用默认 embedding 函数，写入/检索会报错 |
| 3 | **元数据字段类型** | 向量元数据写入 `knowledge_id`（数字）、`title`、`type`（字符串），Chroma 对元数据类型有限制，如报错优先查这里 |
| 4 | **知识库增删改后的索引一致性** | 目前是「尽力而为」的同步写入，失败只记日志不阻断主流程。接入后建议手工验证：新增/编辑/删除知识后，`vectorCount` 是否正确变化 |
| 5 | **`index/rebuild` 的幂等性** | 当前每次调用都会重写全部向量，知识条数多时耗时较长，且未做并发保护 |

### 验收标准

- `GET /api/ai/status` 返回 `chromaReachable: true`、`retrievalMode: "VECTOR"`、`vectorCount` 与知识库条目数一致
- 调用 `POST /api/ai/recommend`，响应中 `retrievalMode` 为 `VECTOR`（而非 `FULLTEXT`）
- 知识库新增/删除条目后，重新检索结果随之变化

---

## 二、Docker 容器化配置验证 ❌ 完全没有实测

**状态**：配置已写完，但本机**未安装 Docker**，**`docker compose up` 从未执行过**。

已做的静态检查仅限于：
- YAML 语法解析通过（js-yaml）
- Compose 引用的文件路径存在性检查通过

**以下全部未经验证**：两个 Dockerfile 能否构建成功、nginx.conf 语法是否正确、容器间网络与健康检查是否按预期工作、数据卷挂载是否正常。

### 首次运行步骤

```bash
cp .env.example .env
docker compose up -d --build        # 首次构建预计 3-5 分钟
docker compose ps                   # 确认各容器状态
docker compose logs -f backend      # 观察后端启动日志
```

访问 http://localhost:8888 ，账号 `admin` / `admin123`。

### 已知风险点（按可能性排序）

| # | 风险点 | 说明与排查方向 |
|---|---|---|
| 1 | **后端镜像构建失败** | 构建阶段用 `maven:3.9-eclipse-temurin-17`，若无外网或镜像拉取受限会失败。本项目的 pom 固定了 `lombok 1.18.38` 并显式声明 `annotationProcessorPaths`，在 JDK 17 下应当正常 |
| 2 | **后端容器连不上 MySQL** | 健康检查用了 `mysqladmin ping`，若密码含特殊字符会解析异常。已用 `--character-set-server=utf8mb4` 与 `--ngram_token_size=2`，后者是中文全文索引的关键参数 |
| 3 | **首次启动时 `schema.sql` 未执行** | 该脚本仅在 MySQL 数据卷**为空**时执行。若卷已存在（比如之前 `docker compose up` 过），不会重新建表。需 `docker compose down -v` 后再起 |
| 4 | **前端 502** | nginx 反代 `http://backend:8080`，依赖 compose 的服务名解析。若后端尚未就绪，前端会短暂 502，属正常现象 |
| 5 | **`host.docker.internal` 不生效** | compose 中 Chroma 地址默认用 `host.docker.internal`（指向宿主机）。Linux 下需额外配置 `extra_hosts: ["host.docker.internal:host-gateway"]`，当前仅在 Windows/macOS 的 Docker Desktop 上开箱可用 |
| 6 | **前端构建内存不足** | Element Plus 全量引入，构建峰值内存较高。若失败可提高 Docker Desktop 的内存限制 |

### 验收标准

- `docker compose ps` 中 5 个服务均为 `healthy` / `running`
- 浏览器打开 `http://localhost:8888` 能正常登录并使用
- `docker compose down && docker compose up -d` 后数据仍在（数据卷持久化生效）

---

## 三、Redis 缓存命中路径未验证 ❌ 未实测

**状态**：`CacheService` 与缓存键设计已完成，**优雅降级路径已验证，但缓存命中路径从未真正生效过**。

**发现经过**：2026-09-23 排查未决事项时检查本机端口，发现 **6379 无服务监听**——即 `RedisTemplate` 始终连不上，`CacheService` 每次调用都走 `markUnavailable()` 分支，**缓存从未真正命中过**。

> ⚠️ 之前的表述是「Redis 缓存已实现」，严格说应为「**缓存代码已实现且降级路径已验证，缓存命中路径未验证**」。
> 这两者在面试或答辩时的含义**完全不同**，务必区分。

### 现状说明

| 路径 | 状态 |
|---|---|
| Redis 不可用时的降级（跳过缓存、直查数据库） | ✅ 已验证——开发期一直跑的就是这条路径 |
| Redis 可用时的缓存读写与命中 | ❌ **未验证** |
| 缓存失效（`evict` / `evictByPrefix`） | ❌ 未验证 |
| 缓存键含用户维度（多项目隔离下的正确性） | ❌ 未验证 |

### 接入步骤

```bash
# 启动 Redis（宿主机映射 6379 端口）
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 配置连接（.env 或环境变量）
REDIS_HOST=localhost
REDIS_PORT=6379

# 重启后端，确认启动日志中不再出现缓存不可用告警
```

### 接入后必须验证的点

| # | 风险点 | 说明 |
|---|---|---|
| 1 | **缓存是否真的命中** | 连续调两次 `GET /api/stats/overview`，第二次应无 SQL 输出（用 MyBatis 日志的 `Preparing:` 计数确认） |
| 2 | **缓存键的用户维度** | 用 admin 与普通用户分别请求统计，确认二者结果**不串**——这是多项目隔离下的关键正确性点，也是 `CacheKeys` 设计带 userId 的原因 |
| 3 | **失效是否生效** | 新建/关闭缺陷后再查统计，应拿到新数据而非旧缓存 |
| 4 | **`evictByPrefix` 的 SCAN 行为** | 实现用 `SCAN` 遍历（`SCAN_LIMIT = 1000L`），知识条目多时需确认能删干净 |

### 验收标准

- 后端启动日志出现 Redis 连接成功，且**不再**出现缓存不可用告警
- `GET /api/stats/overview` 第二次请求不产生 SQL
- 不同用户请求同一统计接口，绝不返回他人数据

---

## 四、前端浏览器端验证 🟡 部分已验证

**状态**：构建通过、接口契约全部通过，**页面已由用户手工走查了一部分，但未覆盖全部**。

**已验证**：
- `npm run build` 全量通过
- 45 个接口契约全通过
- dev server 与 `/api` 代理可用
- **用户手工验证**（2026-09-23 确认）：登录、项目列表、项目详情、缺陷列表、admin / TESTER 双角色切换

**未验证**：知识库列表与详情、AI 能力状态页、统计看板的 ECharts 图表渲染、附件上传下载、用户管理页的新建/编辑/重置密码、侧边栏按角色过滤菜单、路由守卫跳转、表单校验交互。

**验收方法**：按 [docs/开发与部署文档.md](docs/开发与部署文档.md) 第三节启动前后端，用 `admin` / `admin123` 登录后逐页点击一遍（重点覆盖上面「未验证」清单）。

---

## 五、其他待改进项

### 5.1 JWT 无状态导致禁用/删除用户的令牌在过期前仍有效

**现状**：`JwtInterceptor` 只解析令牌、不查库校验用户状态，因此管理员禁用或删除某用户后，该用户手里的令牌在有效期内（默认 24 小时）仍能正常访问。

**这是无状态 JWT 的固有权衡**，不是缺陷。若需即时失效，可选：
- 引入 Redis 令牌黑名单（禁用/删除时写入，拦截器校验）
- 缩短 `JWT_EXPIRATION`（当前 86400 秒）
- 在拦截器中每次查库校验用户状态（牺牲性能换实时性）

### 5.2 前端产物体积偏大

Element Plus 走全量引入，构建产物有两个约 1MB / 1.27MB 的 chunk（gzip 后约 346KB / 409KB）。

内网工具场景够用。若要优化，可改为按需引入（`unplugin-vue-components` + `unplugin-auto-import`），预计可减少一半以上体积。

### 5.3 RabbitMQ 已编排但业务未使用

技术栈要求包含 RabbitMQ，`docker-compose.yml` 已编排、`application.yml` 已配置，但**业务代码中尚无生产者与消费者**。

预留场景：缺陷变更通知、向量索引异步重建、批量操作。

### 5.4 「愿景文档提到、但不在本期规格范围」的功能

根目录 `ReadME.md` 的「核心功能实现」章节还描述了两项能力，但项目规格（7 阶段计划的核心功能范围）中未包含，**本期未实现**：

- **第三方集成能力**：GitHub 代码提交与缺陷 ID 关联、邮件/站内信双渠道通知
- **代码提交风险提示**：分析 Git 提交改动范围，提示可能引入的缺陷风险

如需实现，建议作为独立迭代处理。

---

## 六、已完成（归档备查）

以下事项曾列入待办，现已完成，保留记录便于回溯：

| 事项 | 完成情况 |
|---|---|
| `assertMember` 存在性校验顺序（管理员可对不存在项目操作，产生孤儿数据） | ✅ 阶段 5 后修复，见 `f99a1b6` |
| `assertOwner` 存在性校验顺序（潜在陷阱，非活跃缺陷） | ✅ 见 `3fe6c79` |
| 登录页注册入口误导性（注册即访客且无法自助升级） | ✅ 已移除入口并补管理员建号功能，见 `3fe6c79` |
| 缺陷/知识库列表的 N+1 查询 | ✅ 阶段 7 修复，t_user 查询由每条 1~2 次降为整页 1 次 |
| AI 分类提示词被界面词带偏 | ✅ 阶段 5 修复，见 `93b948d` |
| 列表页空态文案误导（未被加入项目的用户看到「暂无项目」，误判为系统故障） | ✅ 2026-09-23 修复，见下方说明 |

### 关于「列表页空态文案」的说明

**现象**：用户在管理员账号下创建项目与缺陷后，切换到测试人员账号却什么都看不到。

**根因**：这是**多项目隔离的正常行为**——可见项目 = 我负责的 + 我加入的（管理员例外）。
测试人员既非负责人也不在成员表，故无任何可见项目。真正的问题在于**空态只显示「暂无项目」，
未说明原因**，用户无法区分「系统故障」与「我没被加进任何项目」。

**修复**：三处列表页的空态改为按场景区分文案——

| 页面 | 空态文案策略 |
|---|---|
| 项目列表 | 管理员：「暂无项目。点击「新建项目」创建，**创建后请记得添加项目成员**」<br>非管理员：「你还没有加入任何项目。请联系项目负责人或管理员，将你添加为项目成员」 |
| 缺陷列表 | 无可访问项目时提示「未加入任何项目」；有项目但**有筛选条件**时提示「当前筛选条件下没有缺陷」；无筛选时提示「暂无缺陷，可点击「新建缺陷」提交」 |
| 知识库 | 知识库为全局共享不涉及数据权限，空态仅补「可将已解决的缺陷一键沉淀为知识」的引导 |

> **设计要点**：空列表有两种完全不同的成因——「确实没有数据」与「数据权限过滤后为空」。
> 前端已知 `projects.length`，据此可准确区分，无需后端额外提供字段。
