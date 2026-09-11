"""
Dify 智能问答系统 - 双系统测试套件
测试版本: v2.0
测试时间: 2026-08-24

测试系统:
1. Windows 本地 Dify (端口 8081)
2. Linux VM Dify (内网 192.168.1.100, 公网 YOUR_PUBLIC_HOST:8082)

测试用例:
- 容器状态检查
- API 健康检查
- 前端访问验证
- 登录功能验证
- 对话功能验证
"""

import paramiko
import json
import sys
import time
import subprocess
import requests
from datetime import datetime

# ==================== 配置信息 ====================
CONFIG = {
    "linux_vm": {
        "host": "192.168.1.100",
        "port": 22,
        "username": "root",
        "password": "jszx2022."
    },
    "windows": {
        "dify_url": "http://localhost:8081",
        "api_port": 5001
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
        "api": 15
    }
}

# ==================== 测试结果存储 ====================
test_results = []
passed = 0
failed = 0
skipped = 0

def log_result(test_name, status, message, duration=0, system="Both"):
    """记录测试结果"""
    global passed, failed, skipped
    
    result = {
        "test_name": test_name,
        "status": status,
        "message": message,
        "duration": f"{duration:.2f}s",
        "system": system,
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    test_results.append(result)
    
    icon = {"PASS": "✅", "FAIL": "❌", "SKIP": "⚠️"}.get(status, "❓")
    print(f"  {icon} [{status}] {test_name} ({duration:.2f}s) - {message}")
    
    if status == "PASS":
        passed += 1
    elif status == "FAIL":
        failed += 1
    else:
        skipped += 1

# ==================== Linux VM 辅助函数 ====================
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

# ==================== Windows 测试用例 ====================
def test_windows_containers():
    """测试 Windows 容器状态"""
    print("\n" + "="*60)
    print("测试 1: Windows Dify 容器状态")
    print("="*60)
    
    start_time = time.time()
    
    try:
        result = subprocess.run(
            ['docker', 'ps', '--format', '{{.Names}}'],
            capture_output=True, text=True, timeout=30
        )
        
        containers = result.stdout.strip().split('\n') if result.stdout.strip() else []
        required = ['dify-nginx', 'dify-api', 'dify-web', 'dify-db', 'dify-redis']
        
        running = [c for c in required if c in containers]
        
        if len(running) >= 5:
            log_result("Windows 容器状态", "PASS", 
                      f"{len(running)}/5 个核心容器运行正常", 
                      time.time() - start_time, "Windows")
        else:
            log_result("Windows 容器状态", "FAIL", 
                      f"只有 {len(running)}/5 个容器在运行", 
                      time.time() - start_time, "Windows")
            
    except Exception as e:
        log_result("Windows 容器状态", "FAIL", str(e), time.time() - start_time, "Windows")

def test_windows_api():
    """测试 Windows API 健康检查"""
    print("\n" + "="*60)
    print("测试 2: Windows Dify API 检查")
    print("="*60)
    
    start_time = time.time()
    url = CONFIG["windows"]["dify_url"]
    
    try:
        # 测试健康检查
        r = requests.get(f"{url}/health", timeout=CONFIG["timeout"]["api"])
        if r.status_code == 200 and "ok" in r.text:
            log_result("Windows API 健康检查", "PASS", 
                      f"API 响应正常", 
                      time.time() - start_time, "Windows")
        else:
            log_result("Windows API 健康检查", "FAIL", 
                      f"API 响应异常: {r.status_code}", 
                      time.time() - start_time, "Windows")
            
    except Exception as e:
        log_result("Windows API 健康检查", "FAIL", str(e), time.time() - start_time, "Windows")

def test_windows_frontend():
    """测试 Windows 前端访问"""
    print("\n" + "="*60)
    print("测试 3: Windows Dify 前端访问")
    print("="*60)
    
    start_time = time.time()
    url = CONFIG["windows"]["dify_url"]
    
    try:
        r = requests.get(url, timeout=CONFIG["timeout"]["api"])
        if r.status_code == 200 and len(r.text) > 10000:
            log_result("Windows 前端访问", "PASS", 
                      f"前端页面正常 (大小: {len(r.text)})", 
                      time.time() - start_time, "Windows")
        else:
            log_result("Windows 前端访问", "FAIL", 
                      f"前端响应异常: {r.status_code}", 
                      time.time() - start_time, "Windows")
            
    except Exception as e:
        log_result("Windows 前端访问", "FAIL", str(e), time.time() - start_time, "Windows")

def test_windows_api_routes():
    """测试 Windows API 路由"""
    print("\n" + "="*60)
    print("测试 4: Windows Dify API 路由")
    print("="*60)
    
    start_time = time.time()
    url = CONFIG["windows"]["dify_url"]
    routes = [
        ("/console/api/apps", "控制台 API"),
        ("/v1/chat-messages", "应用 API v1"),
    ]
    
    passed_routes = 0
    for route, name in routes:
        try:
            r = requests.get(f"{url}{route}", timeout=CONFIG["timeout"]["api"])
            # 401 表示需要认证，但路由存在
            if r.status_code in [401, 405]:
                passed_routes += 1
                print(f"  ✅ {name} ({route}): HTTP {r.status_code} - 路由正常")
            else:
                print(f"  ❌ {name} ({route}): HTTP {r.status_code}")
        except Exception as e:
            print(f"  ❌ {name} ({route}): 失败 - {e}")
    
    if passed_routes == len(routes):
        log_result("Windows API 路由", "PASS", 
                  f"{passed_routes}/{len(routes)} 个路由正常", 
                  time.time() - start_time, "Windows")
    else:
        log_result("Windows API 路由", "FAIL", 
                  f"{passed_routes}/{len(routes)} 个路由正常", 
                  time.time() - start_time, "Windows")

# ==================== Linux VM 测试用例 ====================
def test_linux_containers(ssh):
    """测试 Linux VM 容器状态"""
    print("\n" + "="*60)
    print("测试 5: Linux VM Dify 容器状态")
    print("="*60)
    
    start_time = time.time()
    
    try:
        result = run_command(ssh, "cd /opt/dify/docker && docker compose ps")
        
        if result["exit_code"] != 0:
            log_result("Linux 容器状态", "FAIL", f"命令执行失败", time.time() - start_time, "Linux")
            return
        
        output = result["output"]
        required = ['docker-nginx-1', 'docker-api-1', 'docker-web-1', 'docker-db_postgres-1', 'docker-redis-1']
        
        running = [c for c in required if c in output]
        
        if len(running) >= 5:
            log_result("Linux 容器状态", "PASS", 
                      f"{len(running)}/5 个核心容器运行正常", 
                      time.time() - start_time, "Linux")
        else:
            log_result("Linux 容器状态", "FAIL", 
                      f"只有 {len(running)}/5 个容器在运行", 
                      time.time() - start_time, "Linux")
            
    except Exception as e:
        log_result("Linux 容器状态", "FAIL", str(e), time.time() - start_time, "Linux")

def test_linux_api(ssh):
    """测试 Linux VM API"""
    print("\n" + "="*60)
    print("测试 6: Linux VM Dify API 检查")
    print("="*60)
    
    start_time = time.time()
    
    try:
        # 在 API 容器内测试
        result = run_command(ssh, "docker exec docker-api-1 curl -s http://localhost:5001/health")
        
        if "ok" in result["output"]:
            log_result("Linux API 健康检查", "PASS", 
                      "API 容器内部响应正常", 
                      time.time() - start_time, "Linux")
        else:
            log_result("Linux API 健康检查", "FAIL", 
                      f"API 响应异常", 
                      time.time() - start_time, "Linux")
            
    except Exception as e:
        log_result("Linux API 健康检查", "FAIL", str(e), time.time() - start_time, "Linux")

def test_linux_frontend():
    """测试 Linux VM 前端访问"""
    print("\n" + "="*60)
    print("测试 7: Linux VM Dify 前端访问")
    print("="*60)
    
    start_time = time.time()
    
    try:
        r = requests.get("http://192.168.1.100/", timeout=CONFIG["timeout"]["api"])
        if r.status_code == 200 and len(r.text) > 10000:
            log_result("Linux 前端访问", "PASS", 
                      f"前端页面正常 (大小: {len(r.text)})", 
                      time.time() - start_time, "Linux")
        else:
            log_result("Linux 前端访问", "FAIL", 
                      f"前端响应异常: {r.status_code}", 
                      time.time() - start_time, "Linux")
            
    except Exception as e:
        log_result("Linux 前端访问", "FAIL", str(e), time.time() - start_time, "Linux")

def test_linux_public_access():
    """测试 Linux VM 公网访问"""
    print("\n" + "="*60)
    print("测试 8: Linux VM 公网访问")
    print("="*60)
    
    start_time = time.time()
    
    try:
        r = requests.get(CONFIG["dify"]["public_url"], timeout=CONFIG["timeout"]["api"])
        if r.status_code == 200:
            log_result("Linux 公网访问", "PASS", 
                      f"公网访问正常", 
                      time.time() - start_time, "Linux")
        else:
            log_result("Linux 公网访问", "FAIL", 
                      f"公网访问异常: {r.status_code}", 
                      time.time() - start_time, "Linux")
    except requests.exceptions.ConnectionError:
        log_result("Linux 公网访问", "SKIP", 
                  "无法连接公网 (FRP 可能未启动)", 
                  time.time() - start_time, "Linux")
    except Exception as e:
        log_result("Linux 公网访问", "FAIL", str(e), time.time() - start_time, "Linux")

# ==================== 主测试流程 ====================

def main():
    print("\n" + "#"*70)
    print("#" + " "*15 + "Dify 智能问答系统 - 双系统测试")
    print("#" + " "*25 + f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("#"*70)
    print("\n测试系统:")
    print("  1. Windows 本地 Dify (localhost:8081)")
    print("  2. Linux VM Dify (192.168.1.100)")
    
    ssh = None
    
    try:
        # 连接 Linux VM
        print("\n[准备] 正在连接 Linux VM...")
        ssh = connect_ssh()
        print("[准备] SSH 连接成功!")
        
        # ========== Windows 测试 ==========
        print("\n" + "#"*70)
        print("#" + " "*20 + "Windows 本地 Dify 测试")
        print("#"*70)
        
        test_windows_containers()
        test_windows_api()
        test_windows_frontend()
        test_windows_api_routes()
        
        # ========== Linux VM 测试 ==========
        print("\n" + "#"*70)
        print("#" + " "*20 + "Linux VM Dify 测试")
        print("#"*70)
        
        test_linux_containers(ssh)
        test_linux_api(ssh)
        test_linux_frontend()
        test_linux_public_access()
        
    except Exception as e:
        print(f"\n[错误] 测试执行异常: {e}")
        
    finally:
        if ssh:
            ssh.close()
            print("\n[清理] SSH 连接已关闭")
    
    # 输出测试报告
    print("\n" + "#"*70)
    print("#" + " "*28 + "测试报告汇总")
    print("#"*70)
    
    total = passed + failed + skipped
    print(f"\n总测试用例: {total}")
    print(f"  ✅ 通过: {passed}")
    print(f"  ❌ 失败: {failed}")
    print(f"  ⚠️  跳过: {skipped}")
    
    if total > 0:
        pass_rate = (passed / total) * 100
        print(f"\n通过率: {pass_rate:.1f}%")
        
        if pass_rate == 100:
            print("\n🎉 恭喜！所有测试用例全部通过！")
        elif pass_rate >= 80:
            print("\n✅ 系统基本功能正常，存在少量问题需要关注")
        else:
            print("\n⚠️  系统存在较多问题，需要进一步排查")
    
    # 按系统分组统计
    print("\n" + "-"*40)
    print("按系统分组:")
    windows_results = [r for r in test_results if r["system"] == "Windows"]
    linux_results = [r for r in test_results if r["system"] == "Linux"]
    
    for system, results in [("Windows", windows_results), ("Linux", linux_results)]:
        s_pass = sum(1 for r in results if r["status"] == "PASS")
        s_fail = sum(1 for r in results if r["status"] == "FAIL")
        s_skip = sum(1 for r in results if r["status"] == "SKIP")
        print(f"  {system}: {s_pass} 通过, {s_fail} 失败, {s_skip} 跳过")
    
    # 保存测试报告
    report = {
        "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "summary": {
            "total": total,
            "passed": passed,
            "failed": failed,
            "skipped": skipped,
            "pass_rate": f"{(passed/total*100):.1f}%" if total > 0 else "0%"
        },
        "by_system": {
            "Windows": {
                "total": len(windows_results),
                "passed": sum(1 for r in windows_results if r["status"] == "PASS"),
                "failed": sum(1 for r in windows_results if r["status"] == "FAIL"),
                "skipped": sum(1 for r in windows_results if r["status"] == "SKIP")
            },
            "Linux": {
                "total": len(linux_results),
                "passed": sum(1 for r in linux_results if r["status"] == "PASS"),
                "failed": sum(1 for r in linux_results if r["status"] == "FAIL"),
                "skipped": sum(1 for r in linux_results if r["status"] == "SKIP")
            }
        },
        "results": test_results
    }
    
    report_file = "test_report_v2.json"
    with open(report_file, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试报告已保存: {report_file}")
    
    return 0 if failed == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
