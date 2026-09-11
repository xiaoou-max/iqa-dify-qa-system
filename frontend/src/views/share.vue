<template>
    <div class="chat-container" v-loading.fullscreen.lock="Httploading">
      <!-- 主要聊天区域 -->
      <main  class="main-content">
        <!-- 问答消息区域（居中） -->
        <div class="chat-content-wrapper" ref="contentWrapper">
          <div class="chat-content" ref="chatContent">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-item"
              :class="{ 'user-msg': msg.isUser, 'ai-msg': !msg.isUser}"
            >
              <div v-if="msg.isUser">
                <div class="text-box" :class="{ 'streaming': msg.isStreaming }">
                  <MessageRenderer :items="[{ text: msg.content || '', uuid: msg.conversation_id || index }]" :is-streaming="msg.isStreaming" />
                </div>
              </div>
              <div  v-else>
                <div class="text-box" v-if="msg.isLoading"  v-loading="true"  >正在思考中...</div>
                <div v-else-if ="!msg.loading" class="text-box" :class="{ 'streaming': msg.isStreaming,'markdown-content':true }">
                <!-- 深度思考与普通文本循环按顺序渲染 -->
                <MessageRenderer :items="msg.messageArray" :is-streaming="msg.isStreaming" />
                <div v-if="false" v-for="(info, index) in msg.messageArray" :key="index">
                  
                  <!-- 如果是深度思考模块 -->
                  <div v-if="info.isThought" class="thought-section">
                    <div class="thought-header" @click="info.thoughtCollapsed = !info.thoughtCollapsed">
                      <div class="thought-badge">
                        <el-icon class="brain-icon"><Management /></el-icon>
                        <span>{{ !info.hasFinishedThinking ? '正在深度思考...' : '深度思考过程' }}</span>
                      </div>
                      <div class="thought-toggle">
                        <span>{{ info.thoughtCollapsed ? '展开' : '收起' }}</span>
                        <el-icon :class="{ 'is-rotated': !info.thoughtCollapsed }"><ArrowRight /></el-icon>
                      </div>
                    </div>
                    
                    <el-collapse-transition>
                      <div v-show="!info.thoughtCollapsed" class="thought-body-wrapper">
                        <div class="thought-content-inner" v-html="renderMarkdownT(info.text)"></div>
                      </div>
                    </el-collapse-transition>
                  </div>

                  <!-- 如果是普通文本模块 -->
                  <div v-else>
                    <div v-memo="[info.text]" v-html="renderMarkdownT( info.text)"></div>
                  </div>
                  
                </div>
              </div>
                
              </div>
              <div class="options" v-if="!msg.isUser&&audioFlag=='1'">
                <el-button 
                  @click="audioPlaying[msg.messageId] ? pauseAudio(msg.messageId) : playAudio(msg.messageId)" 
                  style="margin-left: 10px;"
                  text 
                  :icon="audioLoading[msg.messageId] ? 'Loading' : (audioPlaying[msg.messageId] ? 'VideoPause' : 'VideoPlay')"
                  :disabled="audioLoading[msg.messageId]"
                >
                  {{ audioLoading[msg.messageId] ? '加载中' : (audioPlaying[msg.messageId] ? '暂停' : '播放') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </main>
      <div  class="bottom-button" @click="continueclick">
        和AI对话
      </div>
    </div>
  </template>
  
  <script>
  import { ref, computed, onBeforeUnmount , onMounted, nextTick } from 'vue';
  import MessageRenderer from '@/components/chat/MessageRenderer.vue';
  import {marked} from 'marked';
  import hljs from 'highlight.js/lib/core';
  import sql from 'highlight.js/lib/languages/sql';
 import 'highlight.js/styles/atom-one-dark.css';
// 注册 SQL 语言
hljs.registerLanguage('sql', sql);
  import axios from '@/apis/request'
  import DOMPurify from 'dompurify';
  import * as echarts from 'echarts';
  import { buildChartOption } from '@/utils/chartOptions';
  import { useRoute ,useRouter} from 'vue-router'

  
  export default {
    components: { MessageRenderer },
    setup() {
      //配置
      let baseUrl = '/api'
      if(import.meta.env.VITE_APP_TITLE == '生产环境'){
        baseUrl = import.meta.env.VITE_API_URL
      }
      // 预设问题数组
      const presetQuestions = ref([]);
      const route = useRoute()
      // 对话数据
      const inputValue = ref('');
      const isLoading = ref(false);
      const title = ref('');
      const messages = ref([ ]);
      const chatContent = ref(null);
      const textarea = ref(null);
      const inputContainer = ref(null);
      const contentWrapper = ref(null);
      const conversation_id  = ref('')
      const selectMessage = ref([]);
      const showSelect = ref(true);
      const router = useRouter()

      const Httploading = ref(true)
  
      //流式数据
      const fetchStreamData = async (message) => {
        //解析数据方法
        const parseJsonStream = (text) => {
          try {
            // 处理SSE格式（data: {"answer": "..."}）
            const lines = text.split('\n').filter(line => line.trim());
            const lastLine = lines[lines.length - 1];
  
            if (lastLine.startsWith('data: ')) {
              const jsonStr = lastLine.substring('data: '.length).trim();
              if (jsonStr) {
                return JSON.parse(jsonStr);
              }
            }
            // 尝试直接解析JSON
            return JSON.parse(text);
          } catch (error) {
            return null; // 解析失败返回null
          }
        };
  
        try {
          const newMessageIndex = messages.value.length;
          messages.value.push({
            content: '',
            isUser: false,
            isLoading:true,
            avatarColor: '#409EFF',
            timestamp: new Date(),
            streamContent: '',
            isStreaming: true,
            conversation_id:conversation_id.value,
          });
          const response = await fetch(baseUrl+'/chat', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              '_token':window.localStorage.getItem('_token')
            },
            body: JSON.stringify({
              message: message,
              stream: true, // 请求流式响应
              conversation_id:conversation_id.value,
              query:message,
            })
          })
  
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
  
          if (response.body) {
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let showaccumulatedText = '';
  
            while (true) {
  
              const { done, value } = await reader.read();
              if (done) {
                messages.value[newMessageIndex].isStreaming = false;
                messages.value[newMessageIndex].content =  messages.value[newMessageIndex].streamContent
                break;
              }
            
              const chunk = decoder.decode( value, { stream: true });
              const lines = chunk.split('\n');
              lines.forEach(line=>{
                if(line){
                  let jsonStr = line.replace('data:', '');
                  let newData=parseJsonStream(jsonStr)
                  if(newData!=null && newData.event && (newData.event=='agent_message')){
                    if(newData.answer&&newData.answer!=null&&newData.answer!=undefined){
                      messages.value[newMessageIndex].isLoading = false
                      showaccumulatedText = showaccumulatedText + newData.answer
                      conversation_id.value= newData.conversation_id
                      messages.value[newMessageIndex].streamContent = showaccumulatedText;
                    }
                  }
                }
              })
              await nextTick();
            
            }
          }
        } catch (error) {
          console.error('接收流式数据时出错:', error);
          // 显示错误消息
          const errorMessageIndex = messages.value.length;
          messages.value.push({
            content: `<div class="error-message">发生错误: ${error.message}</div>`,
            isUser: false,
            avatarColor: '#f56c6c',
            timestamp: new Date(),
            streamContent: '',
            isStreaming: false,
     
          });
          await nextTick();
        
        }
        nextTick(() => {
          scrollToBottom();
        });
      };
        // 滚动到底部
      const scrollToBottom = () => {
        nextTick(() => {
          if (chatContent.value) {
            // contentWrapper.value.scrollTop = ;
            chatContent.value.scrollTo({
              top: chatContent.value.scrollHeight - 100, // 滚动到距离顶部200px的位置
              behavior: 'smooth' // 平滑滚动效果
            });
          }
        });
      };
      const renderedMessages = computed(() => {
        return messages.value.map(msg => renderMarkdown(msg));
      });
      // Markdown 渲染
      const renderMarkdown = (msg) => {
        console.log('正在渲染消息:', msg); // 调试日志
        // 自定义图表渲染规则
        const renderer = new marked.Renderer();
        renderer.code = (code) => {
          console.log(code)
          // console.log('当前代码块语言标识符:',code); // 调试日志
          // // 处理未定义语言的情况
          if (typeof code.lang === 'undefined') {
            return `
              <div class="code-block">
                <div class="code-header">未指定语言</div>
                <pre><code>${DOMPurify.sanitize(code.text)}</code></pre>
              </div>
            `;
          }
          if (code.lang === 'echarts'||code.lang === 'ech') {
            if(msg.isStreaming){
                return `<div  class="chart-container" style="height:500px;background:#fff">
                  <div class='chart-container-title' style="height:40px;background:#c8ceda40;font-size:16px;padding-left:20px;font-weight:600;line-height:40px">图表加载中...</div>
                  <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 440px; background-color: #f0f0f0; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="position: relative; width: 60px; height: 60px;">
                      <!-- 外圈 -->
                      <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 4px solid #d0d0d0; border-radius: 50%;"></div>
                      <!-- 动画内圈 -->
                      <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 4px solid transparent; border-top-color: #3b82f6; border-radius: 50%; animation: spin 1.2s linear infinite;"></div>
                    </div>
                  </div>
                </div>`
            }else{
              try {
                //清理可能的转义字符
                const cleanedCode = JSON.stringify(code.text).replace(/\\n/g, '\n')
                                        .replace(/\\"/g, '"')
                                        .replace(/^"|"$/g, '');
                // 生成唯一容器ID
                const chartId = `chart-${Math.random().toString(36).substring(2, 9)}`;
                nextTick(() => {
                  let jishqi = null
                  initstart(jishqi,chartId,  JSON.parse(cleanedCode));
                });

                return `<div  class="chart-container" style="height:550px;background:#fff">
                  <div class='chart-container-title' style="height:40px;background:#c8ceda40;font-size:16px;padding-left:20px;font-weight:600;line-height:40px">ECHARTS</div>
                  <div id="${chartId}" class="chart-container"  style="height:500px"></div>
                </div>`;
              } catch (error) {
                return `<div class="chart-error">图表配置解析错误：${error.message}</div>`;
              }
            }
            
          }
          // 调用 highlight 函数处理代码（复用 marked 的 highlight 逻辑）
          const highlightedCode = hljs.highlight(code.text, { 
            language: hljs.getLanguage(code.lang) ? code.lang : 'plaintext' 
          }).value;
          // 自定义样式，同时插入高亮后的代码
          return `
            <div class="code-block">
              <div class="code-header">${code.lang || 'text'}</div>
              <pre><code class="language-${code.lang}">${DOMPurify.sanitize(highlightedCode)}</code></pre>
            </div>
          `;
        };
        renderer.link = function(href, title) {
          let text = 'Download'
          const attrs = [];
          attrs.push(`href="${href.href}"`);
          if (title) {
            attrs.push(`title="${href.title}"`);
          }
          if(href.text){
            text = href.text
          }
          return `<a ${attrs.join(' ')}>${text}</a>`;
        }
        // ========== 新增：清理追问问题标签 ==========
        let cleanText = msg.content
       
        const html = marked.parse(msg.isStreaming?msg.streamContent:cleanText, {
          breaks: true,
          gfm: true,
          renderer
        });
        return DOMPurify.sanitize(html);
      };
      const renderMarkdownT = (text) => {
        console.log(text)
        // 自定义图表渲染规则
        const renderer = new marked.Renderer();
        renderer.code = (code) => {
          // console.log('当前代码块语言标识符:',code); // 调试日志
          // // 处理未定义语言的情况
          // 1. 判断是否是带有 ``` 或 ~~~ 的显式代码块 (fenced code block)
          const isFenced = code.raw && (code.raw.trimStart().startsWith('```') || code.raw.trimStart().startsWith('~~~'));

          // 2. 如果没有指定语言，且不是显式代码块（即由于 4 个空格缩进意外触发的 Markdown 缩进代码块）
          if (!code.lang && !isFenced) {
            // 优雅降级：将其直接当做普通文本渲染，保留空格和换行，无缝融入上下文中，避免出现丑陋的截断黑框
            return `<div class="plain-indented-text" style="white-space: pre-wrap; margin: 0; padding: 0; font-family: inherit; color: inherit; background: transparent; border: none;">${DOMPurify.sanitize(code.text)}</div>`;
          }
          if (typeof code.lang === 'undefined') {
            return `
              <div class="code-block">
                <div class="code-header">未指定语言</div>
                <pre><code>${DOMPurify.sanitize(code.text)}</code></pre>
              </div>
            `;
          }
          if (code.lang === 'echarts'||code.lang === 'ech') {
              try {
                //清理可能的转义字符
                const cleanedCode = JSON.stringify(code.text).replace(/\\n/g, '\n')
                                        .replace(/\\"/g, '"')
                                        .replace(/^"|"$/g, '');
                // 生成唯一容器ID
                const chartId = `chart-${Math.random().toString(36).substring(2, 9)}`;
                nextTick(() => {
                  let jishqi = null
                  initstart(jishqi,chartId,  JSON.parse(cleanedCode));
                });

                return `<div  class="chart-container" style="height:550px;background:#fff">
                  <div class='chart-container-title' style="height:40px;background:#c8ceda40;font-size:16px;padding-left:20px;font-weight:600;line-height:40px">ECHARTS</div>
                  <div id="${chartId}" class="chart-container"  style="height:500px"></div>
                </div>`;
              } catch (error) {
                return `<div class="chart-error">图表配置解析错误：${error.message}</div>`;
              }
            
            
          }
          // 调用 highlight 函数处理代码（复用 marked 的 highlight 逻辑）
          const highlightedCode = hljs.highlight(code.text, { 
            language: hljs.getLanguage(code.lang) ? code.lang : 'plaintext' 
          }).value;
          // 自定义样式，同时插入高亮后的代码
          return `
            <div class="code-block">
              <div class="code-header">${code.lang || 'text'}</div>
              <pre><code class="language-${code.lang}">${DOMPurify.sanitize(highlightedCode)}</code></pre>
            </div>
          `;
        };
        renderer.link = function(href, title) {
          let text = 'Download'
          const attrs = [];
          attrs.push(`href="${href.href}"`);
          if (title) {
            attrs.push(`title="${href.title}"`);
          }
          if(href.text){
            text = href.text
          }
          return `<a ${attrs.join(' ')}>${text}</a>`;
        }
        // ========== 新增：清理追问问题标签 ==========
  
        let cleanText = text
          .replace(/<suggested_start>[\s\S]*?<suggested_end>/g, '') // 移除标签及内容
          .trim();
        const html = marked.parse(cleanText, {
          breaks: true,
          gfm: true,
          renderer
        });
        return DOMPurify.sanitize(html);
      };
      // 时间格式化
      const formatTime = (time) => {
        const date = new Date(time);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      };
      const audioFlag = ref('')
      const init = async ()=>{
        try{
          const response = await axios.get('/globalConfig');
          const globalConfig = response.data;
          window.localStorage.setItem('audioFlag', globalConfig.data.audioFlag || '');
        }catch(e){}
       
        audioFlag.value =  window.localStorage.getItem('audioFlag')
        Httploading.value =true
        let id = route.query.share_code
        axios.get('/share?code='+id).then(res=>{
        messages.value = []
        Httploading.value = false
        res.data.messages.forEach(mes=>{
            let userMeg = {
             content: mes.query,
             isUser: true,
             avatarColor: '#67C23A',
             timestamp: new Date(),
             streamContent: '',
             isStreaming: false,
             conversation_id:mes.id
           }
           messages.value.push(userMeg)
          if(mes.answer&&mes.answer!=''&&mes.answer!=undefined){
            let allMessageRaw = mes.answer.replace(/Transition:(.*?)(?=\n|$)\n?/g, '') || '';
            let parsedSegments =[];
            let currentPos = 0;
            while (currentPos < allMessageRaw.length) {
              let thinkStart = allMessageRaw.indexOf('<think>', currentPos);
              if (thinkStart === -1) {
                let text = allMessageRaw.substring(currentPos);
                if (text) parsedSegments.push({ type: 'text', content: text });
                break;
              }
              if (thinkStart > currentPos) {
                parsedSegments.push({ type: 'text', content: allMessageRaw.substring(currentPos, thinkStart) });
              }
              let thinkEnd = allMessageRaw.indexOf('</think>', thinkStart + 7);
              if (thinkEnd === -1) {
                parsedSegments.push({ type: 'think', content: allMessageRaw.substring(thinkStart + 7), isClosed: false });
                break;
              } else {
                parsedSegments.push({ type: 'think', content: allMessageRaw.substring(thinkStart + 7, thinkEnd), isClosed: true });
                currentPos = thinkEnd + 8;
              }
            }
            let allStrArrayIndex =[];
            parsedSegments.forEach(seg => {
              if (seg.type === 'think') {
                allStrArrayIndex.push({ text: seg.content, isCode: 0, isThought: true });
              } else {
                let textStrArray = seg.content.split(/(?<=```)|(?=```)/);
                textStrArray = textStrArray.filter(item => item.trim() !== '');
                textStrArray.forEach(item => {
                  allStrArrayIndex.push({ text: item, isCode: 0, isThought: false });
                });
              }
            });
            allStrArrayIndex.forEach((item, index) => {
            if(item.text === '```' && item.isCode === 0 && !item.isThought){
              item.isCode = 1;
              if(allStrArrayIndex.length > index + 2){
                allStrArrayIndex[index+1].text = '```' + allStrArrayIndex[index+1].text + '```';
                allStrArrayIndex[index+1].isCode = 2;
                allStrArrayIndex[index+2].isCode = 3;
              } else if(allStrArrayIndex.length > index + 1){
                allStrArrayIndex[index+1].isCode = 2;
                allStrArrayIndex[index+1].text = '```' + allStrArrayIndex[index+1].text;
              }
            }
            });
            allStrArrayIndex = allStrArrayIndex.filter(item => item.text !== '```');

            let messageInfo = allStrArrayIndex.map(item => ({
              isCode: item.isCode === 2 || item.isCode === 1,
              isThought: item.isThought,
              text: item.text,
              thoughtCollapsed: true, // 历史记录默认折叠
              hasFinishedThinking: true
            }));
  
            let aiMeg = {
              messageArray:[...messageInfo],
              isUser: false,
              isLoading:false,
              avatarColor: '#409EFF',
              timestamp: new Date(),
              isStreaming: false,
              conversation_id:mes.conversation_id,
              messageId:mes.id,
              showAudio:true,
             
            }
            messages.value.push(aiMeg)
          }
        })
        console.log(messages.value)
       })
      }
      // 初始化
      onMounted(() => {
       init()
        
      });
  

      // 初始化图表
      const initstart = (jishuqi,containerId, option) => {
        jishuqi = setInterval(() => {
          const container = document.getElementById(containerId);
          if(container&&container.clientWidth != 0 && container.clientHeight != 0){
            initChart(containerId, option)
            clearInterval(jishuqi); 
          }
        }, 1000); // 每1秒就执行一下初始化图表
      };
      const initChart = (containerId, option)=> {
        const container = document.getElementById(containerId);
        if (!container) return;
  
        // 销毁旧实例
        if (container._echarts_instance) {
          echarts.getInstanceByDom(container)?.dispose();
        }
       
        const chart =  echarts.init(container);
        // 增强默认配置
        const baseOptions = {
          color: ['#5470c6', '#91cc75'],
          textStyle: {
            fontFamily: 'inherit',
            fontSize: 14
          },
          grid: {
            containLabel: true,
            left: '3%',
            right: '4%',
            bottom: '12%',
            top: '15%'
          },
          toolbox: {
            feature: {
              saveAsImage: {
                pixelRatio: 2,
                title: '保存图片'
              }
            }
          }
        };
  
        // 移动端适配
        if (window.innerWidth < 768) {
          option.legend = option.legend || {};
          option.legend.top = 'bottom';
          option.legend.itemGap = 5;
          if(option.grid){
            option.grid.bottom = '20%';
          }
        }
  
        // 合并配置
        const finalOption = buildChartOption(option, { mobile: window.innerWidth < 768 });
        // 设置图表
        chart.setOption(finalOption);
  
        // 设置自适应
        // const resizeHandler = () => chart.resize();
        // window.addEventListener('resize', resizeHandler);
  
        // 清理监听
        // $once('hook:beforeDestroy', () => {
        //   window.removeEventListener('resize', resizeHandler);
        //   chart.dispose();
        // });
  
        // 错误处理
        chart.on('error', (error) => {
          console.error('图表渲染错误:', error);
          container.innerHTML = `<div class="chart-error">图表渲染失败：${error.message}</div>`;
        });
      };
     
      // 模拟 Vue 2 的 $once 方法
      const $once =(event, callback) => {
        let called = false;
        const handler = () => {
          if (!called) {
            called = true;
            callback();
          }
        };
        
        // 监听生命周期钩子
        if (event === 'hook:beforeDestroy') {
          onBeforeUnmount(handler);
        }
        
        // 返回取消监听的函数
        return () => {
          called = true;
        };
      };
      const continueclick = ()=>{
       
        console.log(router)
        router.push('/home')
      }

      // 音频相关
      const audioInstances = ref({}); // 存储每个messageId的音频实例 {messageId: Audio}
      const audioLoading = ref({});   // 音频加载状态 {messageId: boolean}
      const audioPlaying = ref({});   // 音频播放状态 {messageId: boolean}
      const audioUrls = ref({});      // 缓存音频链接 {messageId: url}
      const audioError = ref({});     // 音频错误信息 {messageId: string}
     

      // 音频链接请求（适配二进制响应）
      const getAudioUrl = async (messageId) => {
        try {
          if (audioUrls.value[messageId]) return audioUrls.value[messageId].url; // 缓存命中
          audioLoading.value[messageId] = true;

          // 关键：设置responseType为blob，接收二进制数据
          const res = await axios.get(`/download_audio?message_id=${messageId}`, {
            responseType: 'blob' // 告诉axios接收二进制Blob
          });

          if (res.data instanceof Blob) {
            // 生成Blob临时URL（浏览器可识别的音频链接）
            const audioUrl = URL.createObjectURL(res.data);
            // 缓存URL和Blob（用于后续释放资源）
            audioUrls.value[messageId] = {
              url: audioUrl,
              blob: res.data
            };
            audioError.value[messageId] = '';
            return audioUrl;
          }
          throw new Error('后端返回的不是有效音频Blob');
        } catch (error) {
          audioError.value[messageId] = `音频加载失败：${error.message}`;
          ElMessage.error(audioError.value[messageId]);
          return null;
        } finally {
          audioLoading.value[messageId] = false;
        }
      };
      // 播放音频
      const playAudio = async (messageId) => {
        // 停止其他音频播放（互斥）
        Object.keys(audioPlaying.value).forEach(id => {
          if (id !== messageId && audioPlaying.value[id]) {
            pauseAudio(id);
          }
        });

        // 已创建音频实例直接播放
        if (audioInstances.value[messageId]) {
          try {
            await audioInstances.value[messageId].play();
            audioPlaying.value[messageId] = true;
          } catch (error) {
            // 自动播放失败：提示用户手动交互
            ElMessage.warning('浏览器限制自动播放，请点击播放按钮手动播放');
            audioPlaying.value[messageId] = false;
          }
          return;
        }

        // 获取音频链接并创建实例
        const audioUrl = await getAudioUrl(messageId);
        if (!audioUrl) return;

        const audio = new Audio(audioUrl);
        audioInstances.value[messageId] = audio;

        // 音频事件监听
        audio.addEventListener('canplay', async () => {
          try {
            await audio.play();
            audioPlaying.value[messageId] = true;
          } catch (error) {
            ElMessage.warning('请点击播放按钮手动播放音频');
            audioPlaying.value[messageId] = false;
          }
        });

        audio.addEventListener('ended', () => {
          audioPlaying.value[messageId] = false;
        });

        audio.addEventListener('error', (e) => {
          audioError.value[messageId] = '音频加载失败，请重试';
          audioPlaying.value[messageId] = false;
          ElMessage.error(audioError.value[messageId]);
        });
      };
      // 暂停音频
      const pauseAudio = (messageId) => {
        if (audioInstances.value[messageId]) {
          audioInstances.value[messageId].pause();
          audioPlaying.value[messageId] = false;
        }
      };

      // 新增：判断是否有音频实例存在
      const hasPlayingAudio = computed(() => {
        return Object.keys(audioInstances.value).length > 0;
      });

    
   
      return {
        inputValue,
        isLoading,
        messages,
        renderMarkdown,
        formatTime,
        renderMarkdownT,
        presetQuestions,
        title,
        chatContent,
        contentWrapper,
        selectMessage,
        showSelect,
        renderedMessages,
        continueclick,
        Httploading,
        playAudio,
        pauseAudio,
        hasPlayingAudio,
        audioLoading,
        audioPlaying,
        audioFlag
      };
    }
  };
  </script>
  
  <style lang="scss" scoped>
/* 基础样式 */
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Inter', 'Segoe UI', sans-serif; background-color: #f9fafb; }

/* 布局容器 */
.chat-container {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 移动端导航 */
.mobile-top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  z-index: 999;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.left-section { display: flex; align-items: center; }
.menu-btn {
  background: transparent;
  border: none;
  font-size: 20px;
  color: #409eff;
  margin-right: 12px;
  cursor: pointer;
  display: flex;
}
.new-chat {
  display: flex;
  align-items: center;
  margin-left: 12px;
  padding: 6px 10px;
  border-radius: 20px;
  background-color: #e6f4ff;
  color: #1677ff;
  cursor: pointer;
  transition: background-color 0.2s;
}
.new-chat:hover { background-color: #d6e9ff; }
.new-chat-icon { margin-right: 6px; }
.new-chat-text { font-size: 14px; font-weight: 500; }
.right-section { display: flex; align-items: center; }
.action-btn {
  background: transparent;
  border: none;
  font-size: 18px;
  color: #999;
  
  cursor: pointer;
  transition: color 0.2s;
}
.action-btn:hover { color: #409eff; }
.del-btn:hover { color: #F56C6C; }
.del-btnt { color: #F56C6C;  }
.action-btnt { color: #409eff; }
/* 历史对话抽屉 */
.history-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
 

}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #eee;
}
.drawer-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.drawer-history-list {
  list-style: none;
  flex: 1;
  overflow-y: auto;
  max-height: 800px;
}
.history-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background-color 0.2s;
}
.history-item:hover { background-color: #f5f7fa; }
.history-title {
  font-size: 14px;
  color: #333;
  position: relative;
 
  white-space: nowrap; /* 强制文字在一行内显示，不换行 */
  overflow: hidden; /* 隐藏超出宽度的文字 */
  text-overflow: ellipsis; /* 溢出部分显示省略号（...） */
  width: 100%;
}
.history-title-window{
  font-size: 14px;
  color: #333;
  position: relative;
  max-width: 170px;
  white-space: nowrap; /* 强制文字在一行内显示，不换行 */
  overflow: hidden; /* 隐藏超出宽度的文字 */
  text-overflow: ellipsis; /* 溢出部分显示省略号（...） */
}
.history-title-text{
  max-width: 170px;
  white-space: nowrap; /* 强制文字在一行内显示，不换行 */
  overflow: hidden; /* 隐藏超出宽度的文字 */
  text-overflow: ellipsis; /* 溢出部分显示省略号（...） */
}
.history-time {
  font-size: 12px;
  color: #999;
}
.history-actions{
  position: absolute;
  right: 0px;
  top: 7px;
}
.history-actionst{
  position: absolute;
  right: 0px;
  top: -1px;
}

/* 左侧导航栏（桌面端） */

.sidebar-header {
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}
.avatar {
  background: linear-gradient(135deg, #409EFF 0%, #1677ff 100%);
  color: white;
  max-height:24px ;
  max-width: 24px;
}
.username {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-left: 12px;
}
.sidebar-menu {
  flex: 1;
 
  padding: 8px 0;
}
.menu-item {
  display: flex;
  align-items: center;
  width: 90%;
  margin: 0 auto 8px;
  padding: 10px 16px;
  border-radius: 8px;
  background-color: #e6f4ff;
  color: #1677ff;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-size: 14px;
}
.menu-item-button {
  display: flex;
  align-items: center;
  width: 90%;
  margin: 0 auto 8px;
  padding: 10px 16px;
  border-radius: 8px;
  background-color: rgba(0,102,255,.1);
  color: #0057ff;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 0.5px solid rgba(0,102,255,.15);
  font-size: 14px;
}
.close-button{
  background-color: rgba(245, 108, 108, 0.2); 
  border: 1px solid #f56c6c; 
  color: #f56c6c; 
}
.new-chat{
  padding: 8px 12px;
  border-radius: 6px;
  height: 32px;
  font-size: 14px;
  margin-left: 0px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background-color: #e6f4ff;
  color: #1677ff;
}
.menu-item:hover {
  background-color: #d6e9ff;
}

.menu-item i {
  margin-right: 8px;
}
.new-chat-btn {
  font-weight: 500;
}
.divider {
  height: 1px;
  background-color: #f0f0f0;
  margin: 12px 0;
}
.menu-list {
  list-style: none;
}
.menu-list li {
  display: flex;
  align-items: center;
  padding: 10px 24px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s ease;
  font-size: 14px;
}
.menu-list li:hover {
  background-color: #f5f7fa;
}
.menu-list li.active {
  background-color: #e6f4ff;
  color: #1677ff;
  font-weight: 500;
}
.menu-list li i {
  width: 24px;
  margin-right: 12px;
}

.section-title {
  font-size: 12px;
  font-weight: 500;
  color: #999;
  margin: 12px 0 8px;
  text-transform: uppercase;
}

.history-list li {
  padding: 8px 12px;
  border-radius: 6px;
  height: 32px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-list li:hover {
  background-color: #f5f7fa;
}
.history-list li.active {
  background-color: #e6f4ff;
  color: #1677ff;
}

/* 主要聊天区域 */
.select-content{
  width: 100%;
  height: 80px;
  background: #fff;
  padding: 10px;
  margin-bottom: 15px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  border-radius: 10px;
}
::v-deep .el-select__wrapper{
  background: #f9fafb;
  border: none !important;
}
.select-title{
  font-size: 14px;
  font-weight: bold;
  text-align: left;
  padding-left: 2px;
  margin-bottom: 10px;
}
.select-item{
  width: 100% !important;
}
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
  //padding-top: calc(56px + 24px); /* 移动端导航高度 + 间距 */
  transition: margin-left 0.3s ease;
  position: relative;
  overflow-y: hidden;
}
.mobile-main { margin-left: 0 !important; }

/* 问答消息区域 */
.chat-content-wrapper {
  flex: 1;
  display: flex;
  justify-content: center; 
  padding: 0px 0;
  position: relative;
  max-height: 100vh;
 
}
.chat-content {
  width: 100%;
  max-width: 1200px; 
  padding: 0 200px;
  overflow-y: auto;
  padding-bottom: 80px;
  padding-top: 30px;

}

/* 开场白样式 */
.welcome-message {
  margin-bottom: 32px;
  text-align: center;
}
.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.welcome-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
}

/* 预设问题按钮 */
.preset-questions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}
.preset-btn {
  background-color: #e6f4ff;
  color: #1677ff;
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  white-space: nowrap;
}
.preset-btn:hover {
  background-color: #d6e9ff;
  transform: translateY(-1px);
}
.preset-btn:active {
  transform: translateY(0);
}

/* 消息样式 */
.message-item {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease forwards;
  opacity: 0;
  transform: translateY(10px);
  flex-direction: column;
  position: relative;
  .selectBox{
    width: 30px;
    height: 30px;
    position: absolute;
    left: -30px;
    top:calc(50% - 15px);
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
.showSelect{
  margin-left: 30px;
}
@keyframes fadeIn { to { opacity: 1; transform: translateY(0); } }
.user-msg { 
  justify-content: flex-end; 
  align-items: flex-end;
  margin-left: 0; 
}
.user-msg > div:first-child {
  display: flex;
  justify-content: flex-end;
  width: 100%;
  min-width: 0;
}
.ai-msg { 
  justify-content: flex-start; 
  align-items: stretch;
}
.text-box {
  width: 100%; /* 宽度等于父容器 */
  padding: 16px 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  transition: all 0.2s ease;
  position: relative;
  font-size: 15px;
  line-height: 1.6;
}
.text-box:hover { box-shadow: 0 6px 16px rgba(0,0,0,0.08); }
.user-msg .text-box { 
  align-self: flex-end;
  width: max-content;
  max-width: min(700px, 100%);
  background: #eef6ff;
  color: #102a43;
  border: 1px solid #cfe3ff;
  border-radius: 14px 14px 4px 14px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
  padding: 11px 15px;
  line-height: 1.7;
}
.ai-msg .text-box { 
  background-color: white;
  border-radius: 16px 16px 16px 16px;
}
.user-msg .message-renderer {
  align-items: flex-end;
  gap: 0;
  width: auto;
  max-width: 100%;
}
.user-msg :deep(.markdown-block) {
  color: inherit;
  max-width: 100%;
  line-height: inherit;
  overflow-wrap: break-word;
  word-break: normal;
  white-space: normal;
}
.user-msg :deep(.markdown-block p) {
  margin: 0 !important;
  overflow-wrap: break-word;
  word-break: normal;
}
.streaming::after {
  content: "";
  position: absolute;
  bottom: -8px;
  right: 20px;
  width: 16px;
  height: 16px;
  background-color: white;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.options{
  min-height: 0;
  height: auto;
  display: flex;
  align-items: flex-end;
  padding-left: 5px;
  padding-top: 6px;
}
.options:empty,
.user-msg .options {
  display: none;
  padding: 0;
}
.selete-header{
  height: 51px;
  width: 100%;
  position: absolute;
  left: 0px;
  top: 0px;
  display: flex;
  justify-content: center;
  border-bottom: 1px solid rgba(0,0,0,.12);
  padding-top: 10px;
  box-sizing: border-box;
  .selete-content{
    width: 800px;
    display: flex;
    justify-content: space-between;
   
  }
  .selete-title{
    font-size: 14px;
    font-weight: bold;
    color: #1c1f23;
  }
  .selete-button{

  }
}
.showTitle-div{
  height: 51px;
  width: 100%;
  position: absolute;
  left: 0px;
  top: 0px;
  display: flex;
  justify-content: left;
  padding-top: 5px;
  padding-left: 10px;
  box-sizing: border-box;
  flex-direction: column;
  font-size: 14px;
  color: #000;
  .showTitle-div-text{
    color:rgba(0, 0, 0, 0.5);
    font-size: 12px;
  }
  .showTitle-div-title{
    font-weight: bold;
  }
}
@media (max-width: 800px) {
  .showTitle-div{
    display: none;
  }
  
}
.selete-bottom{
  height: 69px;
  width: 100%;
  position: absolute;
  left: 0px;
  bottom: 0px;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgba(0,0,0,.12);
  .selete-bottom-content{
    width: 800px;
    display: flex;
    justify-content: space-between;
    height: 100%;
    align-items: center;
  }
.selete-button{
  width: 80px;
  height: 36px;
  background: #0057ff;
  text-align: center;
  line-height: 36px;
  border-radius: 8px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}
}
@keyframes typing {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

/* Markdown 内容样式 */
.text-box h1, .text-box h2, .text-box h3 { margin-bottom: 12px; color: inherit; font-weight: bold;}
.text-box h4, .text-box h5, .text-box h6 {  color: inherit; font-weight: bold;}
.text-box p { margin-bottom: 12px; }
.text-box ul, .text-box ol { margin: 4px 0 4px 12px; }
.text-box code { 
  background-color: rgba(0,0,0,0.05); 
  padding: 2px 6px; 
  border-radius: 4px; 
  font-size: 0.9em;
}
.user-msg .text-box code { background-color: rgba(255,255,255,0.2); }
.text-box pre {
  background-color: rgba(0,0,0,0.05);
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.text-box :deep(.code-block pre) {
  background: #f8fbff !important;
  padding: 16px !important;
  border-radius: 0 !important;
  margin: 0 !important;
}
.text-box :deep(.code-block code) {
  background: transparent !important;
  color: #1f2937 !important;
  padding: 0 !important;
  font-weight: 600;
}
.user-msg .text-box pre { background-color: rgba(255,255,255,0.1); }
.text-box blockquote {
  border-left: 4px solid #409EFF;
  padding-left: 12px;
  margin: 12px 0;
  color: #666;
  opacity: 0.9;
}
.user-msg .text-box blockquote { border-color: white; color: rgba(255,255,255,0.8); }

/* 输入框样式 */
.input-container {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 800px;
  padding: 0 24px;
}
.stop-button{
  position: absolute;
  bottom: 100px;
  height: 50px;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  max-width: 200px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}
.floating-input {
  width: 100%;
}
.input-wrap {
  display: flex;
  align-items: flex-start;
  background: white;
  border-radius: 24px;
  padding: 12px 16px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  transition: all 0.2s ease;
  border: 1px solid #e5e7eb;
}
.input-wrap:focus-within {
  border-color: #409EFF;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.1);
}
.input-icon {
  color: #409EFF;
  height: 36px;
  width: 36px;
  border-radius: 50%;
  font-size: 18px;
  margin: 3px 10px 0 0;
  margin-left: 8px;
  margin-right: 2px;
  background: rgba(64, 158, 255, 0.2);
  border: 1px solid rgba(64, 158, 255, 1);
}
.custom-textarea {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px;
  font-size: 15px;
  line-height: 1.6;
  min-height: 44px;
  max-height: 160px;
  background: transparent;
  resize: none;
  color: #333;
}
.send-btn {
  background: linear-gradient(135deg, #409EFF 0%, #1677ff 100%);
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 18px;
  margin-left: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  align-self: flex-start;
  margin-top: 4px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
}
.send-btn:hover { 
  background: linear-gradient(135deg, #66b1ff 0%, #409EFF 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.5);
}
.send-btn:active { transform: translateY(0); }
.send-btn:disabled { 
  background: #ccc;
  box-shadow: none;
  cursor: not-allowed;
}
.loading { display: flex; align-items: center; justify-content: center; }

/* 响应式适配 */
@media (min-width: 800px) {
  .mobile-top-nav {
    display: none; /* 桌面端隐藏移动端导航 */
  }
  // .main-content {
  //   padding-top: 24px; /* 恢复桌面端默认间距 */
   
  // }
}
@media (max-width: 800px) {
  .sidebar { display: none; } /* 小于800px时隐藏左侧导航 */
  .user-msg { margin-left: 0; }
  .user-msg .text-box {
    max-width: 88%;
    padding: 10px 13px;
  }

  .input-container { padding: 0 16px; }
  .welcome-message { text-align: left; }
  .preset-questions { justify-content: flex-start; }
  .main-content {
    padding-top: 24px; /* 恢复桌面端默认间距 */
    max-width: 100%;
    margin-top: 0px;
  }
  .chat-content{
    padding-left: 10px;
    padding-right: 10px;
  }
  .preset-btn{
    white-space:pre-wrap!important;
    text-align: left;
  }
  .welcome-subtitle{
    margin-bottom: 10px;
  }
  .text-box {
   
    padding: 12px 10px;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    transition: all 0.2s ease;
    position: relative;
    font-size: 14px;
    line-height: 1.6;
  }
}
@media (max-width: 600px) {
  .chat-content, .input-container { max-width: 100%; }
  .text-box { max-width: 99%; }
  .input-wrap { padding: 12px 12px; }
  .send-btn { padding: 8px 14px; }
  .preset-btn { padding: 6px 12px; font-size: 13px; }
  .chat-content-wrapper{padding: 0px 0;}
}

.chat-content::-webkit-scrollbar {
  display: none;
}

/* 表格样式 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}
.data-table th, .data-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
.data-table th {
  background-color: #f2f2f2;
  white-space: nowrap;
}
/* Markdown渲染优化 */
::v-deep .markdown-content {
  width: 100%;
  table {
    margin: 1.5rem auto;
    border-collapse: collapse;
    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
    padding-right: 20px;
    background: white;
    border-radius: 8px;
    width: max-content;
    max-width: 100%;
    overflow-x: auto;
    border: 1px solid var(--color-divider-regular);
    border-radius: 8px;
    display: block;
    th, td {
      padding: 12px 15px;
      text-align: center;
      border-bottom: 1px solid #e2e8f0;
      &:first-child { padding-left: 24px; }
      &:last-child { padding-right: 24px; }
      white-space: nowrap;
    }

    th {
      background-color: #f8fafc;
      font-weight: 600;
      color: #1e293b;
      letter-spacing: 0.03em;
      white-space: nowrap;
      
    }

    tr:last-child td {
      border-bottom: none;
    }

    /* 数值列特殊样式 */
    // td:nth-child(2) {
    //   font-family: 'JetBrains Mono', monospace;
    //   color: #3b82f6;
    //   font-weight: 500;
    // }
  }
  h3,h5,h4,h6,strong{
    color: #000;
    padding-bottom: 0.5rem;
    // margin-bottom: 1rem;
    padding-top: 0.5rem;
    font-weight:600;
    font-size: 16px;
  }
  h1{
    font-weight:1000 !important;
    color: #000;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    font-size: 24px;
    font-family: "SimHei", "Heiti SC";
  }
  h2,h3{
    font-weight:600 !important;
    color: #000;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    font-family: "SimHei", "Heiti SC";
    font-size:20px;
  }
// ========== 基础：ul / ol 通用（不改左右间距） ==========
ul,
ol {
  padding-left: 8px !important;   // 不动
  margin: 5px 20px !important;   // 不动
  position: relative;
  list-style: none !important;

  > li {
    padding-left: 0px !important; // 不动
    margin: 6px 0 !important;     // 不动
    line-height: 1.5 !important;  // 不动
    position: relative;
     overflow-wrap: break-word; /* 确保长单词或连续字符换行 */
  }

  // 二级列表（ul/ol嵌套）- 不动左右间距
  > li > ul,
  > li > ol {
    padding-left: 14px !important;       // 不动
    margin: 6px 0 6px 10px !important;   // 不动

    > li {
      padding-left: 0px !important;      // 不动
      margin: 4px 0 !important;          // 不动
      line-height: 1.45 !important;      // 不动
      position: relative;
       overflow-wrap: break-word; /* 确保长单词或连续字符换行 */
    }
  }
}

// ========== 无序列表：圆点对齐到“第一行” ==========
ul {
  > li::before {
    content: '' !important;
    position: absolute !important;
    left: -16px !important;          // 不动
    top: 0.75em !important;          // ✅ 关键：锚定第一行（替代 50%）
    width: 6px !important;
    height: 6px !important;
    border-radius: 50% !important;
    background: #666 !important;
    transform: translateY(-50%) !important; // ✅ 圆点垂直居中到第一行
  }

  > li > ul > li::before,
  > li > ol > li::before {
    content: '' !important;
    position: absolute !important;
    left: -14px !important;          // 不动
    top: 0.72em !important;          // ✅ 同样锚定第一行
    width: 5px !important;
    height: 5px !important;
    border-radius: 50% !important;
    background: #888 !important;
    transform: translateY(-50%) !important;
  }
}

// ========== 有序列表：数字对齐到“第一行” ==========
ol {
  counter-reset: top-level !important;

  > li {
    counter-increment: top-level !important;
     overflow-wrap: break-word; /* 确保长单词或连续字符换行 */

    &::before {
      content: counter(top-level) ". " !important;
      position: absolute !important;
      left: -18px !important;         // 不动
      top: 0.75em !important;         // ✅ 锚定第一行
      width: 16px !important;         // 不动
      color: #666 !important;
      font-size: 14px !important;
      font-weight: 400 !important;
      text-align: right !important;
      transform: translateY(-50%) !important; // ✅ 数字垂直居中到第一行
      background: transparent !important;
      border-radius: 0 !important;
    }

    // 二级有序列表
    > ol {
      counter-reset: sub-level !important;
      

      > li {
        counter-increment: sub-level !important;
         overflow-wrap: break-word; /* 确保长单词或连续字符换行 */

        &::before {
          content: counter(sub-level) ". " !important;
          position: absolute !important;
          left: -14px !important;      // 不动
          top: 0.72em !important;      // ✅ 锚定第一行
          width: 14px !important;      // 不动
          font-size: 13px !important;
          color: #888 !important;
          text-align: right !important;
          transform: translateY(-50%) !important;
          background: transparent !important;
          border-radius: 0 !important;
        }
      }
    }
  }
}
  pre {
    background: #f8fafc;
    border-radius: 8px;
    padding: 1rem;
    margin: 1rem 0;
    overflow-x: auto;
    
    code {
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.9em;
    }
  }
  a{
    color: #409EFF;
  }
  @media (max-width: 600px) {
    table{
      margin: 10px auto;
      width: 300px;
      overflow-x: auto;
      font-size: 12px;
      display: block;
      th, td {
        padding: 4px 5px;
        text-align: center;
        border-bottom: 1px solid #e2e8f0;
        &:first-child { padding-left: 14px; }
        &:last-child { padding-right: 14px; }
        white-space: nowrap;
      }
    }
    ul {
  padding-left: 12px !important;
  margin: 8px 10px !important;

  > li {
    margin: 4px 0 !important;
    line-height: 1.4 !important;

    &::before {
      left: -12px !important;
      top: 0.75em !important;                // ✅ 改：锚定第一行（不再用 50%）
      width: 5px !important;
      height: 5px !important;
      transform: translateY(-50%) !important; // ✅ 保留
    }

    // 移动端二级列表
    > ul,
    > ol {
      padding-left: 10px !important;
      margin: 4px 0 4px 8px !important;

      > li {
        margin: 2px 0 !important;
        line-height: 1.4 !important;          // ✅ 建议显式继承，避免不同环境不一致

        &::before {
          left: -10px !important;
          top: 0.72em !important;             // ✅ 改：二级同样锚定第一行
          width: 4px !important;
          height: 4px !important;
          transform: translateY(-50%) !important;
        }
      }
    }
  }
}

ol {
  padding-left: 12px !important;
  margin: 8px 10px !important;

  > li {
    margin: 4px 0 !important;
    line-height: 1.4 !important;

    &::before {
      left: -12px !important;
      top: 0.75em !important;                 // ✅ 改：锚定第一行
      transform: translateY(-50%) !important; // ✅ 保留
    }

    // 移动端二级列表
    > ul,
    > ol {
      padding-left: 10px !important;
      margin: 4px 0 4px 8px !important;

      > li {
        margin: 2px 0 !important;
        line-height: 1.4 !important;

        &::before {
          left: -10px !important;
          top: 0.72em !important;             // ✅ 改：锚定第一行
          width: 4px !important;
          height: 4px !important;
          transform: translateY(-50%) !important;
        }
      }
    }
  }
}

// 移动端有序列表 - 数字对齐到第一行（不改左右间距/宽度）
ol > li::before {
  left: -12px !important;
  top: 0.75em !important;                     // ✅ 改
  width: 12px !important;
  font-size: 12px !important;
  transform: translateY(-50%) !important;
}

ol > li > ol > li::before {
  left: -10px !important;
  top: 0.72em !important;                     // ✅ 改
  width: 10px !important;
  font-size: 11px !important;
  transform: translateY(-50%) !important;
}
  
  }
}
.tooltip-text{
  margin-left: 5px;
    white-space: nowrap;
    overflow: hidden; 
     text-overflow: ellipsis; 
     width: 160px;
}
.menu-message{
  display: flex;
  justify-content: left;
  align-items: center;
  position: relative;
  padding-right: 0px !important ;
  
  span{
    margin-left: 5px;
    white-space: nowrap;
    overflow: hidden; 
   
     text-overflow: ellipsis; 
     width: 160px;
     margin-bottom: 0px;
  }
}
.chart-container {
  margin: 1.5rem 0;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: all 0.3s;
  height: 500px;
}
@media (max-width: 800px) {
  .chart-container {
  margin: 1.5rem 0;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: all 0.3s;
  height: 500px;
}
}
.chart-container:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
::-webkit-scrollbar {
      width: 6px;
      height: 6px;
}
/* 滚动条轨道 */
::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}
/* 滚动条滑块 */
::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}
/* 滚动条滑块悬停状态 */
::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}
/* 滚动条滑块激活状态 */
::-webkit-scrollbar-thumb:active {
  background: rgba(0, 0, 0, 0.2);
}
//引导问题按钮样式
/* 引导问题按钮样式 */
.guide-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
  align-items: flex-start; /* 关键：让按钮左对齐，不拉伸 */
}
/* 引导按钮样式（保留你的原有样式，添加图标相关样式） */
.guide-btn {
  background-color: rgba(0,0,0,.04);
  color: rgba(0,0,0,.8);
  border: 1px solid #e6f4ff;
  padding: 9px 10px;
  padding-left: 16px;
  cursor: pointer;
  border-radius: 15px;
  transition: all 0.2s ease;
  font-size: 14px;
  text-align: left;
  line-height: 1.5;
  min-width: 100px;
  width: auto;
  max-width: 500px;
  white-space: normal;
  word-wrap: break-word;
  word-break: break-word;
  
  /* 按钮内部布局：文字和图标左右分布 */
  display: inline-flex;
  align-items: center;
  gap: 8px;  /* 文字和图标之间的间距 */
}

/* 箭头图标样式 */
.arrow-icon {
  color: rgba(0,0,0,.5);  /* 图标颜色 */
  font-size: 16px;       /* 图标大小 */
  margin-left: auto;     /* 推到最右侧 */
  transition: transform 0.2s ease;
}

.guide-btn:hover {
  background-color: #e6f4ff;
  border-color: #bae0ff;
  transform: translateX(2px);
}

/* 移动端适配 */
@media (max-width: 600px) {
  .guide-questions {
    gap: 6px;
    margin-top: 12px;
    padding-top: 10px;
  }
  
  .guide-btn {
    padding: 6px 10px;
    font-size: 12px;
    max-width: 260px;
  }
}

//退出登录和用户名称
.user-header{
  height: 51px;
  width: 100%;
  position: absolute;
  left: 0px;
  top: 0px;
  display: flex;
  justify-content: center;
  padding-top: 10px;
  box-sizing: border-box;
}
.el-select-dropdown .el-select-dropdown__item {
  padding-left: 15px !important; /* 调整选项文字的左侧缩进 */
}
.upload-btn{
  background: #fff;
  color: rgba(0, 0, 0, 0.85);
  border: none; 
  font-weight: 600;
  margin-bottom: 0px;

}
.upload-btn:hover{
  background: rgba(0, 0, 0, 0.15);
}


.diviproductionder-border{
  margin-left: 13px;
  margin-right: 13px;
  .diviproductionder-border-content{
    border-bottom: 0.5px solid  rgb(200, 201, 204);
  }
  padding-top: 10px;
  padding-bottom: 10px;
}


.data-op-btn {
  background-color: #ECF5FF; /* 和新对话的浅蓝底色一致 */
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.data-op-btn:hover {
  background-color: #D9ECFF; /* hover效果和新对话统一 */
}

/* 弹窗样式优化 */
.data-operation-popover {
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  border: 1px solid #E4E7ED;
}
.operation-buttons {
  display: flex;
  flex-direction: column;
  gap: 0; /* 取消多余间距 */
}
.operation-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  border: none;
  background: transparent;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}
.operation-btn:hover {
  background-color: #F5F7FA; /* 轻量hover背景 */
}
/* 停止按钮容器：适配多按钮布局 */
.stop-button {
  position: absolute;
  bottom: 100px;
  height: 50px;
  left: 50%;
  transform: translateX(-50%);
  width: auto; /* 改为自适应宽度 */
  max-width: 400px; /* 增大最大宽度 */
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px; /* 按钮间距 */
  z-index: 10;
}
/* 音频控制按钮核心样式：醒目且和整体风格统一 */
.audio-control-btn {
  padding: 8px 12px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
/* 停止播放状态（红色+暂停图标）：醒目提示正在播放 */
.audio-control-btn-danger {
  background-color: #ccc !important;
  border-color:none !important;
  color: white !important;
}
 .audio-control-btn-danger:hover {
  background-color: #E43939;
  border-color: #E43939;
  transform: translateY(-1px); /* 轻微上浮 */
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3); /* 增强阴影 */
}



/* 移动端适配：按钮尺寸缩小，间距调整 */
@media (max-width: 600px) {
  .stop-button {
    bottom: 90px;
    gap: 8px;
    max-width: 90%;
  }
  .audio-control-btn {
    padding: 6px 12px;
    font-size: 12px;
  }
  .stop-button .el-button {
    padding: 6px 10px;
    font-size: 12px;
  }
}

.mobile-top-nav .right-section .audio-control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background-color: #ecf5ff; /* 与新对话按钮一致的浅蓝底色 */
  color: #409EFF; /* 主色调 */
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mobile-top-nav .right-section .audio-control-btn:hover {
  background-color: #d0eaff; /* hover效果加深 */
}

.mobile-top-nav .right-section .audio-control-btn .el-icon {
  font-size: 18px; /* 图标大小 */
}

/* 适配小屏幕布局 */
@media (max-width: 375px) {
  .mobile-top-nav .right-section .audio-control-btn {
    width: 32px;
    height: 32px;
  }
  
  .mobile-top-nav .right-section .audio-control-btn .el-icon {
    font-size: 16px;
  }
  
  /* 小屏幕下右侧按钮间距缩小 */
  .mobile-top-nav .right-section {
    gap: 4px;
  }
}

/* 确保数据操作按钮和音频按钮布局协调 */
.mobile-top-nav .right-section .data-op-btn {
  margin: 0; /* 重置margin，使用gap控制间距 */
}

//新增按钮历史对话左侧样式
/* 1. sidebar 容器设为 Flex 列布局，高度占满屏幕 */
.sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  height: 100vh; /* 关键：固定高度 */
  overflow: hidden; /* 防止整体滚动 */
}

.sidebar-header {
  /* 头部保持固定高度，flex-shrink: 0 防止被压缩 */
  flex-shrink: 0; 
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}

/* 2. sidebar-menu 设为 Flex 列布局，占据剩余空间 */
.sidebar-menu {
  flex: 1; /* 占据 sidebar 剩余空间 */
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  overflow: hidden; /* 内部滚动，容器本身不滚动 */
  min-height: 0; /* Firefox/Flexbox 嵌套滚动的关键修复 */
}

/* 按钮样式微调 */
.menu-item-button {
  flex-shrink: 0; /* 防止按钮被压缩 */
}

/* 更多功能按钮样式 */
.toggle-more-btn {
  background-color: transparent;
  border: 1px dashed rgba(0, 102, 255, 0.3);
  color: #666;
  justify-content: left;
}
.toggle-more-btn:hover {
  background-color: rgba(0, 0, 0, 0.02);
  color: #409EFF;
  border-color: #409EFF;
}

/* 3. 自定义按钮区域样式 */
.custom-buttons-area {
  overflow: hidden;
  transition: max-height 0.3s ease-in-out;
  max-height: 0; /* 默认收起 */
  
  /* 如果展开内容特别多，也允许内部滚动，不影响整体 */
  overflow-y: auto; 
}

.custom-buttons-area.is-expanded {
  max-height: 400px; /* 展开后的最大高度，超过出滚动条 */
  border-bottom: 1px dashed #eee;
  margin-bottom: 5px;
}
/* 自定义滚动条样式 */
.custom-buttons-area::-webkit-scrollbar {
  width: 4px;
}
.custom-buttons-area::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.1);
  border-radius: 2px;
}

.diviproductionder {
  padding-bottom: 5px;
}

/* 4. 历史对话区域 (Flex 自适应核心) */
.history-section {
  padding: 0 16px;
  flex: 1; /* ✅ 关键：自动填充剩余的所有空间 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止双重滚动条 */
  /* 移除原本的固定 height: calc(...) */
  height: auto !important; 
  min-height: 0; /* Flexbox 嵌套滚动的关键 */
}

.history-list {
  list-style: none;
  flex: 1; /* 列表占据 section 内部的所有空间 */
  overflow-y: auto; /* ✅ 只有列表内容区域滚动 */
  padding-bottom: 10px;
}

/* 5. 底部按钮区域（加载更多/退出登录） */
.sidebar-footer {
  flex-shrink: 0; /* 不被压缩 */
  padding-top: 10px;
  background: #fff; /* 防止列表滚动时文字重叠 */
  z-index: 2;
}

/* 原有的历史标题微调 */
.history-title-text-t {
  padding-left: 11px;
  margin-bottom: 8px;
  margin-top: 10px;
  color: rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
}


/* =========================================
   底部 "和AI对话" 悬浮按钮样式 (iOS 适配版)
   ========================================= */
.bottom-button {
  position: fixed; /* 强制固定在屏幕上，脱离文档流 */
  left: 50%;
  transform: translateX(-50%); /* 水平居中 */
  
  /* 核心适配：基础距离 + iOS底部安全区域距离 */
  bottom: calc(30px + env(safe-area-inset-bottom)); 
  
  z-index: 2000; /* 保证层级最高，不被内容遮挡 */
  
  /* 按钮视觉样式 */
  width: 140px;
  height: 44px;
  background: #007AFF; /* 鲜艳的蓝色，类似 iOS 风格 */
  color: #fff;
  border-radius: 22px; /* 胶囊形状 */
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 122, 255, 0.3); /* 漂亮的投影 */
  transition: all 0.2s ease;
  
  /* 点击反馈 */
  &:active {
    transform: translateX(-50%) scale(0.96);
    background: #0062cc;
  }
}

/* 针对 iOS Safari 浏览器的特殊处理 (防止地址栏遮挡) */
@supports (-webkit-touch-callout: none) {
  .bottom-button {
    /* 在 iOS 上稍微再抬高一点，防止误触底部横条 */
    bottom: calc(40px + env(safe-area-inset-bottom));
  }
}

/* 深度思考总容器 */
.thought-section {
  margin: -4px -4px 16px -4px; /* 稍微负边距让它在 text-box 内更紧凑 */
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.02); /* 极浅的底色 */
  border: 1px solid rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* 思考头部 - 按钮质感 */
.thought-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.05);
  }
}

.thought-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399; /* 柔和的灰色 */
  font-size: 13px;
  font-weight: 500;

  .brain-icon {
    font-size: 16px;
    color: #409EFF; /* 给小图标一点品牌色，增加精致感 */
    opacity: 0.8;
  }
}

