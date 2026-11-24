# 🚄 火车票智能客服管理系统

基于 AI Agent 的火车票预订智能客服系统，采用前后端分离架构，集成 LangChain4j + 大模型 API 实现智能对话交互，支持查票、订票、改签、退订等全流程智能化处理。

## 📋 项目简介

本项目是一个完整的火车票智能客服管理系统，核心亮点是基于 AI Agent 实现的智能对话交互功能。用户可以通过自然语言与 AI 客服对话，完成车次查询、订票、改签、退票等操作，AI Agent 会自动识别用户意图并调用相应的业务接口完成处理。

### ✨ 核心亮点

1. **AI 智能客服 Agent**
   - 自然语言对话交互，支持文字对话
   - 智能意图识别（查班次、预订、退订、改签、票价咨询等）
   - AI Agent 自主调用后端接口，完成业务全流程
   - RAG 知识库问答（购票规则、退票手续费、改签规则等）

2. **完整的票务业务功能**
   - 车次查询、在线订票、订单管理
   - 在线改签、智能退票
   - 乘客信息管理

3. **完善的后台管理**
   - 用户管理、订单管理、车次管理、站点管理
   - AI 对话记录管理
   - 数据看板可视化统计

## 🏗️ 技术栈

### 后端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| Spring MVC | - | Web 层 |
| MyBatis-Plus | 3.5.5 | 持久层框架 |
| MySQL | 8.0 | 数据库 |
| LangChain4j | 1.0.0-beta2 | AI Agent 调度 |
| Milvus | - | 向量数据库（可选，支持降级） |
| Lombok | - | 简化代码 |
| Hutool | 5.8.25 | 工具库 |

### 前端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.4.21 | 前端框架 |
| Vite | 5.2.0 | 构建工具 |
| Element Plus | 2.8.7 | UI 组件库 |
| Vue Router | 4.3.0 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Axios | 1.7.7 | 网络请求 |
| ECharts | 5.5.0 | 数据可视化 |
| markdown-it | 14.1.0 | Markdown 渲染 |

### 开发工具
- **IDEA** / **VS Code**
- **Navicat** / **MySQL Workbench**
- **JDK 17**
- **Node.js** (>= 18)
- **Maven** (>= 3.8)

## 📁 项目结构

```
ticket-booking/
├── src/main/java/com/ticket/
│   ├── controller/          # 控制层（RESTful API）
│   ├── service/             # 业务逻辑层
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   ├── dto/                 # 数据传输对象
│   ├── vo/                  # 视图对象
│   ├── config/              # 配置类
│   ├── common/              # 公共组件
│   └── ai/                  # AI 模块
│       ├── agent/           # AI Agent 定义
│       ├── config/          # AI 配置
│       └── rag/             # 检索增强生成
│
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   ├── db/schema.sql        # 数据库脚本
│   └── knowledge/           # AI 知识库文件
│
└── app/ticket-booking-frontend/  # 前端项目
    ├── src/
    │   ├── api/             # API 接口
    │   ├── views/           # 页面组件
    │   ├── components/      # 公共组件
    │   ├── layouts/         # 布局组件
    │   ├── router/          # 路由配置
    │   ├── stores/          # 状态管理
    │   ├── utils/           # 工具函数
    │   └── styles/          # 样式文件
    │
    ├── package.json
    ├── vite.config.js
    └── index.html
```

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Maven 3.8+**
- **Docker**（可选，用于部署 Milvus）

### 1. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/schema.sql
```

默认创建的管理员账号：
- 用户名：`admin`
- 密码：`admin123`

测试用户账号：
- 用户名：`user`
- 密码：`user123`

### 2. 后端配置

编辑 `src/main/resources/application.yml`：

```yaml
# 数据源配置（修改为你的MySQL配置）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_booking
    username: root
    password: root

# AI配置（配置你的大模型API）
ai:
  chat-model:
    api-key: your-api-key
    model-name: qwen-max
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  embedding-model:
    api-key: your-api-key
    model-name: text-embedding-v2
```

### 3. 启动后端

```bash
# 进入项目根目录
cd ticket-booking

# 使用 Maven 启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/ticket-booking-1.0.0.jar
```

后端服务启动后访问：http://localhost:8080/api

### 4. 启动前端

```bash
# 进入前端项目目录
cd app/ticket-booking-frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 或者打包构建
npm run build
npm run preview
```

前端应用启动后访问：http://localhost:5173

### 5. 部署 Milvus（可选）

如需使用向量数据库存储知识库，可部署 Milvus：

```bash
# 使用 Docker 部署
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  -v /your-path/milvus:/var/lib/milvus \
  milvusdb/milvus:latest
```

> 如不部署 Milvus，请显式配置 `ai.vector-store.type=memory` 使用内存向量存储，仍需配置 Embedding 模型。

## 📖 API 接口文档

启动后端服务后，访问 Swagger 文档（已集成）：

```
http://localhost:8080/api/swagger-ui/index.html
```

### 主要接口列表

#### 用户模块 `/api/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录 |
| GET | /info | 获取当前用户信息 |
| PUT | /update | 更新用户信息 |
| GET | /list | 用户列表（管理端） |

