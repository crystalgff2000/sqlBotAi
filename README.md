# sqlBotAi — AI 数据资产平台

基于 Spring Boot 的企业数据资产统一管理与智能应用平台，提供概念/实体/数据资产管理、知识库问答、知识图谱可视化与数据统计图表等功能。

## 功能特性

| 模块 | 说明 |
|------|------|
| **概念管理** | 管理数据概念、业务概念与技术概念，支持分类与关键词搜索 |
| **实体管理** | 维护数据库表、API 接口等业务实体信息 |
| **数据资产** | 统一管理企业数据资产，支持增删改查 |
| **数据可视化** | 概念分布、实体趋势、资产统计等多维度图表展示 |
| **知识库** | 支持 PDF/Word 等文档上传，提供智能问答能力 |
| **知识图谱** | 基于文档或数据自动生成知识图谱，可视化展示实体关系 |

## 技术栈

- **后端**：Spring Boot 3.2、Spring Data JPA、Spring WebSocket
- **前端**：Thymeleaf、Bootstrap 5、Bootstrap Icons
- **数据库**：H2（内存数据库，开发环境）
- **文档处理**：Apache PDFBox、Apache POI
- **图数据库驱动**：Neo4j Java Driver
- **构建工具**：Maven
- **JDK**：17

## 项目结构

```
sqlBotAi/
├── src/main/java/com/sqlbot/
│   ├── config/          # 配置与数据初始化
│   ├── controller/      # Web 控制器
│   ├── service/         # 业务逻辑
│   ├── repository/      # 数据访问层
│   ├── entity/          # JPA 实体
│   └── dto/             # 数据传输对象
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── templates/       # Thymeleaf 页面模板
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 启动项目

```bash
# 克隆项目
git clone https://github.com/crystalgff2000/sqlBotAi.git
cd sqlBotAi

# 编译并运行
mvn spring-boot:run
```

启动成功后访问：**http://localhost:8080**

### 其他启动方式

```bash
# 仅编译
mvn clean compile

# 打包
mvn clean package

# 运行 JAR
java -jar target/sqlBotAi-1.0.0.jar
```

## 页面路由

| 路径 | 说明 |
|------|------|
| `/` | 首页 |
| `/concepts` | 概念管理 |
| `/entities` | 实体管理 |
| `/data-assets` | 数据资产 |
| `/charts` | 数据图表 |
| `/knowledge-base` | 知识库 |
| `/knowledge-graph` | 知识图谱 |
| `/h2-console` | H2 数据库控制台 |

## API 接口

所有 REST API 返回统一格式 `ResponseResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 模块 | 接口 | 方法 |
|------|------|------|
| 概念 | `/concepts/api/all` | GET |
| 概念 | `/concepts/api/{id}` | GET |
| 概念 | `/concepts/api/save` | POST |
| 概念 | `/concepts/api/search?keyword=` | GET |
| 实体 | `/entities/api/all` | GET |
| 数据资产 | `/data-assets/api/all` | GET |
| 图表 | `/charts/api/all` | GET |
| 知识库 | `/knowledge-base/api/documents` | GET |
| 知识库 | `/knowledge-base/api/upload` | POST |
| 知识库 | `/knowledge-base/api/chat` | POST |
| 知识图谱 | `/knowledge-graph/api/graph` | GET |

## 配置说明

主要配置位于 `src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | `jdbc:h2:mem:sqldb` | H2 内存数据库 |
| `spring.servlet.multipart.max-file-size` | 50MB | 文件上传大小限制 |

H2 控制台连接信息：

- JDBC URL: `jdbc:h2:mem:sqldb`
- 用户名: `sa`
- 密码: （空）

## 开发说明

- 项目启动时会自动初始化示例数据（概念、实体、数据资产、知识图谱）
- 源码编码统一为 UTF-8
- IDE 推荐使用 IntelliJ IDEA 或 Cursor，导入 Maven 项目即可

## License

MIT
