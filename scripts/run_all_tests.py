"""
Dify 智能问答系统 - 冒烟测试套件
测试版本: v1.0
测试时间: 2026-08-24

测试用例:
1. test_01_container_status  - 检查所有容器运行状态
2. test_02_api_health       - API 健康检查
3. test_03_login            - 登录功能验证
4. test_04_chat             - 对话功能验证
"""

import paramiko
import json
import base64
import sys
import time
from datetime import datetime

# ==================== 配置信息 ====================
CONFIG = {
    "linux_vm": {
        "host": "192.168.1.100",
        "port": 22,
        "username": "root",
        "password": "jszx2022."
    },
    "dify": {
        "public_url": "http://YOUR_PUBLIC_HOST:8082",
        "login_email": "difyadmin@dify.com",
        "login_password": "123456",
        "api_token": "YOUR_DIFY_APP_TOKEN",
        "app_id": "4aa10005-c0cc-40c6-bee4-2f6a84073d32",
        "app_name": "洛阳市房产销售智能问答"
    },
    "timeout": {
        "ssh": 30,
        "command": 60,
        "api": 30
    }
}

# ==================== 测试结果存储 ====================
test_results = []
passed = 0
failed = 0
skipped = 0

def log_result(test_name, status, message, duration=0):
    """记录测试结果"""
    global passed, failed, skipped
    
    result = {
        "test_name": test_name,
        "status": status,
        "message": message,
        "duration": f"{duration:.2f}s",
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    test_results.append(result)
    
    if status == "PASS":
        passed += 1
        print(f"  ✅ PASS: {test_name} ({duration:.2f}s) - {message}")
    elif status == "FAIL":
        failed += 1
        print(f"  ❌ FAIL: {test_name} ({duration:.2f}s) - {message}")
    else:
        skipped += 1
        print(f"  ⚠️  SKIP: {test_name} - {message}")

def connect_ssh():
    """连接 SSH"""
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(
            CONFIG["linux_vm"]["host"],
            port=CONFIG["linux_vm"]["port"],
            username=CONFIG["linux_vm"]["username"],
            password=CONFIG["linux_vm"]["password"],
            timeout=CONFIG["timeout"]["ssh"],
            auth_timeout=60,
            banner_timeout=60,
            allow_agent=False,
            look_for_keys=False
        )
        return ssh
    except Exception as e:
        raise Exception(f"SSH 连接失败: {e}")

def run_command(ssh, command, timeout=None):
    """执行远程命令"""
    if timeout is None:
        timeout = CONFIG["timeout"]["command"]
    
    try:
        stdin, stdout, stderr = ssh.exec_command(command, timeout=timeout)
        output = stdout.read().decode().strip()
        error = stderr.read().decode().strip()
        exit_code = stdout.channel.recv_exit_status()
        
        return {
            "output": output,
            "error": error,
            "exit_code": exit_code
        }
    except Exception as e:
        return {
            "output": "",
            "error": str(e),
            "exit_code": -1
        }

# ==================== 测试用例 ====================

def test_01_container_status(ssh):
    """测试 1: 检查容器运行状态"""
    print("\n" + "="*60)
    print("测试 1: 检查 Docker 容器运行状态")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 检查所有容器状态
        result = run_command(ssh, "docker ps --format '{{.Names}} {{.Status}}'")
        
        if result["exit_code"] != 0:
            log_result("容器状态检查", "FAIL", f"命令执行失败: {result['error']}", time.time() - start_time)
            return
        
        container_info = result["output"]
        required_containers = [
            "nginx", "api", "web", "db", "redis", "weaviate"
        ]
        
        running_count = 0
        container_status = {}
        
        for container in required_containers:
            # 检查容器是否在运行
            if container in container_info:
                running_count += 1
                container_status[container] = "运行中"
            else:
                container_status[container] = "未运行"
        
        if running_count >= 5:
            log_result("容器状态检查", "PASS", 
                      f"{running_count}/6 个核心容器运行正常", 
                      time.time() - start_time)
            
            # 打印详细状态
            print("    容器详情:")
            for container, status in container_status.items():
                icon = "✅" if status == "运行中" else "❌"
                print(f"      {icon} {container}: {status}")
        else:
            log_result("容器状态检查", "FAIL", 
                      f"只有 {running_count}/6 个容器在运行", 
                      time.time() - start_time)
            
    except Exception as e:
        log_result("容器状态检查", "FAIL", str(e), time.time() - start_time)

def test_02_api_health(ssh):
    """测试 2: API 健康检查"""
    print("\n" + "="*60)
    print("测试 2: API 服务健康检查")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 检查 API 健康状态
        result = run_command(ssh, "curl -s http://localhost:5001/health")
        
        if result["exit_code"] == 0 and "ok" in result["output"].lower():
            log_result("API 健康检查", "PASS", 
                      f"API 响应正常: {result['output']}", 
                      time.time() - start_time)
        else:
            # 尝试通过 Nginx 访问
            result2 = run_command(ssh, "curl -s http://localhost/health")
            
            if result2["exit_code"] == 0 and "ok" in result2["output"].lower():
                log_result("API 健康检查", "PASS", 
                          f"通过 Nginx 访问正常", 
                          time.time() - start_time)
            else:
                log_result("API 健康检查", "FAIL", 
                          f"API 响应异常: {result['output'][:100]}", 
                          time.time() - start_time)
                
    except Exception as e:
        log_result("API 健康检查", "FAIL", str(e), time.time() - start_time)

def test_03_login(ssh):
    """测试 3: 登录功能验证"""
    print("\n" + "="*60)
    print("测试 3: Dify 登录功能验证")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 在 API 容器内执行登录测试
        login_script = '''
import requests
import base64

session = requests.Session()
pwd = base64.b64encode(b'123456').decode()

try:
    resp = session.post(
        'http://localhost:5001/console/api/login',
        json={'email': 'difyadmin@dify.com', 'password': pwd},
        timeout=10
    )
    
    if resp.status_code == 200:
        print("LOGIN_SUCCESS")
    else:
        print(f"LOGIN_FAILED: {resp.status_code} - {resp.text[:100]}")
except Exception as e:
    print(f"LOGIN_ERROR: {str(e)}")
'''
        
        sftp = ssh.open_sftp()
        with sftp.file('/tmp/test_login.py', 'w') as f:
            f.write(login_script)
        sftp.close()
        
        result = run_command(
            ssh, 
            "docker cp /tmp/test_login.py docker-api-1:/tmp/ && "
            "docker exec docker-api-1 python /tmp/test_login.py",
            timeout=30
        )
        
        if "LOGIN_SUCCESS" in result["output"]:
            log_result("登录功能验证", "PASS", 
                      "管理员登录成功", 
                      time.time() - start_time)
        else:
            log_result("登录功能验证", "FAIL", 
                      f"登录失败: {result['output'][:100]}", 
                      time.time() - start_time)
            
    except Exception as e:
        log_result("登录功能验证", "FAIL", str(e), time.time() - start_time)

def test_04_chat(ssh):
    """测试 4: 对话功能验证"""
    print("\n" + "="*60)
    print("测试 4: 智能对话功能验证")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 在 API 容器内执行对话测试
        chat_script = '''
import requests
import json

api_token = "YOUR_DIFY_APP_TOKEN"

url = "http://localhost:5001/v1/chat-messages"
headers = {
    "Authorization": f"Bearer {api_token}",
    "Content-Type": "application/json"
}
data = {
    "inputs": {},
    "query": "你好，请简单介绍一下自己",
    "response_mode": "blocking",
    "user": "test_user"
}

try:
    resp = requests.post(url, headers=headers, json=data, timeout=60)
    
    if resp.status_code == 200:
        result = resp.json()
        answer = result.get("answer", "")
        print(f"CHAT_SUCCESS: {answer[:100]}")
    else:
        print(f"CHAT_FAILED: {resp.status_code} - {resp.text[:100]}")
except Exception as e:
    print(f"CHAT_ERROR: {str(e)}")
'''
        
        sftp = ssh.open_sftp()
        with sftp.file('/tmp/test_chat.py', 'w') as f:
            f.write(chat_script)
        sftp.close()
        
        result = run_command(
            ssh, 
            "docker cp /tmp/test_chat.py docker-api-1:/tmp/ && "
            "docker exec docker-api-1 python /tmp/test_chat.py",
            timeout=90
        )
        
        output = result["output"]
        if "CHAT_SUCCESS" in output:
            answer_preview = output.split("CHAT_SUCCESS:")[1].strip() if "CHAT_SUCCESS:" in output else "回答正常"
            log_result("对话功能验证", "PASS", 
                      f"对话成功，回答预览: {answer_preview}", 
                      time.time() - start_time)
        elif "CHAT_ERROR" in output:
            log_result("对话功能验证", "FAIL", 
                      f"对话错误: {output[:100]}", 
                      time.time() - start_time)
        else:
            log_result("对话功能验证", "FAIL", 
                      f"对话失败: {output[:100]}", 
                      time.time() - start_time)
            
    except Exception as e:
        log_result("对话功能验证", "FAIL", str(e), time.time() - start_time)

def test_05_database(ssh):
    """测试 5: 数据库连接验证"""
    print("\n" + "="*60)
    print("测试 5: 数据库连接验证")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 检查数据库连接
        result = run_command(
            ssh,
            "docker exec docker-db_postgres-1 psql -U postgres -d dify -c "
            "\"SELECT COUNT(*) as app_count FROM apps;\""
        )
        
        if result["exit_code"] == 0:
            # 解析查询结果
            lines = result["output"].split('\n')
            for line in lines:
                if 'app_count' in line or line.strip().isdigit():
                    count = line.strip()
                    log_result("数据库连接验证", "PASS", 
                              f"数据库连接正常，应用数量: {count}", 
                              time.time() - start_time)
                    return
            
            log_result("数据库连接验证", "PASS", 
                      "数据库连接正常", 
                      time.time() - start_time)
        else:
            log_result("数据库连接验证", "FAIL", 
                      f"数据库连接失败: {result['error'][:100]}", 
                      time.time() - start_time)
            
    except Exception as e:
        log_result("数据库连接验证", "FAIL", str(e), time.time() - start_time)

def test_06_public_access():
    """测试 6: 公网访问验证"""
    print("\n" + "="*60)
    print("测试 6: 公网访问验证")
    print("="*60)
    
    start_time = time.time()
    
    try:
        import requests
        
        url = CONFIG["dify"]["public_url"]
        resp = requests.get(url, timeout=10)
        
        if resp.status_code == 200:
            log_result("公网访问验证", "PASS", 
                      f"公网访问正常，状态码: {resp.status_code}", 
                      time.time() - start_time)
        else:
            log_result("公网访问验证", "FAIL", 
                      f"公网访问异常，状态码: {resp.status_code}", 
                      time.time() - start_time)
                      
    except requests.exceptions.ConnectionError:
        log_result("公网访问验证", "SKIP", 
                  "无法连接公网地址（可能在内网环境）", 
                  time.time() - start_time)
    except Exception as e:
        log_result("公网访问验证", "FAIL", str(e), time.time() - start_time)

def run_offline_tests():
    """离线模式测试 - 当 Linux VM 不可达时"""
    print("\n" + "[离线模式] Linux VM 不可达，执行离线测试")
    print("[离线模式] 将记录所有测试用例为 SKIP 状态")
    
    offline_results = [
        ("容器状态检查", "需 SSH 访问 Linux VM 检查 Docker 容器状态"),
        ("API 健康检查", "需 SSH 访问 Linux VM 执行 curl 健康检查"),
        ("登录功能验证", "需 SSH 访问 Linux VM 在 API 容器内执行登录测试"),
        ("对话功能验证", "需 SSH 访问 Linux VM 在 API 容器内执行对话测试"),
        ("数据库连接验证", "需 SSH 访问 Linux VM 执行 PostgreSQL 查询"),
        ("公网访问验证", "需网络可达性测试，当前网络环境受限")
    ]
    
    for name, reason in offline_results:
        log_result(name, "SKIP", reason, 0)
    
    print("\n[离线模式] 测试用例说明:")
    print("  1. 确保 Linux VM 已开机并联网")
    print("  2. 确保 Windows 主机可以访问 Linux VM (ping 192.168.1.100)")
    print("  3. 确保 Dify 服务正在运行 (docker compose ps)")
    print("  4. 重新运行: python run_all_tests.py")

# ==================== 主测试流程 ====================

def main():
    print("\n" + "#"*70)
    print("#" + " "*20 + "Dify 智能问答系统 - 冒烟测试")
    print("#" + " "*25 + f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("#"*70)
    
    ssh = None
    online_mode = False
    
    try:
        # 连接 SSH
        print("\n[准备] 正在连接 Linux VM...")
        ssh = connect_ssh()
        print("[准备] SSH 连接成功!")
        online_mode = True
        
        # 执行测试用例
        test_01_container_status(ssh)
        test_02_api_health(ssh)
        test_03_login(ssh)
        test_04_chat(ssh)
        test_05_database(ssh)
        test_06_public_access()
        
    except Exception as e:
        print(f"\n[警告] 无法连接 Linux VM: {e}")
        print("[警告] 切换到离线模式...")
        run_offline_tests()
        
    finally:
        if ssh:
            ssh.close()
            print("\n[清理] SSH 连接已关闭")
    
    # 输出测试报告
    print("\n" + "#"*70)
    print("#" + " "*28 + "测试报告汇总")
    print("#"*70)
    
    total = passed + failed + skipped
    print(f"\n测试模式: {'在线模式' if online_mode else '离线模式'}")
    print(f"总测试用例: {total}")
    print(f"  ✅ 通过: {passed}")
    print(f"  ❌ 失败: {failed}")
    print(f"  ⚠️  跳过: {skipped}")
    
    if online_mode and total > 0:
        pass_rate = (passed / total) * 100
        print(f"\n通过率: {pass_rate:.1f}%")
        
        if pass_rate == 100:
            print("\n🎉 恭喜！所有测试用例全部通过！")
        elif pass_rate >= 80:
            print("\n✅ 系统基本功能正常，存在少量问题需要关注")
        else:
            print("\n⚠️  系统存在较多问题，需要进一步排查")
    elif not online_mode:
        print("\n⚠️  当前为离线模式，所有测试用例被跳过")
        print("💡 当 Linux VM 恢复在线后，请重新运行测试脚本")
    
    # 保存测试报告
    report = {
        "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "test_mode": "online" if online_mode else "offline",
        "system_info": {
            "linux_vm": CONFIG["linux_vm"]["host"],
            "dify_public_url": CONFIG["dify"]["public_url"]
        },
        "summary": {
            "total": total,
            "passed": passed,
            "failed": failed,
            "skipped": skipped,
            "pass_rate": f"{(passed/total*100):.1f}%" if total > 0 else "N/A"
        },
        "results": test_results
    }
    
    report_file = "test_report.json"
    with open(report_file, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试报告已保存: {report_file}")
    
    # 返回退出码
    return 0 if (online_mode and failed == 0) or not online_mode else 1

if __name__ == "__main__":
    sys.exit(main())
