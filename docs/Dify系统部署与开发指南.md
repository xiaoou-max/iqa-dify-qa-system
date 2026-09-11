# Dify 系统部署与开发完整指南

> **文档版本**: v1.0  
> **最后更新**: 2025-08-23  
> **适用对象**: 系统管理员、开发工程师、运维工程师

---

## 目录

- [一、账号与密码信息](#一账号与密码信息)
- [二、系统架构设计](#二系统架构设计)
- [三、部署指南](#三部署指南)
- [四、核心功能实现](#四核心功能实现)
- [五、技术难点与解决方案](#五技术难点与解决方案)
- [六、常见问题排查](#六常见问题排查)

---

## 一、账号与密码信息

### 1.1 服务器访问凭证

| 项目 | 详情 |
|------|------|
| **腾讯云公网 IP** | `YOUR_PUBLIC_HOST` |
| **Linux VM 内网 IP** | `192.168.1.100` |
| **操作系统** | Ubuntu Server (Linux) |
| **SSH 用户名** | `root` |
| **SSH 密码** | `jszx2022.` (注意：末尾有一个点) |
| **SSH 端口** | `22` |
| **SSH 连接命令** | `ssh root@192.168.1.100` |

### 1.2 腾讯云服务账号

| 项目 | 详情 |
|------|------|
| **SecretId** | `AKIDL6xnY7b2MVSJ6oeZQ7d1FH6i9SdfZJuK` |
| **SecretKey** | `KWbWyGRQDuVxGi6KSUPBnJkCWhfDhBas` |
| **用途** | 腾讯云 API 调用、云服务器管理 |

### 1.3 Dify 系统账号

#### Linux VM 部署 (公网访问)

| 项目 | 详情 |
|------|------|
| **访问地址** | `http://YOUR_PUBLIC_HOST:8082` |
| **登录邮箱** | `difyadmin@dify.com` |
| **登录密码** | `123456` |
| **备选账号** | `3194162154@qq.com` (蝴蝶蓝) |
| **部署目录** | `/opt/dify/docker` |

#### Windows 本地部署

| 项目 | 详情 |
|------|------|
| **访问地址** | `http://localhost:8081` |
| **初始邮箱** | `admin@example.com` |
| **初始密码** | `admin123456` |
| **部署目录** | `D:\BaiduNetdiskDownload\智能问答-自研前后端\dify-deploy\docker` |

### 1.4 数据库访问信息

#### Linux VM - PostgreSQL

| 项目 | 详情 |
|------|------|
| **容器名** | `docker-db_postgres-1` |
| **主机** | 容器内部访问或 `localhost:5432` |
| **用户名** | `postgres` |
| **密码** | `difyai123456` |
| **主数据库** | `dify` (应用数据) |
| **插件数据库** | `dify_plugin` (插件守护进程数据) |
| **连接命令** | `docker exec -it docker-db_postgres-1 psql -U postgres -d dify` |

#### Windows 本地 - PostgreSQL

| 项目 | 详情 |
|------|------|
| **容器名** | `dify-db` |
| **用户名** | `dify` |
| **密码** | `CHANGE_ME` |
| **数据库** | `dify` |
| **环境变量** | `DB_USERNAME=dify`, `DB_PASSWORD=CHANGE_ME` |

### 1.5 Redis 缓存信息

| 项目 | 详情 |
|------|------|
| **Linux VM 容器** | `docker-redis-1` |
| **Windows 容器** | `dify-redis` |
| **密码 (Linux)** | 无密码 (容器内部) |
| **密码 (Windows)** | `dify_redis_pwd_2026_Lp2c` |
| **默认端口** | `6379` |

### 1.6 Weaviate 向量数据库

| 项目 | 详情 |
|------|------|
| **容器名** | `dify-weaviate` |
| **API Key** | `dify_weaviate_2026_Wv9k` |
| **端口** | `8080` |
| **数据目录** | `./volumes/weaviate` |

### 1.7 大模型 API Key

#### 阿里云 DashScope (百炼)

| 项目 | 详情 |
|------|------|
| **API Key (Linux VM)** | `YOUR_DASHSCOPE_API_KEY_vippeLj_rzsyUXqg8NiNnAiEAlxDfHlpwTJpYK0zoX0XQoJeUe-v5Lf8Yzvir_A9e_Yw` |
| **API Key (Windows)** | `YOUR_DASHSCOPE_API_KEY` |
| **支持模型** | `qwen3.7-plus`, `qwen3.8-max`, `text-embedding-v3` |
| **API 端点** | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| **Embedding 端点** | `https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings` |

### 1.8 FRP 内网穿透配置

| 项目 | 详情 |
|------|------|
| **FRP 服务器** | `YOUR_PUBLIC_HOST:7000` |
| **认证 Token** | `CHANGE_ME` |
| **本地映射** | `192.168.1.100:80` → 公网 `8082` |
| **配置文件** | `frpc-vm.toml` |
| **Windows 客户端路径** | `frp-client/frp_0.61.1_windows_amd64/` |

### 1.9 Dify 安全密钥

| 项目 | 详情 |
|------|------|
| **SECRET_KEY** | `sk-dify-secret-2026-a8f3c9e2b7d4a1f6c5e8d3b9a2c7f1e5d4a8b3c9e6f2d1b4a7c9e3f5d8a2b7c1` |
| **PLUGIN_DAEMON_KEY** | `plugin-api-key` |
| **SANDBOX_API_KEY** | `sandbox-api-key-2026-Xc8v` |
| **工作区域名** | `sandbox` |

### 1.10 已部署的 Dify 应用

| 应用名称 | 应用 ID | 类型 | API Token |
|----------|---------|------|-----------|
| 洛阳市房产销售智能问答 | `4aa10005-c0cc-40c6-bee4-2f6a84073d32` | Chatflow (advanced-chat) | `YOUR_DIFY_APP_TOKEN` |
| 广西交投财务智能助手 | `807f6f1b-d9ab-40ca-92ac-65b5a8acd6c8` | Agent Chat | - |

---

## 二、系统架构设计

### 2.1 整体架构图

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                        用户访问层                              │
                    │  ┌──────────────┐  ┌──────────────────────────────────────┐  │
                    │  │  公网用户     │  │           局域网/本机用户               │  │
                    │  └──────┬───────┘  └──────────────────┬───────────────────┘  │
                    └─────────┼─────────────────────────────┼──────────────────────┘
                              │                             │
                              ▼                             ▼
                    ┌─────────────────────────────────────────────────────────────┐
                    │                      网络接入层                              │
                    │  ┌──────────────────────┐  ┌──────────────────────────────┐  │
                    │  │  FRP 内网穿透 (8082)  │  │        本地端口访问 (8081)    │  │
                    │  └──────────┬───────────┘  └───────────────┬──────────────┘  │
                    └─────────────┼─────────────────────────────┼────────────────┘
                                  │                             │
                                  ▼                             ▼
                    ┌─────────────────────────────────────────────────────────────┐
                    │                       Nginx 反向代理                         │
                    │  ┌───────────────────────────────────────────────────────┐  │
                    │  │  /console/api/ → dify-api:5001                        │  │
                    │  │  /api/         → dify-api:5001                        │  │
                    │  │  /v1/          → dify-api:5001                        │  │
                    │  │  /console      → dify-web:3000                        │  │
                    │  │  /app          → dify-web:3000                        │  │
                    │  │  /plugin/      → plugin-daemon:5002                   │  │
                    │  └───────────────────────────────────────────────────────┘  │
                    └─────────────────────────────────────────────────────────────┘
                                  │                             │
                    ┌─────────────┴─────────────────────────────┴────────────────┐
                    │                                                              │
                    ▼                                                              ▼
          ┌────────────────────────────────────────┐          ┌─────────────────────────────┐
          │           Dify 后端服务                 │          │      Dify 插件守护进程       │
          │  ┌──────────────────────────────────┐  │          │  ┌─────────────────────┐  │
          │  │  dify-api (Flask)                │  │          │  │  plugin-daemon      │  │
          │  │  - 用户认证                      │  │          │  │  - LLM 模型调用     │  │
          │  │  - 工作流执行                    │  │          │  │  - Embedding 服务   │  │
          │  │  - API 网关                      │  │          │  │  - 工具管理         │  │
          │  └──────────────────────────────────┘  │          │  └─────────────────────┘  │
          │  ┌──────────────────────────────────┐  │          └─────────────────────────────┘
          │  │  dify-worker (Celery)            │  │
          │  │  - 异步任务处理                  │  │
          │  │  - 邮件发送                      │  │
          │  └──────────────────────────────────┘  │
          │  ┌──────────────────────────────────┐  │
          │  │  dify-beat (Celery Beat)         │  │
          │  │  - 定时任务调度                  │  │
          │  └──────────────────────────────────┘  │
          └────────────────────────────────────────┘
                                  │
                    ┌─────────────┴────────────────────────────────────────────────┐
                    │                                                              │
                    ▼                                                              ▼
          ┌────────────────────────────────┐                    ┌────────────────────────────┐
          │          Dify 前端              │                    │        外部服务依赖        │
          │  ┌──────────────────────────┐  │                    │  ┌──────────────────────┐  │
          │  │  dify-web (Next.js)      │  │                    │  │  DashScope API       │  │
          │  │  - 控制台 UI              │  │                    │  │  - LLM 对话          │  │
          │  │  - 应用编辑器             │  │                    │  │  - 文本向量生成      │  │
          │  │  - 工作流可视化           │  │                    │  └──────────────────────┘  │
          │  └──────────────────────────┘  │                    │  ┌──────────────────────┐  │
          └────────────────────────────────┘                    │  │  Sandbox 服务        │  │
                                                                │  │  - 代码执行环境     │  │
          ┌──────────────────────────────────────────────────────┘  └──────────────────────┘  │
          │                                                              │                      │
          ▼                                                              ▼                      │
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│                              数据存储层                                                     │
│  ┌──────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  PostgreSQL      │  │    Redis     │  │  Weaviate    │  │       本地文件存储        │  │
│  │  - 用户数据      │  │  - 会话缓存  │  │  - 向量检索  │  │  - 上传文件              │  │
│  │  - 工作流配置    │  │  - 任务队列  │  │  - 语义搜索  │  │  - 应用资源              │  │
│  │  - 模型凭据      │  │  - 状态存储  │  │              │  │  - 日志文件              │  │
│  └──────────────────┘  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 容器服务清单

| 容器名 | 镜像 | 端口 | 说明 |
|--------|------|------|------|
| `dify-nginx` | `nginx:1.27-alpine` | `8081:80` | 统一入口，反向代理 |
| `dify-api` | `langgenius/dify-api:1.16.1` | 内部 `5001` | API 后端服务 |
| `dify-web` | `langgenius/dify-web:1.16.1` | 内部 `3000` | 前端 UI 服务 |
| `dify-worker` | `langgenius/dify-api:1.16.1` | - | Celery 异步任务 |
| `dify-beat` | `langgenius/dify-api:1.16.1` | - | 定时任务调度 |
| `dify-db` | `postgres:15-alpine` | 内部 `5432` | PostgreSQL 数据库 |
| `dify-redis` | `redis:6-alpine` | 内部 `6379` | Redis 缓存 |
| `dify-weaviate` | `semitechnologies/weaviate:1.27.0` | 内部 `8080` | 向量数据库 |
| `dify-sandbox` | `langgenius/dify-sandbox:0.2.1` | 内部 `8194` | 代码执行沙箱 |
| `plugin-daemon` | 自定义 Python | 内部 `5002` | 插件守护进程 |

### 2.3 网络拓扑

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Windows 主机 (192.168.1.x)                         │
│                                                                                 │
│  ┌─────────────────────────────────────────────┐                                │
│  │  FRP 客户端 (frpc.exe)                       │                                │
│  │  ┌─────────────────────────────────────┐   │                                │
│  │  │ 本地: 192.168.1.100:80  ────────────┼───┼──> 公网: YOUR_PUBLIC_HOST:8082 │
│  │  └─────────────────────────────────────┘   │                                │
│  └─────────────────────────────────────────────┘                                │
│                                                                                 │
│  ┌─────────────────────────────────────────────┐                                │
│  │  Dify Windows 部署 (Docker)                 │                                │
│  │  端口: 8081 (Nginx)                         │                                │
│  └─────────────────────────────────────────────┘                                │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ SSH (192.168.1.100:22)
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Linux VM (192.168.1.100)                              │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Dify Docker Compose 部署 (/opt/dify/docker)                            │   │
│  │  ┌─────────┐  ┌─────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │
│  │  │  Nginx  │  │   API   │  │     Web      │  │  Plugin Daemon       │   │   │
│  │  │  :80    │  │  :5001  │  │    :3000     │  │      :5002           │   │   │
│  │  └─────────┘  └─────────┘  └──────────────┘  └──────────────────────┘   │   │
│  │  ┌─────────┐  ┌─────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │
│  │  │   DB    │  │  Redis  │  │  Weaviate    │  │      Sandbox        │   │   │
│  │  │:5432    │  │  :6379  │  │   :8080      │  │      :8194          │   │   │
│  │  └─────────┘  └─────────┘  └──────────────┘  └──────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ FRP 端口映射
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          腾讯云服务器 (YOUR_PUBLIC_HOST)                           │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  FRP 服务端 (frps.exe)                                                  │   │
│  │  监听端口: 7000 (控制), 8082 (数据)                                     │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.4 数据库表结构关系图

```
┌─────────────────────────────────────────────────────────────────────┐
│                              apps (应用)                            │
├─────────────────────────────────────────────────────────────────────┤
│  id (UUID)                     ←── 主键                             │
│  tenant_id (UUID)              ←── 租户 ID                         │
│  name (VARCHAR)                ←── 应用名称                        │
│  mode (VARCHAR)                ←── 应用类型 (chatflow/agent/...)   │
│  app_model_config_id (UUID)    ←── 外键 → app_model_configs.id      │
│  workflow_id (UUID)            ←── 外键 → workflows.id              │
│  enable_api (BOOLEAN)          ←── 是否启用 API                     │
│  status (VARCHAR)              ←── 状态                            │
└──────────────┬──────────────────────────────────────────────────────┘
               │
    ┌──────────┴──────────┐
    ▼                      ▼
┌─────────────────┐  ┌───────────────────────────────────────────────┐
│app_model_configs│  │                workflows (工作流)                │
├─────────────────┤  ├───────────────────────────────────────────────────┤
│ id (UUID)       │  │ id (UUID)                                        │
│ app_id (UUID)   │  │ app_id (UUID)           ←── 关联应用              │
│ provider (VARCHAR)│  │ version (INTEGER)     ←── 版本号                │
│ model_id (UUID) │  │ status (VARCHAR)      ←── 状态 (draft/published) │
│ model (JSON)    │  │ graph (JSON)          ←── 工作流图结构            │
│ configs (JSON)  │  │ created_by (UUID)     ←── 创建人                │
└─────────────────┘  │ created_at (TIMESTAMP)                          │
                     │ updated_at (TIMESTAMP)                          │
                     └───────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                  provider_credentials (提供商凭据)                  │
├─────────────────────────────────────────────────────────────────────┤
│  id (UUID)                      ←── 主键                           │
│  tenant_id (UUID)               ←── 租户 ID                         │
│  provider_type (VARCHAR)        ←── 提供商类型                      │
│  provider_name (VARCHAR)       ←── 提供商名称                      │
│  encrypted_config (TEXT)        ←── RSA 加密的凭据配置              │
│  is_valid (BOOLEAN)             ←── 是否有效                        │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│              provider_model_credentials (模型凭据)                   │
├─────────────────────────────────────────────────────────────────────┤
│  id (UUID)                      ←── 主键                           │
│  provider_credential_id (UUID)  ←── 外键 → provider_credentials.id  │
│  model_name (VARCHAR)           ←── 模型名称                        │
│  model_type (VARCHAR)           ←── 模型类型 (llm/embedding/...)    │
│  encrypted_config (TEXT)        ←── RSA 加密的模型配置              │
│  is_valid (BOOLEAN)             ←── 是否有效                        │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                  accounts (用户账号)                                │
├─────────────────────────────────────────────────────────────────────┤
│  id (UUID)                      ←── 主键                           │
│  tenant_id (UUID)               ←── 租户 ID                         │
│  email (VARCHAR)                ←── 登录邮箱                        │
│  name (VARCHAR)                 ←── 用户名                          │
│  password (VARCHAR)             ←── 加密后的密码                    │
│  status (VARCHAR)               ←── 状态                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 三、部署指南

### 3.1 Linux VM 环境初始化

#### 步骤 1: SSH 连接

```bash
ssh root@192.168.1.100
# 密码: jszx2022. (注意末尾的点)
```

#### 步骤 2: 禁用 SSH 反向 DNS 查询 (加速连接)

```bash
# 编辑 sshd_config
vi /etc/ssh/sshd_config

# 添加或修改以下配置
UseDNS no
GSSAPIAuthentication no

# 重启 SSH 服务
systemctl restart sshd
```

#### 步骤 3: 更新系统

```bash
apt update && apt upgrade -y
```

#### 步骤 4: 安装 Docker 和 Docker Compose

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash

# 安装 Docker Compose
apt install docker-compose-plugin

# 验证安装
docker --version
docker compose version
```

### 3.2 Dify 部署

#### 步骤 1: 获取部署文件

```bash
cd /opt
git clone https://github.com/langgenius/dify.git
cd dify/docker
```

#### 步骤 2: 配置环境变量

```bash
# 复制示例配置
cp .env.example .env

# 编辑配置
vi .env
```

关键配置项:
```env
# 数据库配置
DB_USERNAME=postgres
DB_PASSWORD=<你的密码>
DB_HOST=db
DB_PORT=5432
DB_DATABASE=dify

# Redis 配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<你的Redis密码>

# 安全密钥 (生成方式见下方)
SECRET_KEY=<使用 openssl rand -hex 32 生成>

# 公网访问配置 (重要!)
CONSOLE_API_URL=http://YOUR_PUBLIC_HOST:8082
CONSOLE_WEB_URL=http://YOUR_PUBLIC_HOST:8082
APP_API_URL=http://YOUR_PUBLIC_HOST:8082
APP_WEB_URL=http://YOUR_PUBLIC_HOST:8082
SERVICE_API_URL=http://YOUR_PUBLIC_HOST:8082

# CSRF 配置 (临时禁用)
CSRF_TRUSTED_ORIGINS=*
WTF_CSRF_ENABLED=False
ENABLE_CSRF=false
```

#### 步骤 3: 配置 Nginx

```bash
# 编辑 Nginx 配置
vi nginx/conf.d/default.conf
```

关键路由配置:
```nginx
# API 路由
location /console/api/ {
    proxy_pass http://api:5001/console/api/;
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 300s;
    proxy_send_timeout 300s;
    proxy_buffering off;
}

location /api/ {
    proxy_pass http://api:5001/api/;
    # ... 同上
}

location /v1/ {
    proxy_pass http://api:5001/v1/;
    # ... 同上
}

# 插件路由 (重要!)
location /plugin/ {
    proxy_pass http://plugin_daemon:5002;
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 300s;
}

# 前端路由
location /console {
    rewrite ^/console(.*)$ /$1 break;
    proxy_pass http://web:3000;
}

location /app {
    proxy_pass http://web:3000/app;
}

location / {
    proxy_pass http://web:3000;
}
```

#### 步骤 4: 启动服务

```bash
cd /opt/dify/docker

# 初始化并启动
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f nginx
docker compose logs -f api
```

#### 步骤 5: 初始化 Dify

1. 访问 `http://YOUR_PUBLIC_HOST:8082`
2. 首次访问会跳转到安装页面
3. 设置管理员邮箱和密码
4. 完成初始化

### 3.3 FRP 内网穿透配置

#### Windows 主机配置

```toml
# frpc-vm.toml
serverAddr = "YOUR_PUBLIC_HOST"
serverPort = 7000

auth.method = "token"
auth.token = "CHANGE_ME"

log.to = "./frpc-vm.log"
log.level = "info"

# 将 Linux VM 的 80 端口映射到公网 8082
[[proxies]]
name = "dify-vm"
type = "tcp"
localIP = "192.168.1.100"
localPort = 80
remotePort = 8082
```

#### 启动 FRP 客户端

```bash
# 在 Windows 主机的 cmd 中运行
cd D:\BaiduNetdiskDownload\智能问答-自研前后端\frp-client\frp_0.61.1_windows_amd64
frpc.exe -c frpc-vm.toml
```

### 3.4 插件守护进程配置

插件守护进程是一个自定义 Python 服务，用于处理模型调用。

#### 文件位置
```
/opt/dify/docker/volumes/plugin_daemon/
```

#### 启动方式
```bash
# 在 Linux VM 上启动
cd /opt/dify/docker/volumes/plugin_daemon
python3 plugin_daemon.py &
```

#### 核心代码结构
```python
# plugin_daemon.py 关键配置
DASHSCOPE_API_KEY = "你的 DashScope API Key"
TENANT_ID = "你的租户 ID"
PLUGIN_UNIQUE_ID = "langgenius/tongyi/tongyi"

# 支持的模型列表
MODELS = [
    "qwen3.7-plus",
    "qwen3.8-max", 
    "text-embedding-v3",
    "text-embedding-v2"
]
```

---

## 四、核心功能实现

### 4.1 大模型 (LLM) 集成

#### 步骤 1: 在 Dify 控制台配置模型

1. 进入 **设置** → **模型供应商**
2. 选择 **通义 (DashScope)**
3. 填写 DashScope API Key
4. 点击 **保存**

#### 步骤 2: 数据库直接配置 (备选方案)

如果前端配置失败，可以通过数据库直接插入:

```sql
-- 1. 插入 provider_credentials
INSERT INTO provider_credentials (id, tenant_id, provider_type, provider_name, encrypted_config, is_valid)
VALUES (
    uuid_generate_v4(),
    '36810dd3-e395-4d40-9600-f0c89441e596',
    'custom',
    'langgenius/tongyi/tongyi',
    '<RSA加密后的配置>',
    true
);

-- 2. 插入 provider_model_credentials
INSERT INTO provider_model_credentials (id, provider_credential_id, model_name, model_type, encrypted_config, is_valid)
VALUES (
    uuid_generate_v4(),
    '<上面的id>',
    'qwen3.7-plus',
    'llm',
    '{}',
    true
);
```

#### 步骤 3: RSA 密钥管理

Dify 使用 RSA 加密存储模型凭据:

```python
# 在 API 容器内执行
docker exec -it docker-api-1 python

from app.core.security import decrypt_token, encrypt_token

# 加密凭据
config = {"dashscope_api_key": "your-api-key"}
encrypted = encrypt_token(json.dumps(config))

# 解密凭据
decrypted = decrypt_token(encrypted)
```

### 4.2 工作流配置

#### 查看当前工作流

```sql
-- 查询工作流
SELECT id, app_id, status, version, created_at 
FROM workflows 
WHERE app_id = '4aa10005-c0cc-40c6-bee4-2f6a84073d32';
```

#### 更新工作流 LLM 节点配置

```python
import json

# 获取当前工作流
workflow = get_workflow_from_db(workflow_id)
graph = json.loads(workflow.graph)

# 更新 LLM 节点的模型配置
for node in graph["nodes"]:
    if node["data"]["type"] == "llm":
        node["data"]["model"] = {
            "provider": "langgenius/tongyi/tongyi",
            "name": "qwen3.7-plus",
            "mode": "chat"
        }

# 保存更新后的工作流
update_workflow_in_db(workflow_id, json.dumps(graph))
```

### 4.3 应用 API 调用

#### 获取 API Token

```sql
-- 查询 API Token
SELECT a.name, a.mode, t.access_token 
FROM apps a 
JOIN api_tokens t ON a.id = t.app_id 
WHERE a.enable_api = true;
```

#### 调用对话接口

```python
import requests

# 使用 API Token
api_token = "YOUR_DIFY_APP_TOKEN"

# 调用对话
url = "http://YOUR_PUBLIC_HOST:8082/v1/chat-messages"
headers = {
    "Authorization": f"Bearer {api_token}",
    "Content-Type": "application/json"
}
data = {
    "inputs": {},
    "query": "你好，请介绍一下洛阳的房产",
    "response_mode": "blocking",  # 或 "streaming"
    "user": "test_user_001"
}

response = requests.post(url, headers=headers, json=data)
result = response.json()
print(result["answer"])
```

#### 流式响应模式

```python
response = requests.post(url, headers=headers, json=data, stream=True)

for line in response.iter_lines():
    if line:
        data = json.loads(line)
        if data.get("event") == "message":
            print(data["data"]["answer"], end="", flush=True)
```

### 4.4 Agent 应用配置

#### Agent 应用模型配置

```sql
-- 插入 Agent 应用的模型配置
INSERT INTO app_model_configs (id, app_id, provider, model_id, model, configs, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    '<APP_ID>',
    'langgenius/tongyi/tongyi',
    '<MODEL_ID>',
    '{"provider":"langgenius/tongyi/tongyi","name":"qwen3.7-plus","mode":"chat"}'::json,
    '{"context_size":128000,"max_tokens":8192,"mode":"chat","platform":"dashscope","tools":[]}'::json,
    NOW(),
    NOW()
);

-- 更新应用关联
UPDATE apps 
SET app_model_config_id = '<新配置ID>' 
WHERE id = '<APP_ID>';
```

---

## 五、技术难点与解决方案

### 5.1 网络访问问题

#### 问题描述
Linux VM 无法访问外部互联网，导致模型 API 调用失败。

#### 根因分析
- 默认网关不可达
- DNS 解析失败
- 无法直接访问 DashScope API

#### 解决方案

**方案 A: 配置 Windows 代理服务器**

```python
# proxy_server.py - 在 Windows 主机上运行
from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.request

class ProxyHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self._proxy()
    
    def do_POST(self):
        self._proxy()
    
    def _proxy(self):
        # 代理转发请求
        pass

# 启动代理
server = HTTPServer(('0.0.0.0', 8888), ProxyHandler)
server.serve_forever()
```

**方案 B: 修改 Linux VM 网关**

```bash
# 添加默认网关指向 Windows 主机
ip route add default via 192.168.1.1 dev ens33

# 配置 DNS
echo "nameserver 8.8.8.8" >> /etc/resolv.conf
```

### 5.2 登录认证问题

#### 问题描述
Dify 前端登录返回 `Invalid encrypted data` 错误。

#### 根因分析
Dify 前端将密码 Base64 编码后发送，API 需要解码后验证。

#### 解决方案

**方法 1: 使用正确的登录格式**

```python
import base64
import requests

# Base64 编码密码
password = base64.b64encode(b'123456').decode()

# 登录请求
response = requests.post(
    'http://localhost/console/api/login',
    json={
        'email': 'difyadmin@dify.com',
        'password': password  # Base64 编码后的密码
    }
)
```

**方法 2: 禁用 CSRF 检查 (临时)**

```python
# 修改 /app/api/libs/token.py
# 在 check_csrf_token 函数中直接返回 True

def check_csrf_token(request):
    return True  # 临时禁用 CSRF 检查
```

### 5.3 模型凭据加密问题

#### 问题描述
模型凭据加密后无法解密，导致 `KeyError: 'dashscope_api_key'`。

#### 根因分析
- RSA 密钥不匹配
- 数据库中存储的加密凭据无法正确解密

#### 解决方案

**步骤 1: 重新生成 RSA 密钥**

```python
# 在 API 容器内执行
docker exec -it docker-api-1 python

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend

# 生成新的 RSA 密钥对
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048,
    backend=default_backend()
)

# 保存密钥
private_pem = private_key.private_bytes(...)
public_pem = private_key.public_key().public_bytes(...)
```

**步骤 2: 修复插件代码**

```python
# 修改 plugin_daemon.py 中的凭据获取方式
# 将硬编码的 credentials["dashscope_api_key"] 改为:
api_key = credentials.get("dashscope_api_key", "")
```

### 5.4 工作流同步问题

#### 问题描述
前端显示"同步数据中"长时间加载。

#### 根因分析
- 插件守护进程数据库缺少必要记录
- Nginx 未正确转发 `/plugin/` 请求

#### 解决方案

**修复 Nginx 插件路由**

```nginx
# 必须在 Nginx 配置中添加插件路由
location /plugin/ {
    proxy_pass http://plugin_daemon:5002;
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 300s;
    proxy_send_timeout 300s;
}
```

**清理无效的工具提供者引用**

```sql
-- 查找引用不存在工作流的工具提供者
SELECT * FROM tool_api_providers 
WHERE workflow_id NOT IN (SELECT id FROM workflows);

-- 删除无效记录
DELETE FROM tool_api_providers 
WHERE workflow_id NOT IN (SELECT id FROM workflows);
```

### 5.5 数据库 JSON 格式问题

#### 问题描述
`ERROR: invalid input syntax for type json` 错误。

#### 根因分析
JSON 字符串格式错误，键名缺少双引号。

#### 解决方案

```python
# 错误的 JSON 格式
configs = '{"context_size":128000}'  # 正确
configs = '{context_size:128000}'    # 错误！缺少引号

# 正确的生成方式
import json
config_dict = {"context_size": 128000, "max_tokens": 8192}
configs_json = json.dumps(config_dict, separators=(',', ':'))
# 结果: {"context_size":128000,"max_tokens":8192}
```

### 5.6 公网访问配置问题

#### 问题描述
公网访问 Dify 时，API 请求返回错误。

#### 根因分析
Dify 内部使用 `localhost` 构建 URL，公网用户无法访问。

#### 解决方案

**配置公网 URL 环境变量**

```env
# 在 .env 文件中配置
CONSOLE_API_URL=http://YOUR_PUBLIC_HOST:8082
CONSOLE_WEB_URL=http://YOUR_PUBLIC_HOST:8082
APP_API_URL=http://YOUR_PUBLIC_HOST:8082
APP_WEB_URL=http://YOUR_PUBLIC_HOST:8082
SERVICE_API_URL=http://YOUR_PUBLIC_HOST:8082
```

**注意**: 修改后需要重启所有服务:
```bash
docker compose down
docker compose up -d
```

### 5.7 Agent 应用配置页加载失败

#### 问题描述
Agent 应用配置页显示"加载中"，API 返回 404。

#### 根因分析
`app_model_configs` 表中缺少配置记录，`model` 字段格式错误。

#### 解决方案

**步骤 1: 插入正确的模型配置**

```sql
INSERT INTO app_model_configs (id, app_id, provider, model_id, model, configs, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    '807f6f1b-d9ab-40ca-92ac-65b5a8acd6c8',
    'langgenius/tongyi/tongyi',
    '075eba29-05a6-4936-bafc-d8dfe7c8500c',
    '{"provider":"langgenius/tongyi/tongyi","name":"qwen3.7-plus","mode":"chat"}'::json,
    '{"context_size":128000,"max_tokens":8192,"mode":"chat","platform":"dashscope","tools":[]}'::json,
    NOW(),
    NOW()
);
```

**步骤 2: 更新应用关联**

```sql
UPDATE apps 
SET app_model_config_id = 'e935cd90-3682-43e6-82c8-68684882e8bd'
WHERE id = '807f6f1b-d9ab-40ca-92ac-65b5a8acd6c8';
```

---

## 六、常见问题排查

### 6.1 查看服务状态

```bash
# 查看所有容器状态
docker compose ps

# 查看特定容器状态
docker compose ps nginx
docker compose ps api

# 查看健康检查状态
docker inspect --format='{{json .State.Health}}' docker-api-1 | python -m json.tool
```

### 6.2 查看日志

```bash
# 实时查看 API 日志
docker compose logs -f api

# 查看最近 100 行 API 日志
docker compose logs --tail 100 api

# 查看 Nginx 日志
docker compose logs -f nginx

# 查看插件守护进程日志
docker logs docker-plugin_daemon-1 --tail 50
```

### 6.3 数据库操作

```bash
# 连接 PostgreSQL
docker exec -it docker-db_postgres-1 psql -U postgres -d dify

# 常用查询
\d apps                    # 查看 apps 表结构
SELECT * FROM apps;        # 查看所有应用
SELECT * FROM provider_credentials;  # 查看模型凭据
SELECT * FROM workflows;   # 查看工作流
```

### 6.4 API 测试

```bash
# 测试 API 健康检查
curl http://localhost/health
# 或
curl http://localhost:5001/health

# 测试登录
curl -X POST http://localhost/console/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"difyadmin@dify.com","password":"MTIzNDU2"}'

# 测试应用对话
curl -X POST http://localhost/v1/chat-messages \
  -H "Authorization: Bearer YOUR_DIFY_APP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"inputs":{},"query":"你好","response_mode":"blocking","user":"test"}'
```

### 6.5 服务重启

```bash
# 重启所有服务
docker compose restart

# 重启特定服务
docker compose restart api
docker compose restart nginx

# 完全重建 (谨慎使用)
docker compose down
docker compose up -d
```

### 6.6 数据备份

```bash
# 导出数据库
docker exec docker-db_postgres-1 pg_dump -U postgres dify > dify_backup.sql

# 导出插件数据库
docker exec docker-db_postgres-1 pg_dump -U postgres dify_plugin > dify_plugin_backup.sql

# 压缩备份
gzip dify_backup.sql
gzip dify_plugin_backup.sql

# 恢复数据
cat dify_backup.sql | docker exec -i docker-db_postgres-1 psql -U postgres -d dify
```

---

## 附录

### A. 文件路径索引

| 用途 | Linux VM 路径 | Windows 路径 |
|------|---------------|--------------|
| Dify 配置 | `/opt/dify/docker/.env` | `dify-deploy/docker/.env` |
| Nginx 配置 | `/opt/dify/docker/nginx/conf.d/default.conf` | `dify-deploy/docker/nginx.conf` |
| 插件代码 | `/opt/dify/docker/plugin_daemon.py` | `dify-deploy/plugin-daemon/plugin_daemon_v3.py` |
| FRP 配置 | - | `frp-client/frp_0.61.1_windows_amd64/frpc-vm.toml` |
| 数据库卷 | `/opt/dify/docker/volumes/db/data` | `dify-deploy/docker/volumes/db/data` |

### B. 快捷命令参考

```bash
# 一键启动所有服务
cd /opt/dify/docker && docker compose up -d

# 一键停止所有服务
cd /opt/dify/docker && docker compose down

# 查看服务状态汇总
cd /opt/dify/docker && docker compose ps

# 重启 API 服务
docker compose restart api

# 清理所有数据 (谨慎!)
docker compose down -v
```

### C. 常见错误码

| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| `401 Unauthorized` | 未认证 | 检查登录状态或 API Token |
| `403 Forbidden` | 无权限 | 检查用户角色和权限 |
| `404 Not Found` | 资源不存在 | 检查 URL 和资源 ID |
| `500 Internal Error` | 服务器错误 | 查看日志排查原因 |
| `invalid_param` | 参数无效 | 检查请求参数格式 |
| `KeyError` | 键不存在 | 检查凭据字段是否完整 |

---

**文档结束**

> ⚠️ **安全提示**: 本文档包含敏感信息，请妥善保管，切勿泄露给无关人员。生产环境部署时请更换所有默认密码和密钥。
