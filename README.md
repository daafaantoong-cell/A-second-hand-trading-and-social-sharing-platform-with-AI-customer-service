# 黑马点评 · AI 客服增强版

基于 Spring Boot 3 + LangChain4j 的点评类社交平台后端，在黑马点评原始业务（商铺浏览、优惠券秒杀、探店博客、关注 feed）基础上，集成了 **AI 智能客服** 能力，支持流式对话、Tool Calling、RAG 知识检索和会话记忆隔离。

## 技术栈

| 层面 | 技术选型 |
|------|----------|
| 框架 | Spring Boot 3.2.4 / Java 17 |
| ORM | MyBatis-Plus 3.5.5 |
| 缓存 | Redis 6379（缓存/分布式锁）+ Redis Stack 6380（向量库） |
| 分布式锁 | Redisson 3.20.1 |
| AI 框架 | LangChain4j 1.18（OpenAI 兼容接口 + DashScope Embedding） |
| LLM | 通义千问 qwen-plus（OpenAI 兼容模式） |
| Embedding | DashScope text-embedding-v4（1024 维） |
| 流式输出 | Reactor Flux（SSE） |
| API 文档 | Knife4j 4.4（OpenAPI 3） |
| 工具库 | Hutool 5.8 |

## 核心功能

### 原始业务模块

- **用户系统**：短信登录、JWT Token 刷新、Redis Session
- **商铺系统**：按类型浏览、按距离排序、详情查询
- **优惠券秒杀**：Redis 分布式锁 + Lua 脚本保证原子性、异步下单（Stream 消费队列）
- **探店博客**：发布、点赞、关注 Feed 流（Redis Sorted Set 滚动分页）
- **关注系统**：共同关注、关注推送

### AI 智能客服模块

- **流式对话**：SSE 流式输出，前端逐字显示
- **RAG 知识检索**：探店 Blog 向量化存入 Redis Stack，对话时自动召回相关测评
- **Tool Calling**：LLM 自主调用工具获取结构化数据
- **会话隔离**：按 userId:sessionId 隔离，不同用户/会话互不串扰
- **双层记忆**：Redis（热数据）+ MySQL（冷数据），token 溢出自动裁剪

## 工具调用

| 工具 | 类型 | 说明 |
|------|------|------|
| queryShopById | 查询 | 按商铺 ID 查询详情 |
| queryShopsByType | 查询 | 按类型查询商铺列表（支持距离排序） |
| queryShopByName | 查询 | 按名称模糊搜索商铺 |
| queryVoucherOfShop | 查询 | 查询商铺可用优惠券 |
| eserveShop | 写入 | 商铺预约（自动从会话获取 userId） |
| submitReport | 写入 | 提交举报（博客/评论/商家） |

## 项目结构

` 
com.hmdp
├── ai/                  # AI 多 Agent（意图分类、商铺/订单/举报 Agent）
├── cache/               # Redis 缓存工具类
├── config/              # 配置类（ChatMemory、RAG、Redisson、Knife4j、MVC）
├── controller/          # 接口层（含 AiChatController 流式对话入口）
├── dto/                 # 数据传输对象
├── entity/              # 实体类
├── mapper/              # MyBatis-Plus Mapper
├── policy/              # Token 窗口裁剪策略
├── repository/          # 双层 ChatMemoryStore（Redis + MySQL）
├── service/             # 服务层（含 AiChatService @AiService 声明）
│   └── impl/
├── task/                # 定时任务（记忆清理）
├── tool/                # Tool Calling 工具类（ShopTools、ReportTools 等）
└── utils/               # 工具类（UserHolder、RedisIdWorker、LoginInterceptor 等）
` 

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6+（普通 Redis，端口 6379）
- Redis Stack（向量库，端口 6380，Docker 部署）
- Maven 3.9+

### 1. 初始化数据库

`sql
CREATE DATABASE hmdianping DEFAULT CHARACTER SET utf8mb4;
`

执行 SQL 脚本：

`ash
# 主业务表
mysql -u root -p hmdianping < src/main/resources/db/hmdp.sql

# AI 客服扩展表（聊天记忆、预约、举报等）
mysql -u root -p hmdianping < src/main/resources/db/customer_service_upgrade.sql
`

### 2. 启动 Redis

`ash
# 普通 Redis（缓存/锁）
redis-server --port 6379

# Redis Stack（向量库）
docker run -d --name redis-stack -p 6380:6379 redis/redis-stack:latest
`

### 3. 配置 API Key

编辑 src/main/resources/application.yaml，替换 DashScope API Key：

`yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: 你的通义千问API-Key
    streaming-chat-model:
      api-key: 你的通义千问API-Key
  community:
    dashscope:
      embedding-model:
        api-key: 你的通义千问API-Key
`

### 4. 编译启动

`ash
mvn clean compile
mvn spring-boot:run
`

### 5. 初始化 RAG 向量库

项目启动后，调用管理端接口将 Blog 测评内容写入向量库：

`ash
POST http://localhost:8081/chat/rag/init
# 需登录用户 ID = 1（管理员）
`

## API 一览

| 接口 | 方法 | 说明 |
|------|------|------|
| /chat/flux | POST | AI 流式对话（SSE） |
| /chat/normal | POST | AI 普通对话（非流式） |
| /chat/rag/init | POST | 全量初始化 RAG 向量库 |
| /shop/{id} | GET | 查询商铺详情 |
| /shop/type/list | GET | 商铺类型列表 |
| /voucher/list/{shopId} | GET | 查询商铺优惠券 |
| /voucher-order/seckill/{id} | POST | 优惠券秒杀下单 |
| /blog/hot | GET | 热门博客 |
| /user/code | POST | 发送短信验证码 |
| /user/login | POST | 登录 |

Swagger UI: http://localhost:8081/doc.html

## 关键设计

### 会话记忆隔离

- Controller 生成 memoryId = userId:sessionId
- ChatMemoryProvider 按 memoryId 获取独立 Memory
- DualChatMemoryStore: Redis(热) + MySQL(冷)，token 超限时自动裁剪

### RAG + Tool Calling 协同

- **商铺推荐**：走 RAG，从 Blog 测评中语义召回，用自然语言描述推荐理由
- **商铺精确查询**：走 Tool Calling，从数据库拿结构化字段（ID、地址、人均、评分）
- **预约/举报**：走 Tool Calling，LLM 调用 reserveShop/submitReport 写入数据库

### 反幻觉约束

System Prompt 内置硬规则：
1. 禁止编造商铺信息，所有数据来自工具返回
2. 调用 reserveShop 前必须先通过工具获取真实商铺 ID
3. 禁止编造操作结果（预约单号、举报单号等）
4. 工具返回空结果时如实告知，不自行补充替代商铺
5. 注入当前系统时间，防止 LLM 时间认知偏差
