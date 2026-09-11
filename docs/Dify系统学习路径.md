# Dify 系统管理学习路径

> **适用人群**: 零基础入门者 · 系统管理员 · 运维工程师  
> **学习周期**: 约 2-3 周（每天 1-2 小时）  
> **目标**: 独立管理 Dify 系统，能排查和解决常见问题

---

## 📚 学习路径总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Dify 系统管理学习路线图                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  阶段一: 打好基础 (Week 1)                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Day 1: Linux 基础命令                                            │   │
│  │  Day 2: SSH 远程连接与文件传输                                      │   │
│  │  Day 3: Docker 基础概念与常用命令                                  │   │
│  │  Day 4: Docker Compose 编排                                        │   │
│  │  Day 5: 阶段练习 - 启动/停止/重启服务                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  阶段二: 网络与存储 (Week 2)                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Day 6: Nginx 反向代理原理与配置                                   │   │
│  │  Day 7: 网络排错 - 端口/路由/防火墙                                │   │
│  │  Day 8: PostgreSQL 数据库基础                                      │   │
│  │  Day 9: 数据库备份与恢复                                           │   │
│  │  Day 10: 阶段练习 - 配置公网访问                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  阶段三: Dify 进阶 (Week 3)                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Day 11: Dify 架构与核心概念                                        │   │
│  │  Day 12: 模型配置与 API 集成                                        │   │
│  │  Day 13: 工作流 (Workflow) 管理                                     │   │
│  │  Day 14: 常见故障排查实战                                           │   │
│  │  Day 15: 综合练习 - 部署新应用                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 学习目标

完成本学习路径后，你将能够：

1. ✅ **独立远程管理** Linux 服务器
2. ✅ **熟练使用** Docker 和 Docker Compose 管理容器
3. ✅ **配置** Nginx 反向代理实现公网访问
4. ✅ **管理** PostgreSQL 数据库和 Redis 缓存
5. ✅ **配置** Dify 模型凭据和 API 集成
6. ✅ **排查** 系统常见故障（登录失败、API 错误、网络问题等）
7. ✅ **部署** 新的 Dify 应用并进行基本配置

---

## 第一阶段：打好 Linux 和 Docker 基础

### Day 1: Linux 基础命令

#### 学习目标
掌握远程服务器的基本操作，能够查看文件、管理进程、监控系统资源。

#### 核心命令速查表

```bash
# ==================== 1. 文件和目录操作 ====================

# 查看当前目录
pwd

# 列出文件
ls                    # 简单列表
ls -la                # 详细列表（含权限、大小、时间）
ls -lh                # 人类可读的大小格式
ls /opt/dify          # 查看指定目录

# 切换目录
cd /opt/dify          # 进入指定目录
cd ..                 # 返回上一级
cd ~                  # 返回用户主目录
cd -                  # 返回上一个访问的目录

# 创建目录
mkdir new_folder
mkdir -p /path/to/nested/directory  # 递归创建多级目录

# 创建文件
touch file.txt

# 查看文件内容
cat file.txt          # 完整查看
head -20 file.txt     # 查看前 20 行
tail -20 file.txt     # 查看后 20 行
tail -f app.log       # 实时查看日志（Ctrl+C 退出）

# 复制文件
cp file.txt backup.txt
cp -r folder/ backup_folder/   # 复制整个目录

# 移动/重命名
mv old_name.txt new_name.txt
mv file.txt /target/path/

# 删除
rm file.txt
rm -rf folder/        # 递归删除目录（危险！）

# ==================== 2. 权限管理 ====================

# 查看文件权限
ls -l file.txt
# 输出示例: -rw-r--r-- 1 root root 1234 Aug 23 10:00 file.txt
#          |  |  |
#          |  |  +-- 其他用户 (r--)
#          |  +----- 组用户   (r--)
#          +-------- 文件所有者(rw-)

# 修改权限
chmod 644 file.txt    # 所有者读写, 其他只读
chmod +x script.sh    # 添加执行权限
chmod 755 directory/  # 目录常用权限

# 修改所有者
chown newuser file.txt
chown -R newuser:newgroup folder/   # 递归修改

# ==================== 3. 进程管理 ====================

# 查看进程
ps                    # 当前用户的进程
ps aux                # 所有进程（详细）
top                   # 实时进程监控（q 退出）

# 查找特定进程
ps aux | grep nginx
pgrep -la python

# 管理进程
kill 1234             # 优雅停止进程
kill -9 1234          # 强制杀死进程
systemctl stop nginx  # 停止服务
systemctl start nginx # 启动服务
systemctl restart nginx # 重启服务
systemctl status nginx # 查看服务状态

# ==================== 4. 系统资源监控 ====================

# 查看 CPU 和内存
free -h               # 内存使用情况
top                   # 实时监控 (q 退出)

# 查看磁盘
df -h                 # 磁盘空间使用
du -sh /opt/dify      # 查看目录大小
du -sh /opt/dify/*     # 查看子目录大小

# 查看网络
netstat -tlnp         # 查看监听的端口
ss -tlnp              # 更快的替代命令
lsof -i :8082         # 查看哪个进程占用了 8082 端口

# ==================== 5. 搜索和查找 ====================

# 在文件中搜索
grep "error" log.txt           # 搜索包含 "error" 的行
grep -i "ERROR" log.txt        # 忽略大小写
grep -n "error" log.txt        # 显示行号
grep -r "config" /opt/dify/    # 递归搜索目录

# 查找文件
find /opt/dify -name "*.log"   # 查找所有 .log 文件
find /opt/dify -name ".env"    # 查找 .env 文件

# ==================== 6. 压缩和解压 ====================

# tar 打包
tar -czvf archive.tar.gz folder/      # 压缩
tar -xzvf archive.tar.gz              # 解压到当前目录
tar -xzvf archive.tar.gz -C /target/  # 解压到指定目录

# zip 压缩 (如果安装了)
zip -r archive.zip folder/
unzip archive.zip

# ==================== 7. 编辑器使用 ====================

# 使用 vi (需要学习基础操作)
vi /opt/dify/docker/.env

# vi 基本操作:
# i         - 进入编辑模式
# Esc       - 退出编辑模式
# :w        - 保存
# :wq       - 保存并退出
# :q!       - 不保存退出
# /keyword  - 搜索关键词

# 或者使用 nano (更简单)
nano /opt/dify/docker/.env
# Ctrl+O 保存, Ctrl+X 退出
```