.thought-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #a8abb2;

  .el-icon {
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    &.is-rotated {
      transform: rotate(90deg);
    }
  }
}

/* 思考内容主体 */
.thought-body-wrapper {
  /* 左侧引导线，这是区分的关键 */
  border-left: 2px solid #dcdfe6; 
  margin: 0 14px 12px 20px;
  padding-left: 12px;
}

.thought-content-inner {
  font-size: 13.5px;
  color: #606266; /* 比正文稍淡 */
  line-height: 1.7;
  letter-spacing: 0.01em;
  
  /* 关键优化：弱化思考盒内的 Markdown 元素 */
  ::v-deep {
    p { margin: 8px 0; }
    h1, h2, h3, h4 { 
      font-size: 14px; 
      color: #909399; 
      margin: 12px 0 6px 0;
    }
    code { 
      background-color: rgba(0, 0, 0, 0.04); 
      font-size: 12px; 
    }
    ul, ol {
      padding-left: 15px;
      color: #909399;
    }
  }
}

/* 正式回复区域优化 */
.response-section {
  position: relative;
  
  /* 如果上方有思考过程，增加一个视觉分割线或间距 */
  &.has-thought {
    margin-top: 10px;
    padding-top: 10px;
    /* 可选：添加一条非常淡的虚线分割线 */
    // border-top: 1px dashed rgba(0, 0, 0, 0.05); 
  }

  /* 强化正文标题的渲染 */
  ::v-deep {
    h1, h2, h3 {
      color: #1a1a1a;
      margin-top: 1.5em;
      margin-bottom: 0.8em;
      &:first-child { margin-top: 0; }
    }
    p {
      color: #303133;
      line-height: 1.8;
    }
  }
}

/* 修正 streaming 动画位置 */
.streaming::after {
  /* 确保光标在 response-section 内部的末尾 */
  position: relative;
  display: inline-block;
  vertical-align: middle;
}
</style>
