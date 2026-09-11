import { createRouter, createWebHashHistory  } from 'vue-router'


const router = createRouter({
  history: createWebHashHistory (),
  
  routes: [
    {
      path: '/',
      name: 'login',
      component:  () => import('../views/login.vue'),
    },
    {
      path: '/home',
      name: 'home',
      component:  () => import('../views/HomeView.vue'),
       meta: { 
        requiresAuth: true,
        keepAlive: false // 【新增】标记该路由需要缓存
      }
    },
    {
      path: '/share',
      name: 'share',
      component:  () => import('../views/share.vue'),
    
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login.vue'),
    },
    {
      path: '/thirdLogin',
      name: 'thirdLogin',
      component: () => import('../views/thirdLogin.vue'),
    },
    {
      path: '/queryData',
      name: 'queryData',
      component: () => import('../views/queryData.vue'),
    }
  ],
})
// 路由守卫保持不变
router.beforeEach(async (to, from, next) => {
  try {
 

    // 2. 根据登录模式处理导航
    const loginMode = window.localStorage.getItem('loginMode')
    const isTokenValid = window.localStorage.getItem('_token')
    if (loginMode === 'noLogin') {
      if( to.path === '/login' ){
        next('/home')
      }else{
         // 无需登录模式
      to.path === '/' ? next('/home') : next()
      }
     
      
    } else if (loginMode === 'apiLogin'||loginMode === 'oriLogin') {
         // API登录模式
      if( to.path === '/login'||to.path === '/share'){
        next()
      }else if (isTokenValid !=''&&isTokenValid !='undefined'&&isTokenValid !=null) {
        to.path === '/' ? next('/home') : next()
      } else {
        next('/login')
      }
    } else if(loginMode === 'ssoTokenLogin'){
      //sso登陆模式
      if( to.path === '/share'||to.path === '/thirdLogin'){
        next()
      }else if(to.path=='/login'){
        next('/thirdLogin')
      }
      else if (isTokenValid !=''&&isTokenValid !='undefined'&&isTokenValid !=null) {
        to.path === '/' ? next('/home') : next()
      } else {
        next('/thirdLogin')
      }
    }else {
      // 处理未知的登录模式（默认情况）
      next() // 或根据需求跳转至错误页
    }
  } catch (error) {
    // 处理异步请求失败的情况
    console.error('获取全局配置失败:', error)
    next('/login') // 或根据需求处理，如跳转至错误页
  }
})



export default router