#### 动手练习

```bash
# 1. 登录服务器
ssh root@192.168.1.100
# 输入密码: jszx2022.

# 2. 查看系统信息
echo "系统版本:" && cat /etc/os-release | head -3
echo "运行时间:" && uptime
echo "内存使用:" && free -h | grep Mem
echo "磁盘使用:" && df -h / | tail -1

# 3. 查看 Dify 目录结构
cd /opt/dify/docker
ls -lah
echo "---"
du -sh */

# 4. 实时查看最近的错误日志
docker compose logs --tail 50 2>&1 | grep -i error

# 5. 修改 .env 文件并验证
cp .env .env.backup    # 先备份
vi .env               # 编辑
grep CONSOLE_API_URL .env  # 验证修改
```

#### 学习建议
- 不需要记住所有命令，知道有什么命令可以完成什么功能
- 遇到忘记的命令，用 `man command` 查看帮助，或 `command --help`
- 多练习，熟能生巧

---

### Day 2: SSH 远程连接与文件传输

#### 学习目标
掌握远程管理 Linux 服务器的基本方法，能够安全地连接、传输文件。

#### 核心知识点

**1. SSH 连接原理**

```
┌──────────────┐        SSH 协议        ┌──────────────┐
│  你的电脑     │ ◄────────────────────► │  Linux 服务器 │
│  (Windows)   │    加密通信通道        │  (192.168.1.100) │
└──────────────┘                        └──────────────┘
```

**2. 连接方式**

```bash
# 基本连接
ssh root@192.168.1.100

# 指定端口 (如果不是默认 22)
ssh -p 22 root@192.168.1.100

# 使用配置文件简化连接
# 编辑 ~/.ssh/config:
# Host dify-server
#   HostName 192.168.1.100
#   User root
#   Port 22
# 然后就可以用: ssh dify-server
```

**3. 密码 vs 密钥认证**

```bash
# 方式 A: 密码认证 (简单但不安全)
# 直接输入密码即可

# 方式 B: 密钥认证 (推荐)
# 在你的 Windows 电脑上生成密钥
ssh-keygen -t rsa -b 4096
# 会生成:
#   ~/.ssh/id_rsa      (私钥, 不要泄露!)
#   ~/.ssh/id_rsa.pub  (公钥, 放到服务器上)

# 将公钥复制到服务器
ssh-copy-id root@192.168.1.100

# 之后就可以免密登录了
ssh root@192.168.1.100  # 不再需要密码
```

**4. 文件传输**

```bash
# SCP 命令 (简单)

# 上传文件到服务器
scp local_file.txt root@192.168.1.100:/opt/dify/docker/

# 从服务器下载文件
scp root@192.168.1.100:/var/log/app.log ./

# 上传整个目录
scp -r local_folder/ root@192.168.1.100:/opt/dify/

# 使用 rsync (适合同步大量文件)
rsync -avz ./local_folder/ root@192.168.1.100:/opt/dify/remote_folder/
```

**5. Windows 下的工具推荐**

| 工具 | 说明 |
|------|------|
| **PowerShell** | Windows 自带，支持 SSH |
| **MobaXterm** | 功能强大，集成终端+SFTP |
| **WinSCP** | 专用的 SFTP/SCP 客户端 |
| **FileZilla** | 通用 FTP/SFTP 客户端 |
| **VS Code Remote SSH** | 在 VS Code 中直接编辑远程文件 |

#### 动手练习

