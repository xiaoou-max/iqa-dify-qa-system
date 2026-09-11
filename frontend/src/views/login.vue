<template>
  <div class="login-container" v-loading.fullscreen.lock="Httploading">
    <!-- 渐变动画背景 -->
    <div class="login-bg"></div>
    
    <!-- 主内容区 -->
    <div class="login-content">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-logo">
          <div class="logo-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M16 24C16 17.3726 21.3726 12 28 12V36C21.3726 36 16 30.6274 16 24Z" fill="#409EFF"/>
              <path d="M28 12C34.6274 12 40 17.3726 40 24C40 30.6274 34.6274 36 28 36V12Z" fill="#66B1FF"/>
              <path d="M8 24C8 14.0589 14.0589 8 24 8V40C14.0589 40 8 34.9411 8 24Z" fill="#2E7BFF"/>
            </svg>
          </div>
          <div class="logo-text">{{title}}</div>
        </div>
        
        <div class="brand-slogan">
          <h2>简单高效的<br>AI对话平台</h2>
          <p>{{describe}}</p>
        </div>
        
        <!-- 使用渐变图形替代图片 -->
        <div class="brand-illustration">
          <div class="illustration-shape shape-1"></div>
          <div class="illustration-shape shape-2"></div>
          <div class="illustration-shape shape-3"></div>
        </div>
      </div>
      
      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="form-header">
          <h3 class="form-title">欢迎回来</h3>
          <p class="form-subtitle">请登录您的账户继续</p>
        </div>
        
        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="loginRules" 
          class="login-form"
        >
          <el-form-item prop="account">
            <el-input
              v-model="loginForm.account"
              prefix-icon="User"
              placeholder="请输入账号"
              class="form-input"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              prefix-icon="Lock"
              placeholder="请输入密码"
              type="password"
              class="form-input"
            />
          </el-form-item>
          
          <!-- <el-form-item>
            <div class="remember-forgot">
              <el-link class="forgot-password" href="#">忘记密码?</el-link>
            </div>
          </el-form-item> -->
          
          <el-button
            type="primary"
            class="login-button"
            @click="handleLogin('login')"
            :loading="loading"
          >
            登录
          </el-button>
          
          <!-- <div class="register-option">
            <p>还没有账户? <el-link @click="handleRegister">立即注册</el-link></p>
          </div> -->
          
        
      
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted,reactive } from 'vue'


import { useRouter } from 'vue-router'
const router = useRouter()
import axios from '@/apis/request'
// 表单数据
const loginFormRef = ref(null)
const Httploading = ref(false)
const title = ref('')
const describe = ref('')
const loginForm = reactive({
  account: '',
  password: '',

})

import { useRoute } from 'vue-router' 
const route = useRoute() // 当前路由信息对象（包含参数）

onMounted(async () => {
  try {
    localStorage.removeItem('title')
    localStorage.removeItem('describe')
  } catch (error) {
    
  }
  try {
    //1请求全局参数
    const parameters =  await axios.get('/parameters')
    window.localStorage.setItem('title',  parameters.data.name);
    window.localStorage.setItem('describe',  parameters.data.describe);
    title.value = window.localStorage.getItem('title')
    describe.value= window.localStorage.getItem('describe')
    let account = route.query.account
    let password = route.query.password
    if(account!='' && account != undefined&&account != null&&password!='' && password != undefined&&password != null){
      loginForm.account = account
      loginForm.password = password
      handleLogin('third')
    }
  } catch (error) {
    console.log(error)
  }
})



// 表单验证规则
const loginRules = reactive({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, message: '账号长度至少为3个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6个字符', trigger: 'blur' }
  ]
})

// 登录状态
const loading = ref(false)

