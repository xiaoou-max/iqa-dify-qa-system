import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

// 基础URL配置
let baseUrl = window.$httpbaseURL
if (import.meta.env.VITE_APP_TITLE === '生产环境') {
  baseUrl = window.$httpbaseURL
}

const service = axios.create({
  baseURL: baseUrl,
  timeout: 300000
})

// 请求拦截器：添加token
service.interceptors.request.use(
  config => {
    config.headers = {
      ...config.headers,
      '_token': window.localStorage.getItem('_token') || '',
      '_from': 'PC' // 这里原逻辑"1 ? 'PC' : 'Mobile'"永远为PC，保持原样
    }
    return config
  },
  error => {
    ElMessage.error('请求发送失败，请检查网络连接')
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
service.interceptors.response.use(
  async (response) => {
    // 处理需要重新登录的情况 判断登录方式
    if (response.data && response.data.type === 'needLogin') {
      if( window.localStorage.getItem('loginMode')=='noLogin'){
        try {
          // 尝试自动登录
          const tokenResponse = await service.post('/autoLogin')
          if (tokenResponse.data && tokenResponse.data._token) {
            window.localStorage.setItem('_token', tokenResponse.data._token)
            ElMessage.success('已自动重新登录')
            // 返回原响应数据，让请求继续处理
            return response.data
          } else {
            throw new Error('自动登录失败，缺少token')
          }
        } catch (autoLoginError) {
          console.error('自动登录失败:', autoLoginError)
          // 自动登录失败，需要手动登录
          handleLoginExpired()
          return Promise.reject(new Error('登录已过期，请重新登录'))
        }
      }else if( window.localStorage.getItem('loginMode')=='apiLogin'||window.localStorage.getItem('loginMode')=='oriLogin'){
        handleLoginExpired()
        return Promise.reject(new Error('登录状态已过期，请重新登录'))
      }
    
    }
    try {
      if(response.headers&&response.headers['content-disposition']!=null&&response.headers['content-disposition']!=undefined){
        return response
      }else{
        return response.data
      }
    } catch (error) {
      return response.data
    }
   
    
  },
  error => {
    // 网络错误处理
    if (!error.response) {
      ElMessage.error('网络连接异常，请检查网络设置')
      return Promise.reject(error)
    }

    // 401 未授权错误处理
    if (error.response.status === 401) {
      handleLoginExpired()
      return Promise.reject(new Error('登录状态已过期，请重新登录'))
    }

    // 403 权限错误处理
    if (error.response.status === 403) {
      ElMessage.error('您没有足够权限执行此操作')
      return Promise.reject(error)
    }

    // 404 错误处理
    if (error.response.status === 404) {
      ElMessage.error('请求的资源不存在')
      return Promise.reject(error)
    }

    // 500 服务器错误处理
    if (error.response.status >= 500) {
      
      try {
        if (error.response.data instanceof Blob) {
        
        }else{
          const errorMsg = error.response?.data?.data || '服务器异常';
          ElMessage.error(errorMsg);
        }
       
      } catch (error) {
        ElMessage.error('服务器内部错误，请稍后再试')
      }
      
      return Promise.reject(error)
    }

    // 其他错误处理
    const errorMessage = error.response.data?.message || '请求处理失败'
    ElMessage.error(errorMessage)
    return Promise.reject(error)
  }
)

/**
 * 处理登录过期的统一方法
 */
function handleLoginExpired() {
  // 避免重复弹出登录对话框
  if (window.isLoginExpiredDialogShow) {
    return
  }
  
  window.isLoginExpiredDialogShow = true
  
  ElMessageBox.confirm(
    '您的登录状态已过期，请重新登录',
    '登录过期提醒',
    {
      showCancelButton: false,
      closeOnClickModal: false,
      showClose: false,
      confirmButtonText: '前往登录',
      closeOnPressEscape: false,
      type: 'warning'
    }
  ).then(() => {
    // 清除登录状态
    localStorage.removeItem('_token')
    localStorage.removeItem('user')
    // 跳转到登录页，记录当前路径以便登录后返回
    router.replace({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }).finally(() => {
    window.isLoginExpiredDialogShow = false
  })
}

export default service
    