```bash
# === 练习 1: 使用 SSH 连接 ===

# 打开 PowerShell，连接服务器
ssh root@192.168.1.100
# 密码: jszx2022.

# 在服务器上执行一些命令
echo "欢迎来到 Dify 服务器!"
hostname
date
whoami

# === 练习 2: 文件传输 ===

# 在本地创建一个测试文件
echo "测试内容" > test_file.txt

# 上传到服务器
scp test_file.txt root@192.168.1.100:/opt/dify/docker/

# 在服务器上验证
ssh root@192.168.1.100 "ls -la /opt/dify/docker/test_file.txt"

# 从服务器下载文件
scp root@192.168.1.100:/opt/dify/docker/.env ./

# === 练习 3: 端口转发 ===

# 将服务器的 5001 端口转发到本地 15001
# 这样本地可以通过 localhost:15001 访问服务器的 API
ssh -L 15001:localhost:5001 -N -f root@192.168.1.100

# 然后在本地测试
curl http://localhost:15001/health
```

#### 常见问题

| 问题 | 解决方案 |
|------|----------|
| `Connection refused` | 检查服务器 SSH 服务是否运行：`systemctl status sshd` |
| `Connection timed out` | 检查网络连通性，防火墙设置 |
| `Permission denied` | 检查用户名、密码，或密钥配置 |
| `Host key verification failed` | 执行 `ssh-keygen -R 192.168.1.100` 删除旧记录 |

---

### Day 3: Docker 基础与常用命令

#### 学习目标
理解 Docker 的核心概念，能够管理容器和镜像。

#### 核心概念图解

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker 核心概念                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  镜像 (Image)                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  一个只读的模板，包含运行应用所需的所有内容               │   │
│  │  - 操作系统文件系统                                      │   │
│  │  - 应用程序代码                                         │   │
│  │  - 运行时环境 (Python, Node.js, ...)                    │   │
│  │  - 依赖库                                                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          │ docker run                          │
│                          ▼                                      │
│  容器 (Container)                                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  镜像的一个运行实例（可读写）                             │   │
│  │  - 独立的进程空间                                        │   │
│  │  - 独立的网络空间                                        │   │
│  │  - 独立的文件系统 (但可以挂载宿主机目录)                 │   │
│  │  - 可以启动、停止、删除                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  仓库 (Registry)                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  存储镜像的地方                                          │   │
│  │  - Docker Hub (公共)                                     │   │
│  │  - 阿里云镜像仓库                                        │   │
│  │  - 本地私有仓库                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 核心命令速查表

```bash
# ==================== 1. 镜像管理 ====================

# 查看本地镜像
docker images
docker images | head -10        # 只看前 10 个

# 拉取镜像
docker pull nginx:latest
docker pull postgres:15-alpine

# 搜索镜像
docker search nginx

# 删除镜像
docker rmi nginx:latest
docker image prune              # 删除所有悬空镜像

# 构建镜像 (需要 Dockerfile)
docker build -t my-app:1.0 .

# ==================== 2. 容器管理 ====================

# 查看容器
docker ps                      # 正在运行的容器
docker ps -a                   # 所有容器 (包括停止的)
docker ps -q                   # 只显示容器 ID

# 查看容器详情
docker inspect container_id    # 查看详细配置
docker stats container_id      # 查看资源使用情况

# 创建并运行容器
docker run -d --name my-nginx -p 8080:80 nginx
# 参数说明:
#   -d: 后台运行 (detached)
#   --name: 指定容器名称
#   -p: 端口映射 (宿主机端口:容器端口)

# 停止/启动容器
docker stop container_id
docker start container_id
docker restart container_id

# 删除容器
docker rm container_id          # 删除停止的容器
docker rm -f container_id       # 强制删除 (包括运行中的)

# 进入容器内部
docker exec -it container_id /bin/bash    # 进入有 bash 的容器
docker exec -it container_id /bin/sh     # 进入只有 sh 的容器

# 在容器中执行命令
docker exec container_id psql -U postgres -d dify -c "SELECT 1;"

# 查看容器日志
docker logs container_id                # 查看所有日志
docker logs -f container_id             # 实时跟踪日志
docker logs --tail 100 container_id     # 只看最后 100 行
docker logs --since "10m" container_id   # 查看最近 10 分钟的日志

# ==================== 3. 数据卷管理 ====================

# 查看数据卷
docker volume ls

# 创建数据卷
docker volume create my-data

# 使用数据卷
docker run -v my-data:/data nginx

# 挂载宿主机目录
docker run -v /opt/dify/storage:/app/storage nginx

# 删除数据卷
docker volume rm my-data
docker volume prune               # 删除未使用的卷

# ==================== 4. 网络管理 ====================

# 查看网络
docker network ls

# 创建网络
docker network create my-network

# 连接容器到网络
docker network connect my-network container_id

# 断开容器网络
docker network disconnect my-network container_id

# 删除网络
docker network rm my-network
```

#### 动手练习

