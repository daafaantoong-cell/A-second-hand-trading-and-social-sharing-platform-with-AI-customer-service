# 接入 AI 客服的二手交易与分享社交平台

> 前后端分离式二手交易社交平台，涵盖用户端、商家端、管理端三大模块。聚焦高并发秒杀、缓存一致性、分布式场景问题解决，通过 Redis 适配多元业务，保障平台高并发、高可用稳定运行。集成 LangChain4j AI 智能客服，支持 SSE 流式对话、Tool Calling 工具调用、RAG 语义检索与会话记忆隔离。

## 技术栈

| 层面 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.2 / Java 17 |
| ORM | MyBatis-Plus 3.5 |
| 缓存 | Redis（缓存/分布式锁/会话共享）+ Redis Stack（向量库） |
| 分布式锁 | Redisson 3.20（看门狗续期） |
| 数据库 | MySQL 8.0 |
| AI 框架 | LangChain4j 1.18（OpenAI 兼容接口 + DashScope Embedding） |
| LLM | 通义千问 qwen-plus |
| 流式输出 | Reactor Flux（SSE） |
| API 文档 | Knife4j 4.4（OpenAPI 3） |
| 开发工具 | Docker、IntelliJ IDEA、DataGrip、Maven、Apifox、Nginx |

## 功能架构

### 三大业务端

- **用户端**：浏览商品、搜索卖家店铺、参与秒杀、发布分享博文、关注与 Feed 流、AI 客服咨询
- **商家端**：店铺管理、商品上架、优惠券发布、自提预约管理、订单处理
- **管理端**：用户管理、内容审核、举报处理、AI 客服知识库初始化

### 核心模块

#### 1. AI 智能客服

基于 LangChain4j 搭建 SSE 流式对话客服，LLM 自主分类用户意图并路由：

- **意图路由**：System Prompt 中构建 7 类意图分类规则（闲聊、商品查询、店铺搜索、优惠券咨询、自提预约、举报投诉、兜底），引导 LLM 自主识别并路由至 RAG 知识检索或 Tool Calling 工具
- **Tool Calling**：注册 6 个工具方法，覆盖卖家店铺查询、名称模糊搜索、按类型筛选、优惠券查询、自提预约、举报投诉等业务场景，LLM 自主决策调用时机与参数
- **反幻觉约束**：System Prompt 内置 6 条硬规则，禁止编造店铺信息/操作结果，要求预约前必须先通过工具获取真实店铺 ID，注入实时系统时间防止 LLM 时间认知偏差
- **会话隔离**：按 userId:sessionId 隔离，通过 @ToolMemoryId 跨 Reactor 异步线程传递用户身份，复用 Service 层越权校验

#### 2. 会话记忆与语义检索

- **Token 控制**：采用 jtokkit BPE 分词算法精确估算 token 数量，超出上限时自动裁剪最早消息
- **双层存储**：Redis 写入热数据（低延迟读写），MySQL 通过自定义有界 IO 线程池异步落库（冷数据持久化），实现读写延迟可控与存储量可控
- **语义检索**：Docker 部署 Redis Stack 向量库，将用户分享博文向量化存储，对话时召回 Top-3 测评原文交由 LLM 生成带依据的商家推荐，解决 SQL 查询无法语义匹配的痛点

#### 3. 分布式认证

- 采用 Redis 替代传统 Session 实现集群环境下的会话共享
- 定义双层权限拦截器：第一层刷新 Token 有效期（所有请求），第二层校验登录态与权限（核心接口）
- 解决集群环境下登录状态不一致、权限混乱等问题

#### 4. 缓存体系优化

- 基于 Cache Aside 缓存模式优化商品、博文等高频查询接口
- 使用 Redisson 读写锁解决缓存与数据库数据一致性问题
- 针对性落地缓存穿透（空值缓存）、击穿（互斥锁）、雪崩（随机 TTL）全套解决方案

#### 5. 高并发秒杀

- 采用 Redis + Lua 脚本完成秒杀资格原子性校验，杜绝商品超卖问题
- 依托 Redisson 分布式锁结合看门狗续期机制实现业务防重与一人一单规则
- 异步下单：Redis Stream 作为消息队列，后台线程消费处理订单落库

## 项目结构

```
com.hmdp
├── ai/                  # AI Agent（意图分类、业务路由）
├── config/              # 配置类（ChatMemory、RAG、Redisson、MVC、Knife4j）
├── controller/          # 接口层（含 AiChatController 流式对话入口）
├── dto/                 # 数据传输对象
├── entity/              # 实体类
├── mapper/              # MyBatis-Plus Mapper
├── policy/              # Token 窗口裁剪策略
├── repository/          # 双层 ChatMemoryStore（Redis + MySQL）
├── service/             # 服务层（含 AiChatService @AiService 声明）
│   └── impl/
├── tool/                # Tool Calling 工具类
├── task/                # 定时任务
├── cache/               # Redis 缓存工具类
└── utils/               # 工具类（UserHolder、RedisIdWorker、LoginInterceptor 等）
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6+（端口 6379）
- Redis Stack（端口 6380，Docker 部署）
- Maven 3.9+

### 1. 初始化数据库

```sql
CREATE DATABASE hmdianping DEFAULT CHARACTER SET utf8mb4;
```

执行 SQL 脚本：

```bash
mysql -u root -p hmdianping < src/main/resources/db/hmdp.sql
mysql -u root -p hmdianping < src/main/resources/db/customer_service_upgrade.sql
```

### 2. 启动 Redis 与 Redis Stack

```bash
# 普通 Redis（缓存/锁/会话）
redis-server --port 6379

# Redis Stack（向量库）
docker run -d --name redis-stack -p 6380:6379 redis/redis-stack:latest
```

### 3. 配置 API Key

编辑 `src/main/resources/application.yaml`，替换 DashScope API Key。

### 4. 编译启动

```bash
mvn clean compile
mvn spring-boot:run
```

### 5. 初始化 RAG 向量库

启动后调用管理端接口，将分享博文写入向量库：

```bash
POST http://localhost:8081/chat/rag/init
```

## API 文档

启动后访问 Knife4j 文档：http://localhost:8081/doc.html
