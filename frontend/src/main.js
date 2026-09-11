import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
window.$httpbaseURL = '/production'
//window.$httpbaseURL = 'http://YOUR_API_HOST:888/production'
//window.$httpbaseURL = 'http://YOUR_API_HOST:8088/api/'
//window.$httpbaseURL = 'http://YOUR_DIFY_HOST:8088/api'
//window.$httpbaseURL = 'http://34784h32y7.imdo.co/api'
// 导入需要的Element Plus图标
import { Menu, Plus, Close, Avatar, Message as MessageIcon, Refresh, Edit, Share, Stopwatch, User, Lock ,Management, Right,CloseBold,Delete,Upload ,Download ,SetUp,Setting,CircleClose,ZoomIn,House,Search,Microphone,Mute ,VideoPlay,VideoPause,Warning,ArrowDown,ArrowUp} from '@element-plus/icons-vue';

// 配置基础URL（如果需要的话）
axios.defaults.baseURL = import.meta.env.VITE_APP_TITLE === '生产环境' ? window.$httpbaseURL : window.$httpbaseURL;
//axios.defaults.baseURL = import.meta.env.VITE_APP_TITLE === '生产环境' ? 'http://34784h32y7.imdo.co/api' : 'http://34784h32y7.imdo.co/api';
//axios.defaults.baseURL = import.meta.env.VITE_APP_TITLE === '生产环境' ? 'http://YOUR_API_HOST:8084/api' : 'http://YOUR_API_HOST:8084/api';
axios.defaults.timeout = 15000;

// 异步初始化函数
async function initApp() {
  try {
    //1请求全局参数
    const parameters =  await axios.get('/parameters')
    window.localStorage.setItem('title',  parameters.data.data.name);
    window.localStorage.setItem('describe',  parameters.data.data.describe);
    const response = await axios.get('/globalConfig');
    const globalConfig = response.data;
    const audioFlag = globalConfig.data.audioFlag + ''
    // 2. 存储配置到localStorage
    window.localStorage.setItem('globalConfig', JSON.stringify(globalConfig));
    window.localStorage.setItem('loginMode', globalConfig.data.loginMode || '');
    window.localStorage.setItem('audioFlag', audioFlag || '');
   
    // 3. 创建并配置应用
    const app = createApp(App);
    // 注册图标
    const icons = [Menu, Plus, Close, Avatar, MessageIcon,Management, Refresh, Edit, Share, Stopwatch, User,Right,Delete, CloseBold,Lock,Upload,Download,SetUp,Setting,CircleClose ,ZoomIn,House,Search,VideoPlay,VideoPause,Mute,Microphone,Warning,ArrowDown,ArrowUp];
    icons.forEach(icon => {
      app.component(icon.name, icon);
    });

    // 5. 挂载路由并挂载应用
    app.use(router);
    app.mount('#app');

  } catch (error) {
    // 处理配置请求失败的情况
    console.error('全局配置加载失败:', error);
    ElMessage.error('系统初始化失败，请刷新页面重试');
    
    // 即使配置加载失败，也挂载应用（可根据业务需求调整）
    const app = createApp(App);
    const icons = [Menu, Plus, Close, Avatar, MessageIcon, Management,Refresh, Edit, Share, Stopwatch, User,Right,Delete, CloseBold,Lock,Upload,Download,SetUp,Setting,CircleClose ,ZoomIn,House,Search,VideoPlay,VideoPause,Mute,Microphone,Warning,ArrowDown,ArrowUp];
    icons.forEach(icon => {
      app.component(icon.name, icon);
    });
    app.use(router);
    app.mount('#app');
  }
}

// 执行初始化
initApp();
    