```bash
# === 练习 1: 查看当前系统中的 Docker 资源 ===

# 查看所有镜像
docker images

# 查看所有容器 (包括停止的)
docker ps -a

# 查看所有数据卷
docker volume ls

# 查看所有网络
docker network ls

# === 练习 2: 管理 Dify 容器 ===

# 查看 Dify 相关的容器
docker ps --filter "name=docker-"
# 或者
docker ps | grep docker-

# 查看某个容器的状态
docker inspect docker-api-1 | grep -A5 "State"

# 查看容器资源使用
docker stats --no-stream docker-api-1

# 查看最近的错误日志
docker logs --tail 50 docker-api-1 2>&1 | grep -i error

# 重启 API 容器
docker restart docker-api-1

# === 练习 3: 进入容器操作 ===

# 进入 API 容器
docker exec -it docker-api-1 /bin/bash

# 在容器内可以做什么?
# - 查看代码: ls /app/api/
# - 修改配置: vi /app/api/config.py
# - 运行 Python: python -c "print('hello')"
# - 执行 API 请求: curl http://localhost:5001/health

# 进入数据库容器
docker exec -it docker-db_postgres-1 psql -U postgres -d dify

# 在 psql 中可以做什么?
# - 查看表: \dt
# - 查看表结构: \d apps
# - 查询数据: SELECT * FROM apps;
# - 退出: \q
```

#### 常见问题

| 问题 | 解决方案 |
|------|----------|
| `docker: Cannot connect` | Docker 服务未启动，执行 `sudo systemctl start docker` |
| `port is already allocated` | 端口被占用，用 `docker ps` 查看并停止占用的容器 |
| `no space left on device` | 磁盘空间不足，清理镜像和无用容器 |
| `permission denied` | 需要 root 权限，加 `sudo` 或配置用户组 |

---

### Day 4: Docker Compose 编排

#### 学习目标
理解 Docker Compose 的工作方式，能够通过一个配置文件管理多个相关容器。

#### 核心概念图解

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Compose 工作方式                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  docker-compose.yaml (配置文件)                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  version: '3.8'                                         │   │
│  │  services:                                              │   │
│  │    nginx:                                               │   │
│  │      image: nginx:1.27-alpine                          │   │
│  │      ports:                                            │   │
│  │        - "8081:80"                                     │   │
│  │      depends_on:                                       │   │
│  │        - api                                           │   │
│  │                                                        │   │
│  │    api:                                                │   │
│  │      image: langgenius/dify-api:1.16.1                 │   │
│  │      environment:                                      │   │
│  │        - SECRET_KEY=xxx                                │   │
│  │      volumes:                                          │   │
│  │        - ./storage:/app/storage                        │   │
│  │      depends_on:                                       │   │
│  │        - db                                            │   │
│  │                                                        │   │
│  │    db:                                                 │   │
│  │      image: postgres:15-alpine                         │   │
│  │      environment:                                      │   │
│  │        - POSTGRES_PASSWORD=xxx                          │   │
│  │      volumes:                                          │   │
│  │        - ./db_data:/var/lib/postgresql/data             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│                    │                                            │
│                    │ docker compose up -d                       │
│                    ▼                                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Docker Compose 会:                                    │   │
│  │  1. 读取 .env 环境变量文件                              │   │
│  │  2. 创建一个网络 (dify)                                │   │
│  │  3. 按依赖顺序启动各个服务                              │   │
│  │  4. 自动配置网络连接                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 核心命令速查表

```bash
# ==================== 1. 启动和停止 ====================

# 启动所有服务 (后台运行)
docker compose up -d

# 启动指定服务
docker compose up -d nginx

# 启动并重新构建
docker compose up -d --build

# 停止所有服务 (保留数据)
docker compose down

# 停止并删除所有数据 (危险!)
docker compose down -v

# 重启服务
docker compose restart
docker compose restart api

# ==================== 2. 查看状态 ====================

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs
docker compose logs -f              # 实时跟踪
docker compose logs api             # 只看某个服务
docker compose logs --tail 100      # 只看最后 100 行

# 查看配置
docker compose config               # 解析并显示最终配置
docker compose config --services    # 列出所有服务名

# ==================== 3. 与容器交互 ====================

# 进入容器
docker compose exec api /bin/bash

# 在容器中执行命令
docker compose exec api python -c "print('hello')"
docker compose exec db psql -U postgres -d dify -c "SELECT 1;"

# 复制文件
docker compose cp api:/app/config.py ./local_config.py
docker compose cp ./local_config.py api:/app/config.py

# ==================== 4. 更新服务 ====================

# 拉取最新镜像
docker compose pull

# 重新创建服务 (不影响数据)
docker compose up -d

# 强制重建 (解决配置变更不生效的问题)
docker compose up -d --force-recreate

# 只重建某个服务
docker compose up -d --force-recreate api
```

#### 动手练习

```bash
# === 练习 1: 查看 Dify 服务状态 ===

cd /opt/dify/docker

# 查看所有服务状态
docker compose ps

# 查看服务日志
docker compose logs --tail 30

# 查看 API 服务状态
docker compose ps api
docker compose logs --tail 20 api

# === 练习 2: 重启特定服务 ===

# 场景: 修改了 Nginx 配置后，需要重启
cd /opt/dify/docker
docker compose restart nginx

# 验证 Nginx 是否已重启
docker compose ps nginx
docker compose logs --tail 5 nginx

# 场景: 修改了 .env 后，需要完全重启
docker compose down
docker compose up -d

# 验证服务是否正常
curl http://localhost/health
curl http://localhost:5001/health

# === 练习 3: 在容器中执行命令 ===

# 查看 Dify 版本
docker compose exec api python -c "import dify; print(dify.__version__)"

# 查看数据库表
docker compose exec db psql -U postgres -d dify -c "\dt"

# 查询应用列表
docker compose exec db psql -U postgres -d dify -c "SELECT id, name, mode FROM apps;"

# 检查 API 健康状态
docker compose exec api curl http://localhost:5001/health

# === 练习 4: 备份和恢复数据 ===

# 备份数据库
docker compose exec db pg_dump -U postgres dify > dify_backup_$(date +%Y%m%d).sql

# 备份插件数据库
docker compose exec db pg_dump -U postgres dify_plugin > dify_plugin_backup.sql

# 恢复数据库 (谨慎操作!)
cat dify_backup.sql | docker compose exec -T db psql -U postgres -d dify

# 查看备份文件大小
ls -lh dify_backup*.sql
```

