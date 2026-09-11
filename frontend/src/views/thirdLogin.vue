<template>
  <div class="sso-loading-page">
    <!-- 背景渐变动画 -->
    <div class="background-gradient"></div>
    
    <!-- 加载容器 -->
    <div class="loading-container">
      <!-- 品牌标识 -->
      <div class="brand-logo">
        <svg width="64" height="64" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M16 24C16 17.3726 21.3726 12 28 12V36C21.3726 36 16 30.6274 16 24Z" fill="#409EFF"/>
          <path d="M28 12C34.6274 12 40 17.3726 40 24C40 30.6274 34.6274 36 28 36V12Z" fill="#66B1FF"/>
          <path d="M8 24C8 14.0589 14.0589 8 24 8V40C14.0589 40 8 34.9411 8 24Z" fill="#2E7BFF"/>
        </svg>
        <div class="logo-text">{{ title }}</div>
      </div>
      
      <!-- 核心加载动画区域 -->
      <div class="loading-content">
        <!-- 主加载动画（失败时停止动画） -->
        <div class="main-spinner">
          <div class="spinner-circle" :class="{ 'stop-animation': !isLoading }"></div>
          <div class="spinner-dot" :class="{ 'stop-animation': !isLoading }"></div>
        </div>
        
        <!-- 加载状态文字 -->
        <div class="loading-status">
          <p class="status-subtext">{{ progressText }}</p>
        </div>
      </div>
      
      <!-- 装饰性浮动元素 -->
      <div class="floating-elements">
        <div class="float-element el-1"></div>
        <div class="float-element el-2"></div>
        <div class="float-element el-3"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus' // 引入ElMessage

// 页面标题
const title = window.localStorage.getItem('title') || 'AI对话平台'
import axios from '@/apis/request'

// 加载状态控制（核心：控制动画播放/停止）
const isLoading = ref(true)
const progressText = ref('初始化验证参数')

import { useRoute } from 'vue-router' 
const route = useRoute() // 当前路由信息对象（包含参数）

import { useRouter } from 'vue-router'
const router = useRouter()

// 模拟加载进度状态
const loadingStates = [
  '初始化验证参数',
  '连接企业SSO服务器',
  '验证用户身份信息',
  '获取授权令牌',
  '完成身份验证'
]

// 模拟加载过程
onMounted(() => {
  // 修复变量声明重复问题
  let queryToken = route.query.ssoToken
  
  try {
      if(!queryToken || queryToken === ''){
          queryToken = window.localStorage.getItem('ssstoken')
      }else{
          window.localStorage.setItem('ssstoken', queryToken)
      }
      
  } catch (error) {
      isLoading.value = false // 停止动画
      progressText.value = '登录失败,请检查sso登录是否正常!'
      ElMessage.error('登录失败,请检查sso登录是否正常!')
      return false
  }

  // 发起登录请求
  axios.post('/ssoLogin', { ssoToken: queryToken }).then(res=>{
      if(res.type === 'fail'){
        isLoading.value = false // 登录失败：停止动画
        ElMessage.error('登录失败,请检查sso登录是否正常!')
        progressText.value = '登录失败,请检查sso登录是否正常!'
      }else{
        window.localStorage.setItem('_token', res.data._token)
        window.localStorage.setItem('user', res.data.name)
        router.push('/home')
      }
  }).catch(e=>{
      isLoading.value = false // 请求异常：停止动画
      ElMessage.error('登录失败,请检查sso登录是否正常!')
      progressText.value = '登录失败,请检查sso登录是否正常!'
    })
})
</script>

<style scoped>
* {
  box-sizing: border-box; /* 全局盒模型统一，避免边框导致的偏移 */
}

.sso-loading-page {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 背景渐变动画 */
.background-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg, 
    rgba(240, 245, 255, 1), 
    rgba(220, 231, 255, 1), 
    rgba(240, 245, 255, 1)
  );
  background-size: 400% 400%;
  animation: gradientFlow 18s ease infinite;
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 加载容器 - 确保绝对居中 */
.loading-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中 */
  align-items: center;     /* 水平居中 */
  z-index: 10;
  padding: 20px;
}

/* 品牌标识 */
.brand-logo {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 60px;
}

.logo-text {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

/* 加载内容区域 - 确保内部元素居中 */
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center; /* 内部元素水平居中 */
  gap: 40px;
  width: 100%;
  max-width: 500px;
}

/* 核心加载动画 - 容器固定尺寸 */
.main-spinner {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto; /* 确保容器自身在父元素中居中 */
}

/* 环形加载动画 - 修复居中关键 */
.spinner-circle {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 6px solid rgba(64, 158, 255, 0.1);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1.5s linear infinite;
  box-sizing: border-box;
}

/* 点状加载动画（内部）- 精准居中 */
.spinner-dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 16px;
  height: 16px;
  background-color: #2e7bff;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: pulse 1.2s ease-in-out infinite;
}

/* 停止动画的关键样式 */
.stop-animation {
  animation: none !important; /* 覆盖原有动画，停止播放 */
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  50% { transform: translate(-50%, -50%) scale(1.3); opacity: 0.8; }
}

/* 加载状态文字 */
.loading-status {
  text-align: center;
  width: 100%;
}

.status-subtext {
  font-size: 16px;
  color: #606266;
  opacity: 0.8;
  transition: all 0.3s ease;
}

/* 装饰性浮动元素 */
.floating-elements {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.float-element {
  position: absolute;
  background-color: rgba(64, 158, 255, 0.05);
  border-radius: 50%;
  filter: blur(30px);
  animation: float 15s ease-in-out infinite;
}

.el-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  left: 10%;
  background-color: rgba(64, 158, 255, 0.08);
  animation-delay: 0s;
}

.el-2 {
  width: 250px;
  height: 250px;
  bottom: 15%;
  right: 15%;
  background-color: rgba(46, 123, 255, 0.06);
  animation-delay: -5s;
}

.el-3 {
  width: 200px;
  height: 200px;
  top: 60%;
  left: 20%;
  background-color: rgba(102, 177, 255, 0.05);
  animation-delay: -8s;
}

@keyframes float {
  0% { transform: translateY(0px) translateX(0px); }
  50% { transform: translateY(-30px) translateX(20px); }
  100% { transform: translateY(0px) translateX(0px); }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .brand-logo {
    margin-bottom: 40px;
  }
  
  .logo-text {
    font-size: 24px;
  }
  
  .main-spinner {
    width: 100px;
    height: 100px;
  }
  
  .spinner-circle {
    border-width: 5px;
  }
  
  .status-subtext {
    font-size: 14px;
  }
  
  .float-element {
    transform: scale(0.8);
  }
}
</style>