// 登录处理
const handleLogin = (type) => {
  localStorage.removeItem('_token')
  localStorage.removeItem('user')
  localStorage.removeItem('thirdLogin')
  loginFormRef.value.validate((valid) => {
    if (valid||type=='third') {
      loading.value = true
      Httploading.value = true
      window.localStorage.setItem('thirdLogin','third');
      let loginUrl = '/apiLogin'
      if(window.localStorage.getItem('loginMode')=='oriLogin'){
        loginUrl = '/login'
      }
      axios.post(loginUrl,{...loginForm}).then(res=>{
        if(res.type == 'fail'){
          loading.value = false
          Httploading.value = false
          ElMessage.error('登录失败,请检查您的账号密码是否正确!')
        }else{
          window.localStorage.setItem('thirdLogin','api');
          loading.value = false
          localStorage.removeItem('_token')
          localStorage.removeItem('user')
          localStorage.removeItem('menu')

          window.localStorage.setItem('_token',res.data._token)
          window.localStorage.setItem('user',res.data.name)
          if(res.data.menus&&res.data.menus.length&&res.data.menus&&res.data.menus.length>0){
            window.localStorage.setItem('menu',JSON.stringify(res.data.menus))
          }
          router.push('/home')
        }
      }).catch(e=>{
        loading.value = false
        Httploading.value = false
      }).finally(e=>{
        loading.value = false
        Httploading.value = false
      })
   
    } else {
      ElMessage.error('请检查您的输入')
      return false
    }
  })
}





</script>

<style scoped>
/* 基础样式 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 渐变动画背景 */
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    -45deg, 
    rgba(245, 247, 250, 0.8), 
    rgba(221, 235, 247, 0.8), 
    rgba(245, 247, 250, 0.8), 
    rgba(221, 235, 247, 0.8)
  );
  background-size: 400% 400%;
  animation: gradient 15s ease infinite;
  z-index: 0;
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 主内容区 */
.login-content {
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 24px;
  box-shadow: 0 10px 50px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  z-index: 1;
  position: relative;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  padding: 60px;
  background: linear-gradient(135deg, #409eff 0%, #2e7bff 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 60px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background-color: #fff;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
}

.brand-slogan h2 {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 20px;
}

.brand-slogan p {
  font-size: 16px;
  opacity: 0.8;
  max-width: 320px;
}

/* 品牌插画（使用渐变形状替代图片） */
.brand-illustration {
  position: relative;
  height: 240px;
  margin-top: 40px;
}

.illustration-shape {
  position: absolute;
  border-radius: 24px;
  transform-origin: center;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 200px;
  height: 160px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.05) 100%);
  top: 20px;
  left: 0;
  animation-delay: 0s;
}

.shape-2 {
  width: 160px;
  height: 120px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.05) 100%);
  top: 80px;
  right: 20px;
  animation-delay: -2s;
}

.shape-3 {
  width: 120px;
  height: 80px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  top: 140px;
  left: 40px;
  animation-delay: -4s;
}

@keyframes float {
  0% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
  100% {
    transform: translateY(0px) rotate(0deg);
  }
}

/* 右侧登录表单 */
.login-form-wrapper {
  flex: 1;
  padding: 60px 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 20px;
}

.form-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.form-subtitle {
  font-size: 16px;
  color: #606266;
}

.form-input {
  border-radius: 12px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.form-input:hover {
  border-color: #c0c4cc;
}

.form-input:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.form-input .el-input__inner {
  height: 48px;
  border-radius: 12px;
}

.remember-forgot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0px;
  padding-left: 3px;
}

.forgot-password {
  color: #606266;
  transition: color 0.3s;
}

.forgot-password:hover {
  color: #409eff;
}

.login-button {
  height: 40px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.register-option {
  text-align: center;
  margin: 20px 0;
  color: #606266;
}

.register-option .el-link {
  color: #409eff;
  font-weight: 500;
  transition: all 0.3s;
}

.register-option .el-link:hover {
  text-decoration: underline;
}

.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 30px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background-color: #dcdfe6;
}

.divider span {
  padding: 0 15px;
  color: #909399;
  font-size: 14px;
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.social-button {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
  padding: 10px 16px;
  border-radius: 12px;
  transition: all 0.3s;
}

.social-button:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.social-button .el-icon {
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .login-content {
    flex-direction: column;
    max-width: 800px;
  }
  
  .login-brand,
  .login-form-wrapper {
    padding: 40px;
  }
  
  .brand-illustration {
    display: none;
  }
}

@media (max-width: 768px) {
  .login-container {
    padding: 20px;
  }
  
  .login-content {
    border-radius: 16px;
  }
  
  .login-brand,
  .login-form-wrapper {
    padding: 30px 20px;
  }
  
  .brand-slogan h2 {
    font-size: 24px;
  }
  
  .brand-slogan p {
    font-size: 14px;
  }
  
  .form-title {
    font-size: 24px;
  }
  
  .form-subtitle {
    font-size: 14px;
  }
  
  .social-login {
    flex-wrap: wrap;
  }
}
</style>