#### 车次模块 `/api/train`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /query | 查询车次 |
| GET | /{id} | 获取车次详情 |
| GET | /list | 车次列表（管理端） |
| POST | /add | 新增车次 |
| PUT | /update | 更新车次 |

#### 订单模块 `/api/order`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /create | 创建订单 |
| POST | /pay/{orderNumber} | 支付订单 |
| GET | /my | 我的订单列表 |
| POST | /refund/{orderNumber} | 退票 |
| POST | /change | 改签 |

#### AI 客服模块 `/api/ai/chat`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /send | AI 对话 |
| GET | /session/{sessionId} | 获取会话记录 |
| GET | /my | 我的对话列表 |
| DELETE | /{id} | 删除对话记录 |

#### 管理模块 `/api/admin`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /stats | 数据看板统计 |
| GET | /chat/records | 对话记录管理 |
| GET | /chat/intents | 高频问题统计 |

## 📝 功能清单

### 用户端功能
- [x] 首页（快速查询、热门线路推荐）
- [x] 车次查询（按出发地、目的地、日期）
- [x] 在线订票（选车次、选座位、选乘客、下单）
- [x] 订单管理（待出行、已完成、已退票）
- [x] 在线改签（同线路改签、差价计算）
- [x] 在线退订（智能计算手续费）
- [x] 个人中心（资料修改、乘客管理）
- [x] AI 智能客服对话

### AI 客服功能
- [x] 自然语言对话交互
- [x] 意图智能识别
- [x] 自动调用业务接口
- [x] RAG 知识库问答
- [x] 多会话管理
- [x] 对话历史记录

### 管理端功能
- [x] 数据看板（订单量、购票率、退单率统计）
- [x] 用户管理（增删改查、禁用启用）
- [x] 订单管理（全量查看、异常处理）
- [x] 车次管理（CRUD、批量导入）
- [x] 站点管理（全国火车站维护）
- [x] AI 对话管理（记录查看、高频问题统计）

## 🎯 AI Agent 设计

### Agent 角色设定
```
你是"小铁"，一个专业的火车票智能客服助手。
你的职责是帮助用户查询车次、预订车票、办理改签和退票业务，
同时解答关于铁路出行的各种问题，包括购票规则、退票政策、
改签条件、学生票/儿童票优惠等。
```

### 工具函数
AI Agent 可调用以下工具函数完成业务操作：
- `queryTrains` - 查询车次
- `getTrainDetails` - 获取车次详情
- `calculateTicketPrice` - 计算票价
- `getStationList` - 获取站点列表
- `getRefundRules` - 退票规则
- `getChangeRules` - 改签规则
- `getStudentTicketRules` - 学生票规则
- `getChildTicketRules` - 儿童票规则

### RAG 知识库
系统预置了以下知识文档用于智能问答：
- 购票规则说明
- 退票政策详解
- 改签条件说明
- 学生票优惠政策
- 儿童票购票规则
- 全国主要站点列表

## 📸 项目截图

### 用户端
- 首页：快速查询入口 + 热门线路
- 车次查询：按日期/站点筛选
- AI 客服：智能对话界面

### 管理端
- 数据看板：订单趋势 + 热门线路
- 用户管理：CRUD 操作
- AI 对话管理：高频问题统计

## 🔧 常见问题

### Q: AI 对话回复为空？
检查 `application.yml` 中的 AI 配置是否正确，确保 API Key 有效。

### Q: 向量数据库连接失败？
如果不需要 RAG 功能，可以不部署 Milvus，系统会自动降级为内存存储。

### Q: 前端页面空白？
1. 确认后端服务已启动
2. 检查 `vite.config.js` 中的代理配置
3. 清除浏览器缓存后重试

### Q: 数据库连接失败？
检查 `application.yml` 中的数据库配置，确保 MySQL 服务已启动。

## 📄 许可证

本项目仅用于学习和展示目的。

## 👨‍💻 作者

火车票智能客服管理系统 - AI Agent 项目

---

**简历亮点描述：**

> 独立开发基于 AI Agent 的火车票智能客服管理系统，采用 SpringBoot + Vue3 前后端分离架构。核心亮点为集成 LangChain4j 实现 AI Agent 智能调度，支持自然语言对话交互、意图智能识别、业务接口自动调用等功能；基于 Milvus + RAG 实现铁路出行知识库智能问答。系统涵盖车次查询、在线订票、改签退订、后台管理等完整业务，包含订单与库存一致性控制，性能指标以实际部署压测为准。


## 本轮增强与验收

详见 [增强实现与验证](IMPLEMENTATION.md)，包含数据库新增表、订单状态规则、AI 确认与 SSE 接口、知识引用及测试边界。

## AI 环境变量

启动后端前设置 `DASHSCOPE_API_KEY` 和 `DASHSCOPE_EMBEDDING_API_KEY`，分别用于聊天模型和向量模型。请勿将真实密钥写入仓库。