---

## 第二阶段：网络与存储

### Day 6: Nginx 反向代理

#### 学习目标
理解反向代理的工作原理，能够配置 Nginx 实现公网访问。

#### 核心概念图解

```
┌─────────────────────────────────────────────────────────────────┐
│                    Nginx 反向代理工作原理                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户请求                  Nginx (80端口)                       │
│  ┌──────┐                ┌──────────────────┐                  │
│  │ 浏览器 │──────────────►│                  │                  │
│  └──────┘                │   根据 URL 路由   │                  │
│                          │   转发请求到      │                  │
│                          │   对应后端服务    │                  │
│                          │                  │                  │
│                          │                  │────► api:5001    │
│                          │  /console/api/   │     (API 服务)   │
│                          │  /api/           │                  │
│                          │  /v1/            │                  │
│                          │                  │                  │
│                          │                  │────► web:3000    │
│                          │  /console        │     (前端服务)   │
│                          │  /app            │                  │
│                          │  /               │                  │
│                          │                  │                  │
│                          │                  │────► plugin:5002   │
│                          │  /plugin/        │     (插件服务)   │
│                          │                  │                  │
│                          └──────────────────┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Nginx 配置文件详解

```nginx
# /opt/dify/docker/nginx/conf.d/default.conf

# 定义一个服务器块
server {
    # 监听 80 端口
    listen 80;
    
    # 服务器名称 (可以是域名或 _ 表示默认)
    server_name _;
    
    # 客户端上传文件大小限制
    client_max_body_size 15M;
    
    # ==================== 路由规则 ====================
    
    # 规则 1: 拦截 /console/api/ 开头的请求
    # 转发到 API 容器的 5001 端口
    location /console/api/ {
        proxy_pass http://api:5001/console/api/;
        
        # 传递原始请求信息
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 超时设置 (重要! LLM 请求可能很慢)
        proxy_read_timeout 300s;     # 读取超时 5 分钟
        proxy_send_timeout 300s;     # 发送超时 5 分钟
        
        # 禁用缓冲 (SSE 流式响应需要)
        proxy_buffering off;
    }
    
    # 规则 2: 拦截 /api/ 开头的请求
    # 注意: 这个会匹配 /api/ 但不会匹配 /console/api/
    # 因为 Nginx 采用最长匹配原则
    location /api/ {
        proxy_pass http://api:5001/api/;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        proxy_buffering off;
    }
    
    # 规则 3: 插件请求
    location /plugin/ {
        proxy_pass http://plugin_daemon:5002;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 300s;
    }
    
    # 规则 4: 前端控制台页面
    location /console {
        rewrite ^/console(.*)$ /$1 break;  # 去掉 /console 前缀
        proxy_pass http://web:3000;
        proxy_set_header Host $http_host;
    }
    
    # 规则 5: 默认路由 - 其他所有请求
    location / {
        proxy_pass http://web:3000;
        proxy_set_header Host $http_host;
    }
}
```

#### 动手练习

```bash
# === 练习 1: 检查当前 Nginx 配置 ===

# 查看 Nginx 配置
cat /opt/dify/docker/nginx/conf.d/default.conf

# 测试配置是否正确
cd /opt/dify/docker
docker compose exec nginx nginx -t

# === 练习 2: 添加一个新的路由规则 ===

# 场景: 需要添加一个 /health 路由用于健康检查

# 1. 编辑配置
vi /opt/dify/docker/nginx/conf.d/default.conf

# 2. 在 location /console/api/ 之前添加:
# location /health {
#     proxy_pass http://api:5001/health;
# }

# 3. 测试配置
docker compose exec nginx nginx -t

# 4. 重新加载配置 (不中断服务)
docker compose exec nginx nginx -s reload
# 或者
docker compose restart nginx

# 5. 测试新路由
curl http://localhost/health

# === 练习 3: 调试路由问题 ===

# 查看 Nginx 访问日志
docker compose logs -f nginx

# 查看 Nginx 错误日志
docker compose logs -f nginx 2>&1 | grep error

# 测试某个请求的路由
curl -v http://localhost/console/api/health
# -v 参数显示详细的请求/响应头

# 直接访问后端服务 (跳过 Nginx)
curl http://localhost:5001/health  # 直接访问 API
curl http://localhost:3000         # 直接访问 Web
```

---

### Day 8: PostgreSQL 数据库管理

#### 学习目标
掌握 PostgreSQL 的基本操作，能够查询数据、备份和恢复。

#### 连接数据库

```bash
# 进入数据库容器
docker exec -it docker-db_postgres-1 psql -U postgres -d dify

