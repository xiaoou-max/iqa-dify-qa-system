# 智能问答系统 (iqa)

基于 [Dify](https://dify.ai) 的领域智能问答系统，面向房产销售业务场景。包含 Java 后端服务、Vue 3 前端、Dify 工作流编排定义、以及 Docker 部署配置。

## 项目结构

```
├── backend/           Java 后端服务 (Spring Boot 2.1 + JPA + Redis + Druid)
├── frontend/          前端应用 (Vue 3 + Vite)
├── dify-dsl/          Dify 工作流 DSL，可直接导入 Dify 平台
├── deploy/
│   ├── dify/          Dify 的 docker-compose 与 nginx 配置
│   ├── plugin-daemon/ 自定义插件守护进程
│   ├── plugin-code/   插件代码
│   ├── frp/           内网穿透客户端配置
│   └── nginx-backup/  nginx 配置备份
├── services/          Python 数据查询服务
├── scripts/           端到端测试脚本
├── sql/               数据库表结构与 ClickHouse 建表脚本
└── docs/              部署指南、开发文档、运维日志
```

## 技术栈

| 层 | 技术 |
|---|---|
| 大模型编排 | Dify（Agent / Chatflow 双模式） |
| 后端 | Spring Boot 2.1.3、Spring Data JPA、Redis、Druid |
| 前端 | Vue 3、Vite、ECharts |
| 数据 | MySQL、ClickHouse、Redis、Weaviate |
| 部署 | Docker Compose、Nginx、FRP 内网穿透 |
| 模型 | 阿里云百炼 DashScope (Qwen) |

## 运行前必读：配置占位符

为保证安全，本仓库中**所有密码、API Key、Token、服务器地址均已替换为占位符**。运行前需要自行填写：

| 占位符 | 出现位置 | 说明 |
|---|---|---|
| `CHANGE_ME` | `backend/src/main/resources/application-change.properties`、`deploy/frp/frpc-vm.toml` | 数据库密码、Redis 密码、FRP 认证 token |
| `YOUR_DIFY_APP_API_KEY` | `application-change.properties` | Dify 应用的 API Key |
| `YOUR_DASHSCOPE_API_KEY` | `deploy/*/plugin_daemon_v3.py` | 阿里云百炼 API Key |
| `YOUR_DIFY_HOST` / `YOUR_API_HOST` / `YOUR_PUBLIC_HOST` | 配置与文档 | 相应的服务地址 |

> 生产环境建议改用环境变量注入，不要将真实凭据写回配置文件。

## 启动方式

**后端**

```bash
cd backend
mvn clean package
java -jar target/iqa-1.0.jar
```

**前端**

```bash
cd frontend
npm install
npm run dev
```

**Dify 编排**

将 `dify-dsl/` 下的 YAML 文件导入 Dify 平台的「工作流 / DSL 导入」。

## 数据查询服务

```bash
cd services/query_database_ly
docker build -t iqa-query .
docker run -p 8000:8000 iqa-query
```

## 说明

本仓库为私有备份，仅供学习与运维参考。项目中的第三方依赖（Spring Boot、Vue、Dify 等）遵循各自的开源许可协议。
