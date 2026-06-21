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

---

## 新人快速上手

### 一、需要安装的软件（本机环境）

新人只需在电脑上安装以下 **3 个基础工具**，项目所需的 Java 库由 Maven 自动下载，**无需手动安装**。

| 工具 | 最低版本 | 用途 | 是否必须 |
|------|----------|------|----------|
| **JDK** | 17 | 运行和编译 Java 代码 | 必须 |
| **Maven** | 3.6+ | 构建项目、下载依赖 | 必须 |
| **Git** | 2.x | 克隆代码仓库 | 必须 |

> **说明**：H2 数据库、Spring Boot、Lombok、PDFBox 等组件已在 `pom.xml` 中声明，首次执行 `mvn` 命令时会自动从 Maven 中央仓库下载，无需单独安装。

#### macOS 安装

```bash
# 使用 Homebrew 安装（推荐）
brew install openjdk@17 maven git

# 配置 JAVA_HOME（安装后执行一次）
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Windows 安装

1. 下载并安装 [JDK 17（Temurin）](https://adoptium.net/temurin/releases/?version=17)
2. 下载并安装 [Maven](https://maven.apache.org/download.cgi)，解压后将 `bin` 目录加入系统 `PATH`
3. 下载并安装 [Git for Windows](https://git-scm.com/download/win)
4. 新建环境变量 `JAVA_HOME`，指向 JDK 安装目录（如 `C:\Program Files\Eclipse Adoptium\jdk-17`）

#### Linux（Ubuntu/Debian）安装

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven git
```

#### 验证安装是否成功

```bash
java -version    # 应显示 17.x
mvn -version     # 应显示 Apache Maven 3.x，且 Java version 为 17
git --version    # 应显示 git version 2.x
```

三条命令均无报错，说明环境准备完毕。

---

### 二、项目 Maven 依赖（自动下载）

以下依赖在 `pom.xml` 中已配置，**首次构建时 Maven 会自动下载**，新人无需手动操作：

| 依赖 | 说明 |
|------|------|
| spring-boot-starter-web | Web 服务 |
| spring-boot-starter-thymeleaf | 页面模板引擎 |
| spring-boot-starter-data-jpa | 数据库 ORM |
| spring-boot-starter-websocket | WebSocket 支持 |
| h2 | 嵌入式内存数据库 |
| neo4j-java-driver | 图数据库驱动 |
| pdfbox / poi-ooxml | PDF、Word 文档解析 |
| lombok | 简化 Java 代码 |
| gson | JSON 处理 |

首次运行 `mvn spring-boot:run` 时，Maven 会下载依赖包到本地仓库（`~/.m2/repository`），可能需要 **3～10 分钟**（取决于网络速度）。请保持网络畅通，耐心等待。

---

### 三、启动项目（分步指南）

#### 步骤 1：克隆代码

```bash
git clone https://github.com/crystalgff2000/sqlBotAi.git
cd sqlBotAi
```

#### 步骤 2：编译项目（首次建议先编译，确认无报错）

```bash
mvn clean compile
```

看到 `BUILD SUCCESS` 表示编译通过。

#### 步骤 3：启动应用

**方式 A：Maven 命令启动（推荐）**

```bash
mvn spring-boot:run
```

**方式 B：打包后启动**

```bash
mvn clean package -DskipTests
java -jar target/sqlBotAi-1.0.0.jar
```

**方式 C：IDE 启动**

1. 用 IntelliJ IDEA 或 Cursor 打开项目根目录（包含 `pom.xml` 的文件夹）
2. 等待 IDE 自动导入 Maven 依赖（右下角进度条完成）
3. 找到 `src/main/java/com/sqlbot/SqlBotAiApplication.java`
4. 右键 → **Run 'SqlBotAiApplication'**（或点击类左侧的绿色运行按钮）

#### 步骤 4：确认启动成功

控制台出现类似以下日志，表示启动成功：

```
Started SqlBotAiApplication in x.xxx seconds
AI数据资产平台启动成功!
访问地址: http://localhost:8080
```

#### 步骤 5：浏览器访问

打开浏览器，访问：**http://localhost:8080**

能看到「AI数据资产平台」首页，且概念/实体等统计数字不为 0，说明运行正常。

---

### 四、IDE 推荐配置（可选）

#### IntelliJ IDEA / Cursor

1. **导入项目**：File → Open → 选择项目根目录
2. **JDK 设置**：File → Project Structure → Project SDK 选择 **JDK 17**
3. **编码设置**：Settings → Editor → File Encodings，全部设为 **UTF-8**
4. **Lombok 插件**：IDEA 需安装 Lombok 插件并开启 Annotation Processing（Cursor 一般已内置支持）
5. **Maven 刷新**：右键 `pom.xml` → Maven → Reload Project

---

### 五、常见问题

| 问题 | 解决方法 |
|------|----------|
| `java: error: release version 17 not supported` | 未安装 JDK 17，或未在 IDE 中选中 JDK 17 |
| `mvn: command not found` | Maven 未安装或未加入 PATH |
| 编译出现中文乱码 / unmappable character | 确认 IDE 文件编码为 UTF-8，或在终端执行 `export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8` |
| 端口 8080 被占用 | 修改 `application.yml` 中 `server.port`，或关闭占用 8080 的进程 |
| Maven 下载依赖很慢 | 可配置国内镜像，在 `~/.m2/settings.xml` 中添加阿里云 Maven 镜像 |
| 启动后页面空白或 404 | 确认访问地址为 `http://localhost:8080`，不要遗漏端口号 |

#### Maven 国内镜像配置（可选，加速依赖下载）

在 `~/.m2/settings.xml` 中添加：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

---

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
│   ├── application.yml       # 开发环境配置
│   ├── application-prod.yml  # 生产环境配置
│   └── templates/            # Thymeleaf 页面模板
├── deploy/              # 阿里云部署脚本
└── pom.xml
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
| `/h2-console` | H2 数据库控制台（仅开发环境） |

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

H2 控制台连接信息（开发环境）：

- JDBC URL: `jdbc:h2:mem:sqldb`
- 用户名: `sa`
- 密码: （空）

## 开发说明

- 项目启动时会自动初始化示例数据（概念、实体、数据资产、知识图谱）
- 源码编码统一为 UTF-8
- 开发环境使用 H2 内存数据库，重启后数据会重置

## 阿里云部署

### 前置条件

- 阿里云 ECS 实例（推荐 Ubuntu 22.04 / CentOS 7+）
- 安全组已放行 **80** 端口（HTTP）和 **22** 端口（SSH）
- 本地已配置 SSH 可登录服务器

### 一键部署

```bash
# 1. 配置服务器信息
cp deploy/deploy.env.example deploy/deploy.env
# 编辑 deploy.env，填写 SERVER_HOST（公网 IP）等

# 2. 执行部署
bash deploy/deploy.sh
```

部署脚本会自动完成：Maven 打包 → 上传 JAR → 安装 Java 17 / Nginx → 配置 systemd 服务 → 启动应用。

### 部署架构

```
用户 → Nginx (:80) → Spring Boot (:8080) → H2 文件数据库
```

### 服务管理

```bash
# 查看状态
systemctl status sqlbotai

# 重启服务
systemctl restart sqlbotai

# 查看日志
journalctl -u sqlbotai -f
tail -f /opt/sqlbotai/logs/sqlbotai.log
```

### 生产环境说明

- 使用 `prod` Profile，配置文件：`application-prod.yml`
- 数据持久化至 `/opt/sqlbotai/data/`
- 应用仅监听 `127.0.0.1:8080`，通过 Nginx 对外暴露 80 端口

## License

MIT