# 连接后会看到:
# dify=# 

# 查看当前数据库
SELECT current_database();

# 查看当前用户
SELECT current_user;

# 退出
\q
```

#### 常用 SQL 命令

```sql
-- ==================== 1. 查看表结构 ====================

-- 列出所有表
\dt

-- 查看表结构
\d apps

-- 查看表的创建语句
\d+ apps

-- ==================== 2. 查询数据 ====================

-- 查询所有应用
SELECT id, name, mode, enable_api FROM apps;

-- 查询特定应用
SELECT * FROM apps WHERE id = 'YOUR_DIFY_APP_ID';

-- 模糊查询 (包含"洛阳"的应用)
SELECT * FROM apps WHERE name LIKE '%洛阳%';

-- 排序查询
SELECT * FROM apps ORDER BY created_at DESC;

-- 限制结果数量
SELECT * FROM apps LIMIT 10;

-- ==================== 3. 统计查询 ====================

-- 统计应用数量
SELECT COUNT(*) FROM apps;

-- 按类型分组统计
SELECT mode, COUNT(*) FROM apps GROUP BY mode;

-- ==================== 4. 更新数据 ====================

-- 更新应用配置
UPDATE apps 
SET app_model_config_id = 'new-config-id'
WHERE id = 'app-id';

-- 更新模型配置
UPDATE app_model_configs 
SET model = '{"provider":"tongyi","name":"qwen3.7-plus"}'::json
WHERE id = 'config-id';

-- ==================== 5. 插入数据 ====================

-- 插入新的模型配置
INSERT INTO app_model_configs (id, app_id, provider, model, configs)
VALUES (
    uuid_generate_v4(),
    'app-id',
    'langgenius/tongyi/tongyi',
    '{"provider":"tongyi","name":"qwen3.7-plus"}'::json,
    '{"context_size":128000}'::json
);

-- ==================== 6. 删除数据 (谨慎!) ====================

-- 删除特定记录
DELETE FROM app_model_configs WHERE id = 'config-id';

-- 删除所有记录 (危险!)
TRUNCATE TABLE apps;
```

#### 备份和恢复

```bash
# ==================== 1. 数据库备份 ====================

# 备份整个数据库
docker exec docker-db_postgres-1 pg_dump -U postgres dify > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份指定表
docker exec docker-db_postgres-1 pg_dump -U postgres -t apps dify > apps_backup.sql

# 只备份数据 (不包括表结构)
docker exec docker-db_postgres-1 pg_dump -U postgres --data-only dify > data_backup.sql

# 压缩备份 (节省空间)
docker exec docker-db_postgres-1 pg_dump -U postgres dify | gzip > backup.sql.gz

# ==================== 2. 数据恢复 ====================

# 从备份恢复 (谨慎! 会覆盖现有数据)
cat backup.sql | docker exec -i docker-db_postgres-1 psql -U postgres -d dify

# 从压缩备份恢复
gunzip -c backup.sql.gz | docker exec -i docker-db_postgres-1 psql -U postgres -d dify

# 恢复指定表
cat apps_backup.sql | docker exec -i docker-db_postgres-1 psql -U postgres -d dify

# ==================== 3. 自动备份脚本 ====================

# 创建备份脚本
cat > /opt/dify/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/dify/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# 备份主数据库
docker exec docker-db_postgres-1 pg_dump -U postgres dify > "$BACKUP_DIR/dify_$DATE.sql"

# 备份插件数据库
docker exec docker-db_postgres-1 pg_dump -U postgres dify_plugin > "$BACKUP_DIR/dify_plugin_$DATE.sql"

