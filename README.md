# sqlBotAi — AI 数据资产平台

基于 Spring Boot 的企业数据资产统一管理与智能应用平台：Wiki 知识库浏览、本地 RAG 问答、业务查数（自然语言 → SQL → MySQL）、知识图谱与资产元数据管理。

## 功能特性

| 模块 | 说明 |
|------|------|
| **Wiki 知识库** | 浏览指标、表资产、概念与领域参考文档 |
| **知识问答** | 意图路由：业务查数 / RAG / Wiki 关键词 / LLM 回退 |
| **业务查数** | 基于 Wiki 口径生成受控 SQL，经沙箱查询 MySQL ADS，返回表格与分析 |
| **知识图谱** | 从 Wiki 链接与元数据自动构建关系图 |
| **概念 / 实体 / 数据资产** | 元数据 CRUD 与示例数据展示 |
| **数据可视化** | 基于种子数据的统计图表 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、Java 17、Spring Data JPA |
| 前端 | Thymeleaf、Bootstrap 5 |
| 智能 | LangChain4j、DeepSeek（问答/SQL）、DashScope Embedding（RAG） |
| 元数据 | H2（开发：内存；生产：文件库） |
| 业务数据 | MySQL ADS（经 Python `query_mysql.py` 只读查询） |
| 部署 | Nginx、systemd、Maven、Shell 脚本 |

---

## 目录

