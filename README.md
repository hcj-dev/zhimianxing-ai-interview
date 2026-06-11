# 智面星 — AI 智能面试备战平台

基于大语言模型的在线面试备战系统，提供简历智能分析、AI 模拟面试、题库练习、岗位匹配等功能，帮助求职者高效备战面试。

## 功能概览

| 模块 | 说明 |
|------|------|
| **简历管理** | 上传 PDF 简历，AI 多维度分析打分（格式、内容、关键词、匹配度） |
| **模拟面试** | 选择岗位/方向，与 AI 面试官实时对话，支持 SSE 流式响应 |
| **面试报告** | 面试结束后自动生成评分报告，含逐题点评、薄弱点分析、六维雷达图 |
| **题库浏览** | 按分类/难度筛选题目，支持全文搜索（Qdrant 向量检索） |
| **错题本** | 模拟面试中的错题自动收录，可逐题复习 |
| **JD 匹配** | 上传岗位描述，AI 分析简历与 JD 的匹配度并给出改进建议 |
| **排行榜** | 综合排行（简历 + 面试）、简历评分排行、面试均分排行 |
| **管理后台** | 用户管理、题库管理、平台数据概览 |

## 技术架构

```
┌─────────────────────────┐
│   interview-ai-frontend │   Vue 3 + Element Plus + TypeScript
│   port :5173 (dev)      │
└──────────┬──────────────┘
           │ HTTP / SSE
┌──────────▼──────────────┐
│   interview-ai-backend  │   Spring Boot 3 + MyBatis-Plus
│   port :8080            │
├─────────────────────────┤
│   MySQL · Redis         │   数据持久化 & 缓存
│   RabbitMQ              │   异步消息（简历分析/面试报告/JD匹配）
│   Qdrant                │   向量数据库（题目语义搜索）
│   DeepSeek API          │   LLM 对话 & 分析
│   DashScope Embedding   │   文本向量化
└─────────────────────────┘
```

## 项目结构

```
├── interview-ai-frontend/       # 前端
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── components/          # 公共组件（侧边栏）
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── styles/              # 全局样式 & CSS 变量
│   │   ├── types/               # TypeScript 类型定义
│   │   └── views/               # 页面视图
│   │       ├── admin/           # 管理后台
│   │       ├── Dashboard.vue    # 仪表盘
│   │       ├── ResumeUpload.vue # 简历上传
│   │       ├── InterviewRoom.vue# 面试房间
│   │       ├── InterviewReport.vue # 面试报告
│   │       ├── QuestionBank.vue # 题库
│   │       ├── Leaderboard.vue  # 排行榜
│   │       └── ...
│   └── public/
│
├── interview-ai-backend/        # 后端
│   └── src/main/java/com/interviewai/
│       ├── common/              # 通用类（异常/JWT/结果封装）
│       ├── config/              # 配置（安全/向量库/消息队列/AI）
│       ├── consumer/            # RabbitMQ 消费者
│       ├── controller/          # REST 控制器
│       ├── dto/                 # 请求/响应 DTO
│       ├── entity/              # 数据库实体
│       ├── mapper/              # MyBatis Mapper
│       ├── service/             # 业务接口 & 实现
│       │   └── ai/              # AI 对话 & Embedding 服务
│       └── vo/                  # 视图对象
```

## 快速开始

### 环境要求

- **JDK** 17+
- **Node.js** 18+
- **MySQL** 8.0+
- **Redis** 7+
- **RabbitMQ** 3.x
- **Qdrant** 1.x

### 1. 后端启动

```bash
cd interview-ai-backend

# 复制配置文件，按实际情况修改
cp src/main/resources/application.example.yml src/main/resources/application.yml

# 编辑 application.yml，填入数据库连接、Redis、RabbitMQ、API Key 等配置
# 然后启动
./mvnw spring-boot:run
```

### 2. 前端启动

```bash
cd interview-ai-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:5173 即可使用。

### 3. 生产构建

```bash
# 前端
cd interview-ai-frontend && npm run build   # 输出到 dist/

# 后端
cd interview-ai-backend && ./mvnw package   # 输出到 target/
```

## 配置文件

后端 `application.yml` 示例见 `interview-ai-backend/src/main/resources/application.example.yml`。关键配置项：

| 配置项 | 说明 |
|--------|------|
| `spring.datasource` | MySQL 数据库连接 |
| `spring.data.redis` | Redis 连接 |
| `spring.rabbitmq` | RabbitMQ 连接 |
| `spring.ai.openai.api-key` | LLM API Key（默认 DeepSeek） |
| `dashscope.api-key` | 阿里云 DashScope Embedding Key |
| `app.qdrant` | Qdrant 向量数据库地址 |
| `jwt.secret` | JWT 签名密钥（生产环境务必修改） |

## 许可证

MIT License