# 压缩备份
gzip "$BACKUP_DIR"/*.sql

# 删除 7 天前的备份
find "$BACKUP_DIR" -name "*.gz" -mtime +7 -delete

echo "备份完成: $DATE"
EOF

# 设置执行权限
chmod +x /opt/dify/backup.sh

# 手动执行备份
/opt/dify/backup.sh

# 设置定时任务 (每天凌晨 2 点备份)
crontab -e
# 添加: 0 2 * * * /opt/dify/backup.sh

# 查看备份
ls -lh /opt/dify/backups/
```

#### 动手练习

```bash
# === 练习 1: 查询应用信息 ===

# 连接数据库
docker exec -it docker-db_postgres-1 psql -U postgres -d dify

# 执行以下查询:
\d                                    # 查看所有表
\dt                                   # 查看数据表

SELECT id, name, mode, enable_api FROM apps;
SELECT COUNT(*) as total_apps FROM apps;

SELECT app_id, provider, model_id FROM app_model_configs;

# 退出
\q

# === 练习 2: 查看工作流配置 ===

# 查询工作流
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, app_id, status, version, created_at 
FROM workflows 
ORDER BY created_at DESC;"

# 查看工作流图 (前 500 字符)
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, LEFT(graph, 500) as graph_preview 
FROM workflows 
WHERE app_id = 'YOUR_DIFY_APP_ID';"

# === 练习 3: 检查模型凭据 ===

# 查看所有提供商凭据
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, provider_name, is_valid 
FROM provider_credentials;"

# 查看模型凭据
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, provider_credential_id, model_name, model_type 
FROM provider_model_credentials;"

# === 练习 4: 备份并恢复 ===

# 执行备份
/opt/dify/backup.sh

# 验证备份
ls -lh /opt/dify/backups/

# 模拟恢复 (先创建测试表)
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
CREATE TABLE test_backup (id SERIAL, name VARCHAR(100));
INSERT INTO test_backup (name) VALUES ('test data');
SELECT * FROM test_backup;"

# 从备份恢复 (如果需要)
# cat dify_*.sql.gz | gunzip | docker exec -i docker-db_postgres-1 psql -U postgres -d dify
```

---

## 第三阶段：Dify 进阶

### Day 11: Dify 架构与核心概念

#### 学习目标
理解 Dify 的核心概念，包括应用类型、工作流、模型集成等。

#### 核心概念图解

```
┌─────────────────────────────────────────────────────────────────┐
│                    Dify 核心概念                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 应用类型 (App Types)                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • Chatflow (对话流)     - 可视化工作流编排             │   │
│  │  • Agent (智能体)        - 自主决策的 AI 助手           │   │
│  │  • Chatbot (聊天机器人)  - 简单的对话应用               │   │
│  │  • Workflow (工作流)     - 自动化流程                   │   │
│  │  • API 应用              - 通过 API 调用                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  2. 工作流节点 (Workflow Nodes)                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 开始 (Start)          - 定义输入变量                 │   │
│  │  • LLM                   - 调用大模型                   │   │
│  │  • 条件判断 (IF/ELSE)    - 根据条件分支                 │   │
│  │  • HTTP 请求             - 调用外部 API                  │   │
│  │  • 代码执行 (Code)       - 运行自定义代码               │   │
│  │  • 知识库 (Knowledge)    - 检索相关文档                 │   │
│  │  • 工具 (Tool)           - 调用外部工具                 │   │
│  │  • 聚合 (Aggregate)      - 合并多个节点的输出           │   │
│  │  • 结束 (End)            - 输出最终结果                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  3. 模型配置 (Model Configuration)                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Provider (提供商): 阿里云百炼、OpenAI、其他             │   │
│  │  Model (模型): qwen3.7-plus、GPT-4 等                   │   │
│  │  Credentials (凭据): API Key、Secret 等                 │   │
│  │  Settings (设置): 温度、最大 Token 数等                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Dify 数据库表关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    核心表结构                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  apps (应用)                                                    │
│  ├── id: 应用 ID                                                │
│  ├── name: 应用名称                                             │
│  ├── mode: 应用类型 (chatflow, agent, ...)                      │
│  ├── app_model_config_id: 关联模型配置                          │
│  ├── workflow_id: 关联工作流 (可选)                            │
│  └── enable_api: 是否启用 API                                   │
│                                                                 │
│  app_model_configs (应用模型配置)                               │
│  ├── id: 配置 ID                                                │
│  ├── app_id: 关联应用                                           │
│  ├── provider: 模型提供商                                       │
│  ├── model_id: 关联的模型凭据 ID                                │
│  ├── model: 模型配置 (JSON)                                     │
│  └── configs: 其他配置 (JSON)                                   │
│                                                                 │
│  workflows (工作流)                                             │
│  ├── id: 工作流 ID                                              │
│  ├── app_id: 关联应用                                           │
│  ├── version: 版本号                                            │
│  ├── status: 状态 (draft, published)                           │
│  └── graph: 工作流图 (JSON)                                     │
│                                                                 │
│  provider_credentials (提供商凭据)                              │
│  ├── id: 凭据 ID                                                │
│  ├── provider_name: 提供商名称                                   │
│  ├── encrypted_config: 加密的配置                               │
│  └── is_valid: 是否有效                                         │
│                                                                 │
│  provider_model_credentials (模型凭据)                          │
│  ├── id: 凭据 ID                                                │
│  ├── provider_credential_id: 关联提供商凭据                     │
│  ├── model_name: 模型名称                                       │
│  └── encrypted_config: 加密的模型配置                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### Day 12: 模型配置与 API 集成

#### 学习目标
掌握如何在 Dify 中配置大模型，理解凭据存储的工作方式。

#### 配置流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    模型配置流程                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  步骤 1: 登录 Dify 控制台                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  访问 http://YOUR_PUBLIC_HOST:8082                       │   │
│  │  账号: difyadmin@dify.com                              │   │
│  │  密码: 123456                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  步骤 2: 进入模型设置                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  设置 → 模型供应商 → 通义 (DashScope)                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  步骤 3: 填写 API Key                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  输入 DashScope API Key                                  │   │
│  │  (系统会自动用 RSA 加密存储)                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  步骤 4: 选择模型                                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  选择要使用的模型:                                       │   │
│  │  - qwen3.7-plus (对话)                                   │   │
│  │  - text-embedding-v3 (向量)                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  步骤 5: 在应用中使用                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  编辑应用 → 选择已配置的模型                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 通过数据库直接配置 (进阶)

当 UI 配置失败时，可以直接操作数据库：

```bash
# 1. 查看现有的凭据
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, provider_name, is_valid FROM provider_credentials;"

# 2. 查看模型凭据
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
SELECT id, model_name, model_type FROM provider_model_credentials;"

# 3. 更新凭据 (如果加密配置有问题)
# 注意: 这需要了解 RSA 加密机制
# 最简单的方式是通过 Dify API 创建

# 4. 使用 Python 脚本更新
docker exec -it docker-api-1 python << 'EOF'
import requests
import json

# 登录获取 Token
session = requests.Session()
import base64
pwd = base64.b64encode(b'123456').decode()
resp = session.post('http://localhost:5001/console/api/login', 
                    json={'email': 'difyadmin@dify.com', 'password': pwd})
csrf = session.cookies.get('csrf_token', '')
headers = {'X-CSRF-Token': csrf}

# 创建新的模型凭据
data = {
    "provider": "langgenius/tongyi/tongyi",
    "model": "qwen3.7-plus",
    "model_type": "llm",
    "credentials": {
        "dashscope_api_key": "YOUR_DASHSCOPE_API_KEY"
    }
}
resp = session.post(
    'http://localhost:5001/console/api/workspaces/current/model-providers/langgenius/tongyi/tongyi/models/qwen3.7-plus/credentials',
    headers=headers,
    json=data
)
print(f"状态: {resp.status_code}")
print(f"响应: {resp.json()}")
EOF
```

---

## 🎯 综合练习

### 练习 1: 查看当前系统状态

```bash
# 1. 查看所有服务状态
cd /opt/dify/docker
docker compose ps

# 2. 查看系统资源使用
echo "=== 内存 ===" && free -h
echo "=== 磁盘 ===" && df -h /
echo "=== CPU ===" && uptime

# 3. 查看最近的错误日志
echo "=== API 错误日志 ===" 
docker compose logs --tail 50 api 2>&1 | grep -i error || echo "无错误"

echo "=== Nginx 错误日志 ==="
docker compose logs --tail 50 nginx 2>&1 | grep -i error || echo "无错误"
```

### 练习 2: 重启单个服务

```bash
# 场景: 修改了 .env 文件后需要重启 API

cd /opt/dify/docker

# 只重启 API 服务
docker compose restart api

# 等待服务启动
sleep 15

# 验证服务状态
docker compose ps api

# 测试 API 健康检查
curl http://localhost:5001/health

# 通过 Nginx 测试
curl http://localhost/console/api/health
```

### 练习 3: 备份并恢复

```bash
# 1. 执行备份
/opt/dify/backup.sh

# 2. 查看备份文件
ls -lh /opt/dify/backups/ | tail -5

# 3. 验证备份完整性
zcat /opt/dify/backups/dify_*.sql.gz | head -20
```

---

## 📖 推荐学习资源

### 官方文档
- Dify 官方文档: https://docs.dify.ai
- Docker 官方文档: https://docs.docker.com/docs
- Docker Compose 文档: https://docs.docker.com/compose
- Nginx 官方文档: https://nginx.org/en/docs
- PostgreSQL 手册: https://www.postgresql.org/docs/

### 视频教程
- Linux 基础入门 (Bilibili 搜索)
- Docker 入门到实战 (B站/极客时间)
- Nginx 配置实战 (B站)

### 书籍推荐
- 《Linux 就该这么学》
- 《Docker 技术入门与实战》
- 《深入浅出 PostgreSQL》

---

## 🛠️ 常用命令速查卡

```bash
# === 日常管理 ===
cd /opt/dify/docker && docker compose ps          # 查看状态
cd /opt/dify/docker && docker compose logs -f    # 查看日志
cd /opt/dify/docker && docker compose restart    # 重启所有
cd /opt/dify/docker && docker compose restart nginx  # 重启单个

# === 进入容器 ===
docker compose exec api /bin/bash               # API 容器
docker compose exec web /bin/bash               # Web 容器
docker compose exec db psql -U postgres -d dify  # 数据库

# === 数据管理 ===
docker exec docker-db_postgres-1 psql -U postgres -d dify -c "\dt"  # 列出表
docker exec docker-db_postgres-1 pg_dump -U postgres dify > backup.sql  # 备份
cat backup.sql | docker exec -i docker-db_postgres-1 psql -U postgres -d dify  # 恢复

# === 测试命令 ===
curl http://localhost/health                    # 健康检查
curl http://localhost:5001/health               # API 健康检查
curl -I http://localhost                        # 检查 HTTP 头
```

---

## ⚠️ 安全提醒

1. **不要在公开渠道分享** 服务器 IP、密码、API Key
2. **定期修改** 默认密码和密钥
3. **定期备份** 数据库（建议至少每周一次）
4. **限制 SSH 访问**，只允许必要的 IP 连接
5. **不要随意删除** Docker 卷和数据目录
6. **修改配置前** 先备份原文件

---

**祝学习愉快！🚀**

如果在学习过程中遇到任何问题，随时可以：
1. 运行 `docker compose logs --tail 100` 查看日志
2. 使用 `docker compose exec` 进入容器排查
3. 查阅官方文档或搜索相关错误信息
4. 向 AI 助手提问具体的错误场景