1. [环境要求](#1-环境要求)
2. [本地安装与启动](#2-本地安装与启动)
3. [生产环境部署](#3-生产环境部署)
4. [配置说明](#4-配置说明)
5. [部署后运维](#5-部署后运维)
6. [项目结构与路由](#6-项目结构与路由)
7. [常见问题](#7-常见问题)

---

## 1. 环境要求

### 本地开发（本机）

| 工具 | 最低版本 | 是否必须 | 说明 |
|------|----------|----------|------|
| JDK | 17 | 必须 | 编译与运行 |
| Maven | 3.6+ | 必须 | 构建与依赖下载 |
| Git | 2.x | 必须 | 克隆仓库 |
| Python 3 | 3.8+ | 建议 | 业务查数脚本；不做查数可暂不装 |
| DeepSeek API Key | — | 建议 | 知识问答 / SQL 生成 |
| DashScope API Key | — | 建议 | RAG 向量检索 |

> `pom.xml` 中的依赖会在首次 `mvn` 时自动下载，无需手动安装。

### 生产服务器

| 项 | 要求 |
|----|------|
| 系统 | Ubuntu 22.04 / CentOS 7+ 等 Linux |
| 权限 | 能以 `root`（或具备 sudo）登录 SSH |
| 安全组 | 放行 **22**（SSH）、**80**（HTTP） |
| 预装依赖 | 一键部署脚本会自动安装 Java 17、Nginx、Python3、mysql 客户端等 |

---

## 2. 本地安装与启动

### 2.1 安装基础工具

**macOS（Homebrew）：**

```bash
brew install openjdk@17 maven git python3
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Ubuntu / Debian：**

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven git python3
```

**验证：**

```bash
java -version   # 17.x
mvn -version    # Maven 3.x + Java 17
git --version
```

### 2.2 获取代码

```bash
git clone https://github.com/crystalgff2000/sqlBotAi.git
cd sqlBotAi
```

### 2.3 准备本地配置（必做）

真实密钥 **不要提交到 Git**。仓库只提供示例配置：

```bash
# 1. 应用配置：从示例复制
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 2. 注入 API Key（推荐用环境变量，避免写进文件）
export DEEPSEEK_API_KEY="你的 DeepSeek Key"
export DASHSCOPE_API_KEY="你的 DashScope Key"
```

说明：

- `application.yml` 已被 `.gitignore` 忽略，可按需修改端口、查数 SSH 等。
- 不配置 API Key 时，基础页面与 Wiki 浏览仍可用，但知识问答 / RAG / SQL 生成会失败或降级。

### 2.4 编译与启动

```bash
# 首次建议先编译确认
mvn clean compile

# 启动（开发环境，默认端口 8080）
mvn spring-boot:run
```

或打包后启动：

```bash
mvn clean package -DskipTests
java -jar target/sqlBotAi-1.0.0.jar
```

IDE 启动：打开项目 → 运行 `com.sqlbot.SqlBotAiApplication`（项目 SDK 选 JDK 17）。

### 2.5 验证本地启动

控制台出现：

```text
Started SqlBotAiApplication ...
访问地址: http://localhost:8080
```

浏览器访问：

| 地址 | 说明 |
|------|------|
| http://localhost:8080 | 首页 |
| http://localhost:8080/knowledge-base | 知识问答 |
| http://localhost:8080/wiki | Wiki 知识库 |
| http://localhost:8080/knowledge-graph | 知识图谱 |

开发环境 H2 控制台：http://localhost:8080/h2-console  
JDBC URL：`jdbc:h2:mem:sqldb`，用户名 `sa`，密码留空。

### 2.6 （可选）本地业务查数

业务查数默认通过 SSH 连接远端 MySQL。需在 `application.yml` 中配置：

```yaml
query-business-data:
  enabled: true
  ssh-host: <MySQL 所在主机>
  ssh-user: root
  # ssh-identity-file: ~/.ssh/id_ed25519
  mysql-config: /root/.ai-data-query.cnf
```

并保证本机能 SSH 到该主机，且远端存在 MySQL 客户端配置文件。

---

## 3. 生产环境部署

部署产物结构：

```text
用户浏览器
    ↓ :80
Nginx（反向代理）
    ↓ 127.0.0.1:8080
Spring Boot（profile=prod，用户 sqlbotai）
    ├── H2 文件库  → /opt/sqlbotai/data/
    ├── 配置/密钥  → /opt/sqlbotai/config/env
    └── 业务查数   → 本机 mysql 客户端（connection-mode=local）
```

相关脚本均在 `deploy/` 目录：

| 脚本 | 作用 |
|------|------|
| `package-artifacts.sh` | 本地打包 JAR → `dist/sqlbotai-deploy.tar.gz` |
| `deploy.sh` | 打包 + 上传 + 远程安装（一键） |
| `install-on-server.sh` | 服务器端：更新应用、写配置、重启服务 |
| `install-server.sh` | 服务器首次：安装 Java/Nginx/Python 等环境 |
| `deploy.env.example` | 部署配置模板（复制为 `deploy.env`） |

### 3.1 方式一：本机一键部署（推荐）

适合：你在本机有代码，能 SSH 到目标服务器。

**步骤 1 — 填写部署配置**

```bash
cd sqlBotAi
cp deploy/deploy.env.example deploy/deploy.env
```

编辑 `deploy/deploy.env`（该文件已被 `.gitignore` 忽略）：

```bash
SERVER_HOST=你的服务器公网IP     # 必填，例如 120.48.17.175
SERVER_USER=root
SERVER_PORT=22

# 二选一：SSH 密钥（推荐）或密码
SSH_KEY=~/.ssh/id_ed25519
# SSH_PASSWORD=你的SSH密码      # 密码方式需安装 sshpass：brew install hudochenkov/sshpass/sshpass

APP_HOME=/opt/sqlbotai

# 写入服务器 /opt/sqlbotai/config/env 的密钥（勿提交）
DB_PASSWORD=
DEEPSEEK_API_KEY=sk-xxxx
DASHSCOPE_API_KEY=sk-xxxx
```

> 注意：`SSH_PASSWORD` **只能出现一次**。若后面又写了空的 `SSH_PASSWORD=`，会覆盖前面的值导致登录失败。

**步骤 2 — 执行一键部署**

```bash
bash deploy/deploy.sh
```

脚本自动完成三步：

1. `mvn clean package` 并组装 `dist/sqlbotai-deploy.tar.gz`
2. `scp` 上传到服务器 `/tmp/sqlbotai-deploy.tar.gz`
3. SSH 执行解压 + `install-on-server.sh`（首次会调用 `install-server.sh` 装环境）

**步骤 3 — 验证**

浏览器打开：`http://<SERVER_HOST>`  
例如：http://120.48.17.175

### 3.2 方式二：手动上传 + 服务器安装

适合：已 SSH 登录服务器，或不便用一键脚本。

**本机打包：**

```bash
cd sqlBotAi
bash deploy/package-artifacts.sh
# 生成：dist/sqlbotai-deploy.tar.gz
```

在运行打包前，请先配置好 `deploy/deploy.env`（至少包含 API Key），打包时会生成内嵌的 `server.env`。

**上传：**

```bash
scp dist/sqlbotai-deploy.tar.gz root@<服务器IP>:/tmp/
```

**服务器上安装：**

```bash
cd /tmp
rm -rf sqlbotai-bundle
tar xzf sqlbotai-deploy.tar.gz
bash sqlbotai-bundle/install-on-server.sh /tmp/sqlbotai-bundle
```

- **首次部署**：没有 systemd 服务时，会先执行 `install-server.sh`（安装 Java 17、Nginx、Python、创建用户等）。
- **再次部署**：只更新 JAR、配置与 Nginx，并 `systemctl restart sqlbotai`。

### 3.3 部署成功后的目录

| 路径 | 说明 |
|------|------|
| `/opt/sqlbotai/app/` | JAR 与 exploded 运行目录 |
| `/opt/sqlbotai/data/` | H2 库、RAG 缓存、查数结果 |
| `/opt/sqlbotai/logs/` | 应用日志 `sqlbotai.log` |
| `/opt/sqlbotai/config/env` | `DB_PASSWORD` / API Key（权限 600） |
| `/opt/sqlbotai/config/mysql.cnf` | 若存在 `/root/.ai-data-query.cnf` 会自动复制 |

---

## 4. 配置说明

### 本地（`application.yml`）

由 `application.yml.example` 复制而来，常用项：

| 配置项 | 说明 |
|--------|------|
| `server.port` | 默认 `8080` |
| `deepseek.api-key` | 推荐 `${DEEPSEEK_API_KEY:}` |
| `dashscope.api-key` | 推荐 `${DASHSCOPE_API_KEY:}` |
| `rag.*` | RAG 开关、分块、Top-K、缓存目录 |
| `query-business-data.*` | 业务查数开关、SSH/本地连接、行数上限 |

### 生产（`application-prod.yml` + 环境变量）

- 启动参数：`--spring.profiles.active=prod`（见 `deploy/sqlbotai.service`）
- 应用只监听 `127.0.0.1:8080`，对外由 Nginx 提供 80 端口
- API Key / DB 密码来自 `/opt/sqlbotai/config/env`
- 业务查数：`connection-mode: local`（同机 mysql 客户端）

### 敏感文件（勿提交）

以下内容已在 `.gitignore` 中忽略：

- `deploy/deploy.env`、`**/server.env`
- `src/main/resources/application.yml`
- `dist/` 部署包、`data/` 本地运行数据
- `*.pem`、`*.cnf`、SSH 私钥等

模板文件可提交：`deploy.env.example`、`application.yml.example`。

---

## 5. 部署后运维

在服务器上执行：

```bash
# 服务状态
systemctl status sqlbotai

# 重启 / 停止 / 启动
systemctl restart sqlbotai
systemctl stop sqlbotai
systemctl start sqlbotai

# 实时日志
journalctl -u sqlbotai -f

# 应用日志文件
tail -f /opt/sqlbotai/logs/sqlbotai.log

# 本机探测应用是否起来（应返回 200）
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/

# Nginx 状态与配置检测
systemctl status nginx
nginx -t
```

更新版本：在本机改代码后，重新执行 `bash deploy/deploy.sh`（或方式二手动上传安装即可）。

---

## 6. 项目结构与路由

### 结构（简要）

```text
sqlBotAi/
├── src/main/java/com/sqlbot/
│   ├── controller/          # Web / API
│   ├── service/             # 问答、Wiki、RAG、图谱
│   ├── service/query/       # 业务查数 Skill
│   ├── service/rag/         # 向量检索
│   └── config/              # 配置与初始化
├── src/main/resources/
│   ├── application.yml.example
│   ├── application-prod.yml
│   ├── skills/query-business-data/   # 查数 Skill 与脚本
│   ├── wiki/                         # Wiki 知识库（本地，默认不入库）
│   └── templates/                    # 页面
├── deploy/                  # 部署脚本与 systemd / nginx 配置
├── docs/                    # 技术方案等文档
└── pom.xml
```

### 页面路由

| 路径 | 说明 |
|------|------|
| `/` | 首页 |
| `/wiki` | Wiki 知识库 |
| `/knowledge-base` | 知识问答 |
| `/knowledge-graph` | 知识图谱 |
| `/concepts` | 概念管理 |
| `/entities` | 实体管理 |
| `/data-assets` | 数据资产 |
| `/charts` | 图表 |
| `/h2-console` | H2 控制台（仅开发环境） |

### 常用 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/knowledge-base/api/chat` | POST | 统一问答入口 |
| `/knowledge-base/api/rag/rebuild` | POST | 重建 RAG 索引 |
| `/wiki/api/catalog` | GET | Wiki 目录 |
| `/knowledge-graph/api/graph` | GET | 图谱数据 |

统一响应格式：

```json
{ "code": 200, "message": "success", "data": {} }
```

---

## 7. 常见问题

| 问题 | 处理建议 |
|------|----------|
| `release version 17 not supported` | 安装 JDK 17，并在 IDE / `JAVA_HOME` 中指定 17 |
| 端口 8080 被占用 | `lsof -ti :8080 \| xargs kill -9`，或改 `server.port` |
| Maven 下载慢 | 在 `~/.m2/settings.xml` 配置阿里云镜像（见下方） |
| 本地问答提示未配置 API Key | 检查 `DEEPSEEK_API_KEY` / `DASHSCOPE_API_KEY` 环境变量或 `application.yml` |
| 一键部署 `Permission denied` | 检查 `deploy.env` 中密码/密钥；确认 `SSH_PASSWORD` 未被后面空行覆盖；密码方式需安装 `sshpass` |
| 部署后页面不是应用 | 检查 `systemctl status sqlbotai`、`nginx -t`、安全组是否放行 80 |
| 服务启动失败 | `journalctl -u sqlbotai -n 50 --no-pager` |
| 业务查数失败 | 确认 MySQL 客户端配置、SSH/本地连接模式、ADS 库权限 |

### Maven 国内镜像（可选）

`~/.m2/settings.xml`：

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

## License

MIT
