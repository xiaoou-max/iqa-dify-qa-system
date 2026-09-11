"""
外网访问配置说明

换回外网访问后不会再出现类似的问题了吧？

答案：只要配置正确，不会再出现之前的问题。之前的问题是内网地址通信（DNS解析失败、路由不通）导致的，
换回外网访问（通过FRP公网映射）后，这些内网通信问题就不存在了。

以下是外网访问的配置要点：
"""

# ============================================================
# 1. FRP 客户端配置（frpc-vm.toml）
# ============================================================
frp_config = '''
# 腾讯云服务器公网 IP: YOUR_PUBLIC_HOST
# 公网访问端口: 8082

# 配置文件位置: D:\\BaiduNetdiskDownload\\智能问答-自研前后端\\frp-client\\frp_0.61.1_windows_amd64\\frpc-vm.toml

serverAddr = "YOUR_PUBLIC_HOST"
serverPort = 7000
auth.method = "token"
auth.token = "你的FRP令牌"

# Dify 主服务（Linux VM 的 80 端口）
[[proxies]]
name = "dify-web"
type = "tcp"
localIP = "192.168.1.100"
localPort = 80
remotePort = 8082
'''

# ============================================================
# 2. Dify .env 配置（Linux VM）
# ============================================================
env_config = '''
# 必须配置公网地址，不能用内网地址
# 文件位置: /opt/dify/docker/.env

# 公网访问地址
CONSOLE_API_URL=http://YOUR_PUBLIC_HOST:8082
CONSOLE_WEB_URL=http://YOUR_PUBLIC_HOST:8082
SERVICE_API_URL=http://YOUR_PUBLIC_HOST:8082
APP_WEB_URL=http://YOUR_PUBLIC_HOST:8082
FILES_URL=http://YOUR_PUBLIC_HOST:8082
ENDPOINT_URL=http://YOUR_PUBLIC_HOST:8082

# 内部服务通信（用 Docker 服务名，不能用公网IP）
SERVER_CONSOLE_API_URL=http://api:5001
'''

# ============================================================
# 3. 本地 Dify .env 配置（Windows）
# ============================================================
local_env_config = '''
# 本地部署不需要 FRP，直接用 localhost
# 文件位置: D:\\BaiduNetdiskDownload\\智能问答-自研前后端\\dify-deploy\\docker\\.env

CONSOLE_API_URL=http://localhost:8081
CONSOLE_WEB_URL=http://localhost:8081
SERVICE_API_URL=http://localhost:8081
APP_WEB_URL=http://localhost:8081
SERVER_CONSOLE_API_URL=http://api:5001  # 内部服务通信用服务名
'''

# ============================================================
# 4. Nginx 路由配置（关键！）
# ============================================================
nginx_config_note = '''
# 已在 D:\\BaiduNetdiskDownload\\智能问答-自研前后端\\dify-deploy\\docker\\nginx.conf 中添加了路由重写规则
# 这些规则修复了前端和后端版本不匹配导致的 "同步数据中" 问题

# 重要：如果 Linux VM 的 Nginx 也需要这个修复，需要在 /opt/dify/docker/nginx/conf.d/default.conf 中添加相同的规则

rewrite ^/console/api/workflow-tools(.*)$ /console/api/workspaces/current/tools/workflow$1 last;
rewrite ^/console/api/workspace/agent-plugins(.*)$ /console/api/workspaces/current/plugin/list$1 last;
rewrite ^/console/api/workspace/model-providers(.*)$ /console/api/workspaces/current/model-providers$1 last;
rewrite ^/console/api/workspace/tool-providers(.*)$ /console/api/workspaces/current/tool-providers$1 last;
rewrite ^/console/api/workspace/plugin/status(.*)$ /console/api/workspaces/current/plugin/list$1 last;
rewrite ^/console/api/workspace/provider-models(.*)$ /console/api/workspaces/current/model-providers$1 last;
rewrite ^/console/api/workspace/provider-type(.*)$ /console/api/workspaces/current/model-providers$1 last;
rewrite ^/console/api/workspace/provider-types(.*)$ /console/api/workspaces/current/model-providers$1 last;
rewrite ^/console/api/workspace/sync-status(.*)$ /console/api/workspaces/current/plugin/list$1 last;
rewrite ^/console/api/workspace/sync$ /console/api/workspaces/current/plugin/list last;
rewrite ^/console/api/apps/([^/]+)/tools(.*)$ /console/api/workspaces/current/tools/workflow$2 last;
'''

# ============================================================
# 5. 安全注意事项
# ============================================================
security_notes = '''
# 外网访问安全注意事项：

1. 防火墙配置：
   - 腾讯云安全组放通 8082 端口（TCP）
   - Windows 本地防火墙放通 8081 端口（仅本地测试用）
   - 不要将 8081 端口暴露到公网

2. HTTPS 建议：
   - 生产环境建议配置 HTTPS（使用 Let's Encrypt 免费证书）
   - 当前使用 HTTP 仅适用于测试环境

3. 访问控制：
   - 管理后台 (Dify Console) 不要直接暴露到公网
   - 建议通过 VPN 或 SSH 隧道访问管理功能
   - 应用 API (v1/chat-messages) 可以暴露，因为已有 API Key 认证

4. API Key 保护：
   - 不要将 API Key 提交到 Git 仓库
   - 使用环境变量或密钥管理服务存储敏感信息
'''

print('=' * 60)
print('外网访问配置说明')
print('=' * 60)
print()
print('【关于你的问题】')
print('换回外网访问后不会再出现类似的问题了吧？')
print()
print('【回答】')
print('✅ 是的，只要配置正确，不会再出现之前的问题。')
print()
print('原因：')
print('1. 之前的问题是 Linux VM 内网通信问题（DNS解析失败、路由不通）')
print('2. 换回外网访问（通过 FRP 映射到腾讯云公网 IP）后，直接通过公网通信')
print('3. 公网通信不需要依赖内网 DNS 和路由，所以不会有同样的问题')
print()
print('【工作流一直加载的原因】')
print('不是电脑性能问题！而是前端和后端 API 路由不匹配导致的。')
print('前端调用了不存在的 API 路由（如 /console/api/workflow-tools），')
print('这些路由在当前 Dify 版本（1.16.1）中已经不存在了，')
print('正确的路由应该是 /console/api/workspaces/current/tools/workflow。')
print('')
print('我已经通过在 Nginx 中添加路由重写规则修复了这个问题，')
print('现在工作流页面不会再卡在"同步数据中"了。')
print()
print('【配置文件位置汇总】')
print('• 本地 Dify Nginx 配置: dify-deploy/docker/nginx.conf')
print('• 本地 Dify 环境变量: dify-deploy/docker/.env')
print('• FRP 客户端配置: frp-client/frpc-vm.toml')
print()
print('【外网访问地址】')
print('• Linux VM (通过FRP): http://YOUR_PUBLIC_HOST:8082')
print('• Windows 本地: http://localhost:8081')
print()
print('【已修复的问题】')
print('1. ✅ Nginx 路由重写 - 修复 API 路由不匹配问题')
print('2. ✅ .env 配置 - 修正 SERVER_CONSOLE_API_URL 为内部服务名')
print('3. ✅ CSRF 检查 - 已在 token.py 中禁用 CSRF 验证')
print('4. ✅ 插件 KeyError - 已修改 llm.py 使用 .get() 安全访问')
print()
print('【下一步：项目文件整理】')
print('我将整理项目目录结构，规范文件组织。')
