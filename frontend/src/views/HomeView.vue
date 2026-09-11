<template>
  <div class="chat-container"  v-loading.fullscreen.lock="Httploading">
    <header class="mobile-top-nav" v-if="isMobile&& !showSelect">
      <div class="left-section">
        <button class="menu-btn" @click="openHistory">
          <el-icon><Menu /></el-icon>
        </button>
        
        <div class="mobile-user-profile" v-if="loginmodel !== 'noLogin'" @click="openProfile">
          <el-avatar :size="22" icon="Avatar" class="mobile-avatar"></el-avatar>
          <span class="mobile-username">{{ username }}</span>
        </div>
      </div>

      <div class="right-section">
        <div class="new-chat-btn-mobile" @click="createNewChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </div>
        <button 
           v-if="audioFlag=='1'"
           class="mobile-audio-btn" 
           @click="mobileToggleAllAudio"
           :class="{'mobile-audio-btn-danger': audioState === '2'}"
         >
           <el-icon v-if="audioState === '1'"><Microphone /></el-icon>
           <el-icon v-else><Mute /></el-icon>
         </button>
        <el-popover
          placement="bottom"
          width="180"
          trigger="click"
          popper-class="data-operation-popover"
          :teleported="true"
          v-if="menuButton&&menuButton.length&&menuButton.length>0"
        >
          <template #reference>
            <div class="new-chat data-op-btn" @click.stop>
              <el-icon style="font-size: 16px;"><Setting style="color: #409EFF;margin-right: 5px;font-size: 16px;" /></el-icon>
              <span class="new-chat-text">数据操作</span>
            </div>
          </template>
          <div class="operation-buttons" >
            <button v-for="item in menuButton"  :key="item.name" @click="showFileUploadclick(item.route,item.name)" class="operation-btn" >
              <el-icon v-if="item.route.indexOf('jwgk_importPriceDatas')>=0"><Upload /></el-icon>
              <el-icon v-if="item.route.indexOf('jwgk_fillPriceDatas')>=0"><SetUp /></el-icon>
              <el-icon v-if="item.route.indexOf('jwgk_queryPriceDatas')>=0"><ZoomIn /></el-icon>
              <span>{{item.name}}</span>
            </button>
         
          </div>
        </el-popover>
      </div>
    </header>

    <el-drawer
      title="历史对话"
      v-model="showHistoryDrawer"
      size="300px"
      :with-header="false"
      direction="ltr"
    >
      <div class="history-drawer">
        <div class="drawer-header">
          <h3>历史对话</h3>
          <el-button icon="Close" circle size="small" @click="showHistoryDrawer = false"></el-button>
        </div>
        <ul class="drawer-history-list"  v-infinite-scroll="loadHistory"   v-loading.fullscreen.lock="fullscreenLoading">
          <li 
            v-for="(history, index) in historyList" 
            :key="index" 
            @click="selectHistory(index)"
            class="history-item"
          >
            <div class="history-title">
              
              <div class="history-title-text">{{ history.name }}</div>
              <div class="history-actions history-actionst">
                <button 
                  class="action-btn del-btn del-btnt" 
                  @click.stop="deleteHistory(index,true)"
                >
                  <el-icon size="16" class="icon"><Delete /></el-icon>
                </button>
                <button 
                  class="action-btn  action-btnt" 
                  @click.stop="renameHistory(index)"
                 
                >
                  <el-icon size="16" class="icon"><Edit /></el-icon>
                </button>
              </div>
            </div>
          </li>
        </ul>
        <button v-if="loginmodel !='noLogin'&&loginmodel !='ssoTokenLogin'" class="menu-item new-chat-btn menu-item-button close-button" @click="outLogin">
        <el-icon><CloseBold /></el-icon>
          <span>退出登录</span>
        </button>
      </div>
    </el-drawer>
 
    <aside class="sidebar" v-if="!isMobile">
      <div class="sidebar-header">
        <el-avatar :size="30" icon="Avatar" class="avatar"></el-avatar>
        <div class="user-info" v-if="loginmodel !='noLogin'">
          <h3 class="username">{{username}}</h3>
        </div>
        <div class="user-info" v-else>
          <h3 class="username">{{showTitle}}</h3>
        </div>
      </div>

      <div class="sidebar-menu">
        <button class="menu-item new-chat-btn menu-item-button" @click="createNewChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </button>

        <button 
          v-if="menuButton && menuButton.length > 0" 
          class="menu-item-button toggle-more-btn" 
          @click="toggleMenuExpand"
        >
          <el-icon>
            <ArrowDown v-if="!isMenuExpanded" />
            <ArrowUp v-else />
          </el-icon>
          <span style="margin-left: 5px;">{{ isMenuExpanded ? '收起功能' : '更多功能' }}</span>
        </button>

        <div 
          class="custom-buttons-area" 
          :class="{ 'is-expanded': isMenuExpanded }"
          v-if="menuButton && menuButton.length > 0"
        >
          <div class="diviproductionder" ref="diviproductionder">
            <button 
              v-for="item in menuButton" 
              :key="item.name" 
              @click="showFileUploadclick(item.route,item.name)"  
              class="menu-item-button upload-btn" 
            >
              <el-icon v-if="item.route.indexOf('jwgk_importPriceDatas')>=0"><Upload /></el-icon>
              <el-icon v-if="item.route.indexOf('jwgk_fillPriceDatas')>=0"><SetUp /></el-icon>
              <el-icon v-if="item.route.indexOf('jwgk_queryPriceDatas')>=0"><ZoomIn /></el-icon>
              <span style="margin-left: 5px;">{{item.name}}</span>
            </button>
          </div>
        </div>

        <div class="diviproductionder-border" v-if="menuButton&&menuButton.length&&menuButton.length>0">
          <div class="diviproductionder-border-content"></div>
        </div>

        <div class="history-section" ref="historySection">
          <div class="history-title-text-t">历史对话</div>
          <ul 
            class="history-list" 
            v-infinite-scroll="loadHistory" 
            infinite-scroll-immediate="false" 
            v-loading.fullscreen.lock="fullscreenLoading"
          >
            <li 
              v-for="(history, index) in historyList" 
              :key="index" 
              @click="selectHistory(index)"
              @mouseenter="handleMouseEnter(index)"
              @mouseleave="handleMouseLeave(index)"
              :class="{ active: currentHistory === index+1,'menu-message':true }"
            >
              <el-icon><Message /></el-icon>
              <div style="margin-left: 3px;" class="history-title-window">
                {{ history.name }}
              </div>
              <div class="history-actions">
                <button class="action-btn del-btn" @click.stop="deleteHistory(index)" v-if="history.hovered">
                  <el-icon size="16" class="icon"><Delete /></el-icon>
                </button>
                <button class="action-btn" @click.stop="renameHistory(index)" v-if="history.hovered">
                  <el-icon size="16" class="icon"><Edit /></el-icon>
                </button>
              </div>
            </li>
          </ul>
        </div>
        
        <div class="sidebar-footer">
          <button class="menu-item new-chat-btn menu-item-button" @click="loadHistory">
            <el-icon><Refresh /></el-icon>
            <span>加载更多</span>
          </button>
          <button 
            v-if="loginmodel !='noLogin'&&loginmodel !='ssoTokenLogin'&&thirdLogin!='third'" 
            class="menu-item new-chat-btn menu-item-button close-button" 
            @click="outLogin"
          >
            <el-icon><CloseBold /></el-icon>
            <span>退出登录</span>
          </button>
        </div>

      </div>
    </aside>

    <main  class="main-content" :class="{'share-mode-padding': showSelect}">
      <div class="chat-content-wrapper" ref="contentWrapper">
        <div class="chat-content" ref="chatContent" @wheel="handleWheel">
          <div class="welcome-message">
            <div v-if="options&&options.length&&options.length>0" class="select-content">
              <div class="select-title">
                {{selectTitle}}
              </div>
              <el-select class="select-item"  v-model="selectValue"  placeholder=""  style="background-color: #f9fafb;">
              <el-option
                v-for="item in options"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            </div>
           
            <h2 class="welcome-subtitle">{{title}}</h2>
            <div class="preset-questions">
              <button 
                v-for="(question, index) in presetQuestions" 
                :key="index" 
                class="preset-btn"
                @click="sendPresetQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>
          
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="{ 'user-msg': msg.isUser, 'ai-msg': !msg.isUser,'showSelect': showSelect}"
           
          >
            <div v-if="msg.isUser">
              <div class="text-box" :class="{ 'streaming': msg.isStreaming }">
                <MessageRenderer :items="msg.messageArray" :is-streaming="msg.isStreaming" />
              </div>
            </div>
            <div v-else>
              <div class="text-box" v-if="msg.isLoading"  v-loading="true"  >正在思考中...</div>
              <div v-else-if ="!msg.loading" class="text-box" :class="{ 'streaming': msg.isStreaming,'markdown-content':true }">
                
                <div class="transitions-wrapper" v-if="msg.transitions && msg.transitions.length > 0">
                  <div class="transition-step" v-for="(step, sIdx) in msg.transitions" :key="sIdx">
                    <el-icon :class="{'is-loading': msg.isStreaming && sIdx === msg.transitions.length - 1, 'success-icon': !msg.isStreaming || sIdx < msg.transitions.length - 1}">
                      <Loading v-if="msg.isStreaming && sIdx === msg.transitions.length - 1" />
                      <Check v-else />
                    </el-icon>
                    <span>{{ step }}</span>
                  </div>
                </div>
                <MessageRenderer :items="msg.messageArray" :is-streaming="msg.isStreaming" />
                <div v-if="false" v-for="(info, index) in msg.messageArray" :key="index">
                  
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
                        <div class="thought-content-inner" v-html="renderMarkdown({ text: info.text })"></div>
                      </div>
                    </el-collapse-transition>
                  </div>

                  <div v-else>
                    <div v-memo="[info.text]" v-html="renderMarkdown(info)"></div>
                  </div>
                  
                </div>
              </div>

            </div>
            <div class="options" v-if="!showSelect && !msg.isUser && (!msg.isStreaming || (audioFlag=='1'&&msg.showAudio))">
                <div v-if="!showSelect&&!msg.isStreaming&&!msg.isUser">
                  <el-button text icon="Edit" @click="copyMessageCLCIK(msg.messageArray,'',true)" >复制</el-button>
                  <el-button @click="showSelectClick(index)" style="margin-left: 10px;"text icon="Share" >分享</el-button>
                </div>
                <div v-if="!showSelect&&audioFlag=='1'&&!msg.isUser&&msg.showAudio">
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
            <div v-if="showSelect" class="selectBox">
              <input  @change="changeCheck(msg,index)"  type="checkbox" v-model="selectMessage" :value="index">
            </div>
          </div>

          <div v-if="guideQuestions&&guideQuestions.length&&guideQuestions.length>0" class="guide-questions">
            <button 
              v-for="(question, qIndex) in guideQuestions" 
              :key="qIndex" 
              class="guide-btn"
              @click="sendPresetQuestion(question)"
            >
            {{ question }}
            <el-icon class="arrow-icon">
              <Right />
            </el-icon>
            </button>
          </div>
        </div>
      </div>
      <div v-if="showStop"  class="stop-button">
        <el-button  v-loading="clickStop" type="info"  @click="stopMessage()" style="padding-left: 10px;padding-right: 10px;" plain><el-icon style="margin-right: 5px;"><Stopwatch /></el-icon>停止回答</el-button>
         <el-button 
          v-if="!isMobile&&hasPlayingAudio" 
          :type="audioState=='1' ? 'danger' : 'success'" 
          @click="toggleAllAudio" 
          class="audio-control-btn"
          plain
        >
          <el-icon style="margin-right: 5px;" v-if="audioState=='1'"><VideoPause /></el-icon>
          <el-icon style="margin-right: 5px;" v-if="audioState=='2'"><VideoPlay /></el-icon>
          {{ audioState=='1' ? '停止播放' : '自动播放' }}
        </el-button>
      </div>
      <div v-if="!showSelect" class="input-container" ref="inputContainer">
        <div class="floating-input">
          <div class="input-wrap">
            <voice @result="handleVoiceResult" v-if="false" ></voice>
            <textarea
              v-model="inputValue"
              class="custom-textarea"
              placeholder="和AI聊天"
              @keydown.enter="handleEnter"
              ref="textarea"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="!inputValue.trim()||inputDisable">
              <span v-if="!isLoading">发送</span>
                <span v-else class="loading">
                  <i class="el-icon-loading"></i>
              </span>
            </button>
        </div>
        </div>
      </div>
      <div v-if="showSelect" class="selete-header">
        <div  class="selete-content">
          <div class="selete-title">分享对话</div>
          <div @click="cancelSelect" class="selete-button">取消</div>
        </div>
        
      </div>
      <div v-if="showSelect" class="selete-bottom">
        <div  class="selete-bottom-content">
          <div >
            <el-checkbox label="全选" @change="checkall" v-model="checkallvalue" size="large" />
            </div>
          <div style="display: flex; gap: 12px;">
            <div class="selete-button image-btn" @click="generateImage">
               生成长图
            </div>
            <div class="selete-button" @click="shareClick">
               复制链接
            </div>
          </div>
        </div>
      </div>
      <div v-if="!showSelect&&loginmodel !='noLogin'" class="showTitle-div">
          <div class="showTitle-div-title" >{{showTitle}}</div>
          <div class="showTitle-div-text">{{showTitledescribe}}</div>
      </div>

      <HistoryDrawer
      v-model="showFileUpload"
      @update:show="handleDialogClose"
      :model-state="modelState"
      :model-title="modelTitle"
    />
    </main>
  </div>
</template>

<script >
import { ref, computed, onMounted, onUnmounted, nextTick, watch  } from 'vue';
import HistoryDrawer from './compenents/HistoryDrawer.vue';
import voice from './compenents/voice.vue';
import MessageRenderer from '@/components/chat/MessageRenderer.vue';
import router from '@/router'
import {marked} from 'marked';
import { onBeforeRouteLeave } from 'vue-router'
import html2canvas from 'html2canvas';

// 仅引入核心库和 SQL 语言
import hljs from 'highlight.js/lib/core';
import sql from 'highlight.js/lib/languages/sql';
import 'highlight.js/styles/atom-one-dark.css';
import { v4 as uuidv4 } from 'uuid';
// 注册 SQL 语言
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('plaintext', () => ({
  name: 'plaintext',
  aliases: ['text', 'txt'],
  contains: [] // 无任何语法规则，纯文本展示
}));

import axios from '@/apis/request'
import { streamChat } from '@/composables/useChatStream';
import { buildChartOption } from '@/utils/chartOptions';
import { buildMessageItemsFromRawText, TRANSITION_LINE_RE } from '@/utils/messageSegments';
import DOMPurify from 'dompurify';
import * as echarts from 'echarts/core';

// 按需引入图表类型
import { 
  PieChart,
  LineChart,
  BarChart
} from 'echarts/charts';

// 按需引入组件
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  ToolboxComponent  
} from 'echarts/components';

// 按需引入渲染器
import { CanvasRenderer } from 'echarts/renderers';

// 注册必须的组件和图表
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  PieChart,
  LineChart,
  BarChart,
  CanvasRenderer,
  ToolboxComponent  
]);


export default {
  components: { HistoryDrawer ,voice, MessageRenderer},
  name:'home',
  setup() {
    let baseUrl = window.$httpbaseURL
    if(import.meta.env.VITE_APP_TITLE == '生产环境'){
      baseUrl = window.$httpbaseURL
    }

    // 响应式判断
    const isMobile = computed(() => window.innerWidth < 800);
    const showSidebar = ref(false);
    const toggleSidebar = () => showSidebar.value = !showSidebar.value;

    // 历史对话抽屉
    const showHistoryDrawer = ref(false);
    const openHistory = () => showHistoryDrawer.value = true;
    
    // 预设问题数组
    const presetQuestions = ref([]);

    // 对话数据
    const inputValue = ref('');
    const isLoading = ref(false);
    const title = ref('');
    const messages = ref([ ]);
    const chatContent = ref(null);
    const textarea = ref(null);
    const inputContainer = ref(null);
    const contentWrapper = ref(null);
    const historySection = ref(null);
    const conversation_id  = ref('')
    const selectMessage = ref([]);
    const showSelect = ref(false);
    const showStop= ref(false);
    const task_id = ref('')
    const clickStop = ref(false)
    const inputDisable = ref(false)
    const Httploading = ref(false)
    const showNewchat = ref(false)
    const stopstate = ref(false)
    let currentStreamController = null
    const regex = /```/g;

    const options = ref([]);
    const selectValue = ref(null)
    const selectTitle = ref(null)
    const selectOption = ref(null)
    const selectrequired = ref(false)
    const showTitle = ref('AI对话')
    const showTitledescribe = ref('按岗定数、精准控域')

    //引导问题按钮
    const guideQuestions = ref([]);
    const username = ref('')

    //上传文件弹框
    const showFileUpload = ref(false)
   
    //问题提问去掉接口
    const suggestedQuestions = (message_id)=>{
      axios.get('/suggested-questions?message_id='+message_id).then(res=>{
        try {
          if(res.data.isArray()){
            guideQuestions.value = res.data
          }
        } catch (error) {
          guideQuestions.value = []
        }
      })
    }
    // 提取追问问题的工具函数
    const extractSuggestedQuestions = (text) => {
      // 匹配<suggested_start>和<suggested_end>之间的内容
      const regex = /<suggested_start>([\s\S]*?)<suggested_end>/;
      const match = text.match(regex);
      
      if (!match || !match[1]) return [];
      
      // 按|||分割，过滤空值、去重、清理首尾空格
      return match[1]
        .split('|||')
        .map(item => item.trim())
        .filter(item => item) // 过滤空字符串
        .filter((item, index, self) => self.indexOf(item) === index); // 去重
    };
    const syncAssistantMessage = (targetMsg, rawText, isStreaming = true) => {
      const transitionMatches = [...rawText.matchAll(/^\s*Transition\s*[:：]\s*(.*?)(?=\r?\n|$)/gm)];
      targetMsg.transitions = transitionMatches.map(item => item[1].trim()).filter(Boolean);

      const previous = targetMsg.messageArray || [];
      let index = 0;
      const nextItems = buildMessageItemsFromRawText(
        rawText.replace(TRANSITION_LINE_RE, ''),
        () => targetMsg.uuid + '-seg-' + index++
      );

      nextItems.forEach((item, itemIndex) => {
        const oldItem = previous[itemIndex];
        if (oldItem?.isThought && item.isThought) {
          item.thoughtCollapsed = oldItem.thoughtCollapsed;
          item._autoCollapsed = oldItem._autoCollapsed;
        }

        if (item.isThought && item.hasFinishedThinking && !item._autoCollapsed) {
          const restTextLength = nextItems
            .slice(itemIndex + 1)
            .filter(next => !next.isThought)
            .reduce((total, next) => total + String(next.text || '').trim().length, 0);
          if (restTextLength > 10) {
            item.thoughtCollapsed = true;
            item._autoCollapsed = true;
          }
        }
      });

      targetMsg.messageArray = nextItems.length > 0
        ? nextItems
        : [{
            isCode: false,
            text: isStreaming ? rawText : '',
            uuid: targetMsg.uuid + '-empty',
            isThought: false,
            thoughtCollapsed: false,
            hasFinishedThinking: false
          }];
    };

    const finishAssistantMessage = (targetMsg, rawAnswer) => {
      targetMsg.isStreaming = false;
      targetMsg.isLoading = false;
      showStop.value = false;
      inputDisable.value = false;
      currentStreamController = null;

      const extractedQuestions = extractSuggestedQuestions(rawAnswer);
      if (extractedQuestions.length > 0) {
        guideQuestions.value = extractedQuestions;
      }

      if (showNewchat.value) {
        axios.get('/conversations').then(res => {
          historyList.value = res.data.conversations;
          showNewchat.value = false;
          currentHistory.value = 1;
        });
      }
    };

    const fetchStreamData = async (message) => {
      guideQuestions.value = [];
      doubleScrollDetected.value = false;
      inputDisable.value = true;
      stopstate.value = false;

      const newMessageIndex = messages.value.length;
      const assistantUuid = uuidv4();
      const assistantMessage = {
        messageArray: [{
          isCode: false,
          text: '',
          uuid: assistantUuid + '-empty',
          isThought: false,
          thoughtCollapsed: false,
          hasFinishedThinking: false
        }],
        transitions: [],
        isUser: false,
        isLoading: true,
        avatarColor: '#409EFF',
        timestamp: new Date(),
        isStreaming: true,
        conversation_id: conversation_id.value,
        messageId: '',
        uuid: assistantUuid,
        audioFetched: false,
        showAudio: false
      };
      messages.value.push(assistantMessage);

      const queryObj = {
        message,
        stream: true,
        conversation_id: conversation_id.value,
        query: message,
        inputs: {}
      };
      queryObj.inputs[selectOption.value] = selectValue.value;

      let rawAnswer = '';
      currentStreamController = new AbortController();

      try {
        await streamChat({
          url: baseUrl + '/chat',
          token: window.localStorage.getItem('_token'),
          payload: queryObj,
          signal: currentStreamController.signal,
          onMessage: (newData) => {
            if (stopstate.value) {
              currentStreamController?.abort();
              return;
            }

            if (newData?.error) {
              throw new Error(newData.error);
            }

            const targetMsg = messages.value[newMessageIndex];
            if (!targetMsg) return;

            if (newData?.event === 'agent_message') {
              if (newData.answer) {
                rawAnswer += newData.answer;
                targetMsg.isLoading = false;
                syncAssistantMessage(targetMsg, rawAnswer, true);
              }

              if (newData.conversation_id) {
                conversation_id.value = newData.conversation_id;
                targetMsg.conversation_id = newData.conversation_id;
              }

              if (newData.task_id) {
                task_id.value = newData.task_id;
                showStop.value = true;
              }

              if (newData.message_id) {
                targetMsg.messageId = newData.message_id;
                targetMsg.showAudio = true;
                if (!targetMsg.audioFetched && audioState.value === '1' && audioFlag.value == '1') {
                  playAudio(newData.message_id);
                  targetMsg.audioFetched = true;
                }
              }
            }

            nextTick(() => {
              scrollToBottom();
            });
          },
          onClose: () => {
            const targetMsg = messages.value[newMessageIndex];
            if (targetMsg) {
              syncAssistantMessage(targetMsg, rawAnswer, false);
              finishAssistantMessage(targetMsg, rawAnswer);
            }
          }
        });
      } catch (error) {
        if (error?.status === 401 || error?.message === 'LOGIN_EXPIRED') {
          handleLoginExpired();
          return;
        }

        if (stopstate.value || error?.name === 'AbortError') {
          const targetMsg = messages.value[newMessageIndex];
          if (targetMsg) {
            syncAssistantMessage(targetMsg, rawAnswer, false);
            finishAssistantMessage(targetMsg, rawAnswer);
          }
          stopstate.value = false;
          return;
        }

        console.log(error);
        stopstate.value = false;
        showStop.value = false;
        inputDisable.value = false;
        currentStreamController = null;

        messages.value.splice(newMessageIndex, 1);
        messages.value.push({
          messageArray: [{
            isCode: false,
            text: 'Error: ' + error.message,
            uuid: uuidv4()
          }],
          transitions: [],
          isUser: false,
          avatarColor: '#f56c6c',
          timestamp: new Date(),
          streamContent: '',
          isStreaming: false
        });
        await nextTick();
      }
    };

    // ??????
    const sendPresetQuestion = (question) => {
      if(inputDisable.value){
        ElMessage.error('请等待对话完成!')
        return false
      }
      inputValue.value = question;
      sendMessage();
    };
    // 输入框调整
    const adjustInputPosition = () => {
      if (!contentWrapper.value || !inputContainer.value) return;
      const inputHeight = inputContainer.value.getBoundingClientRect().height;
      if( contentWrapper.value.offsetHeight> window.screen.height* 0.8){
        contentWrapper.value.style.paddingBottom = `${inputHeight + 10}px`;
        inputContainer.value.style.bottom = '10px';
      }else{
        contentWrapper.value.style.paddingBottom = `${inputHeight + 10}px`;
        inputContainer.value.style.bottom = '10px';
      }
    };

    // 发送消息
    const sendMessage = () => {
      let showMessage = true
      if(selectrequired.value){
        if(selectValue.value == null){
          showMessage = false
        }
      }
      if(!showMessage){
        ElMessage({message: `请先${selectTitle.value}!`,type: 'error',})
        return 
      }
      // 【新增】确保点击新对话时关闭侧边栏
      isMenuExpanded.value = false;
      
      if (!inputValue.value.trim()) return;
      const newMsg = {
        messageArray:[{
          isCode:false,
          text: inputValue.value,
          uuid:uuidv4()
        }],
        isUser: true,
        avatarColor: '#67C23A',
        timestamp: new Date(),
        streamContent: '',
        isStreaming: false,
        conversation_id:'',
        uuid:uuidv4(),
      };
      messages.value.push(newMsg);
      inputValue.value = '';
      nextTick(() => {
        scrollToBottom();
      });
      // 调用流式响应函数
      fetchStreamData(newMsg.messageArray[0].text);
    };

    // 回车处理
    const handleEnter = (e) => {
      if (e.shiftKey) return;
      e.preventDefault();
      sendMessage();
    };

    // 滚动到底部
    const scrollToBottom = () => {
      // 当检测到手动滚动（包括触摸滚动）时，停止自动滚动
      if (doubleScrollDetected.value || isTouchScrolling.value) {
        return false;
      }
      if(  doubleScrollDetected.value ){
        return false
      }
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
    //监听向上滚动事件
    const scrollCount = ref(0);
    const lastScrollTime = ref(0);
    const doubleScrollDetected = ref(false);
    const MAX_INTERVAL = 300;
    const handleWheel = (e)=>{
        // 只检测向上滚动
      if( doubleScrollDetected.value){
        return false
      }
       if (e.deltaY < 0) {
        const currentTime = Date.now();
        // 检查是否在有效时间范围内连续滚动
        if (currentTime - lastScrollTime.value < MAX_INTERVAL && lastScrollTime.value !== 0) {
          // 检测到连续两次向上滚动
          doubleScrollDetected.value = true;
          scrollCount.value = 0;
          lastScrollTime.value = 0
      
        } else {
          // 不是连续滚动，更新计数和时间
          scrollCount.value++;
          doubleScrollDetected.value = false;
        }
        
        // 更新最后滚动时间
        lastScrollTime.value = currentTime;
      }
    };
    //手机端监听上拉事件
    const touchStartY = ref(0);
    const touchMoveY = ref(0);
    const isTouchScrolling = ref(false);
    // 处理触摸开始
    const handleTouchStart = (e) => {
      touchStartY.value = e.touches[0].clientY;
      isTouchScrolling.value = false;
    };
    // 处理触摸移动
    const handleTouchMove = (e) => {
      if (!chatContent.value) return;
      
      touchMoveY.value = e.touches[0].clientY;
      isTouchScrolling.value = true;
      
      // 计算滚动方向（上拉为正）
      const scrollDiff = touchStartY.value - touchMoveY.value;
      
      // 上拉操作且未滚动到顶部时，阻止自动滚动
      if (scrollDiff > 0 && chatContent.value.scrollTop > 0) {
        doubleScrollDetected.value = true;
      }
    };
    // 处理触摸结束
    const handleTouchEnd = () => {
      // 触摸结束后延迟恢复自动滚动
      setTimeout(() => {
        if (!isTouchScrolling.value) {
          doubleScrollDetected.value = false;
        }
      }, 500);
    };

    // Markdown 渲染
    const renderMarkdown = (msg) => {
      if(msg.text==''){
        return 
      }
      if(msg.text== '```'){
        return 
      }
      // ========== 新增：清理追问问题标签 ==========
      let cleanText = msg.text
        .replace(/<suggested_start>[\s\S]*?<suggested_end>/g, '') // 移除标签及内容
        .trim();
      // ===========================================
      // 自定义图表渲染规则
      const renderer = new marked.Renderer();
      renderer.code = (code) => {
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
          const matches = code.raw.match(regex)
          if(matches&&matches.length>1){
            try {
              //清理可能的转义字符
              const cleanedCode = JSON.stringify(code.text).replace(/\\n/g, '\n')
                                      .replace(/\\"/g, '"')
                                      .replace(/^"|"$/g, '');
              // 生成唯一容器ID
              const chartId = `chart-${msg.uuid}`;
              nextTick(() => {
                let jishqi = null
                initstart(jishqi,chartId,JSON.parse(cleanedCode));
              });

              return `<div  class="chart-container" style="height:550px;background:#fff">
                <div class='chart-container-title' style="height:40px;background:#c8ceda40;font-size:16px;padding-left:20px;font-weight:600;line-height:40px">ECHARTS</div>
                <div id="${chartId}" class="chart-container"  style="height:500px"></div>
              </div>`;
            } catch (error) {
              return `<div class="chart-error">图表配置解析错误：${error.message}</div>`;
            }
          }else{
            return `<div  class="chart-container" style="height:500px;background:#fff">
                <div class='chart-container-title' style="height:40px;background:#c8ceda40;font-size:16px;padding-left:20px;font-weight:600;line-height:40px">图表加载中...</div>
                <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 440px; background-color: #f0f0f0; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                  <div style="position: relative; width: 60px; height: 60px;">
                    <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 4px solid #d0d0d0; border-radius: 50%;"></div>
                    <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 4px solid transparent; border-top-color: #3b82f6; border-radius: 50%; animation: spin 1.2s linear infinite;"></div>
                  </div>
                </div>
              </div>`
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
      const html = marked.parse(cleanText, {
        breaks: true,
        gfm: true,
        renderer
      });
      return DOMPurify.sanitize(html);
    };
    // 历史对话
    const historyList = ref([]);
    const currentHistory = ref(0);
    const fullscreenLoading = ref(false)
    const showLoad = ref(false)
    const selectHistory = (index) => {
      // 1. 【新增】重置分享/选择状态
      showSelect.value = false;
      selectMessage.value = [];
      //2. 【新增】重置引导问题状态
      guideQuestions.value = [];

      //3 【新增】点击瞬间立即关闭抽屉，提升体验
      isMenuExpanded.value = false; 
      

      currentHistory.value = index + 1
      let id = historyList.value[index].id
      messages.value = []
      conversation_id.value = historyList.value[index].id
      Httploading.value = true
      axios.get("/messages?conversation_id="+id).then(res=>{
        showNewchat.value = false
        Httploading.value = false
        res.data.messageVos.forEach(mes=>{
           let userMeg = {
            messageArray:[
              {
                isCode:false,
                text:mes.query,
                uuid:uuidv4()
              }],
            isUser: true,
            avatarColor: '#67C23A',
            timestamp: new Date(),
            isStreaming: false,
            conversation_id:mes.conversation_id,
            messageId:mes.id,
            uuid:uuidv4(),
          }
          messages.value.push(userMeg)
          if(mes.answer&&mes.answer!=''&&mes.answer!=undefined){
            
            // ===== 重点优化：剥离历史对话里的 Transition 提示词 =====
            let allMessageRaw = (mes.answer || '').replace(/Transition:(.*?)(?=\n|$)\n?/g, '');
            // =======================================================
            
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
              uuid: uuidv4(),
              thoughtCollapsed: true, // 历史记录默认折叠
              hasFinishedThinking: true
            }));
  
            let aiMeg = {
              messageArray:[...messageInfo],
              transitions: [], // 历史对话清空或不传递 transitions
              isUser: false,
              isLoading:false,
              avatarColor: '#409EFF',
              timestamp: new Date(),
              isStreaming: false,
              conversation_id:mes.conversation_id,
              messageId:mes.id,
              uuid:uuidv4(),
              showAudio:true,
             
            }
            messages.value.push(aiMeg)
          }
        })

        //取消手机弹框
        showHistoryDrawer.value = false
        nextTick(() => {
          scrollToBottom();
        });
      }).catch(e=>{
        Httploading.value = false
      })
     
    };

    const buzy = ref(true)
    //加载更多历史对话
    const loadHistory = ()=>{
      if(!buzy.value){
        return false
      }
      buzy.value = false
      if(showLoad.value){
        ElMessage.error('已无更多对话历史!')
        return false
      }
      fullscreenLoading.value = true
      let id = ''
      try {
        id = historyList.value[historyList.value.length -1].id
      } catch (error) {
        id = ''
      }
      Httploading.value = true
      if(id == ''){
        axios.get('/conversations').then(res=>{
          Httploading.value = false
          if(res.data.conversations.length<20){
            historyList.value  = [...historyList.value,...res.data.conversations]
            fullscreenLoading.value = false 
            showLoad.value = true
          }else{
            buzy.value = true
            historyList.value  = [...historyList.value,...res.data.conversations]
            fullscreenLoading.value = false 
          }
        
        }).catch(e=>{
          Httploading.value = false
        })
      }else{
         axios.get('/conversations?last_id='+id).then(res=>{
          Httploading.value = false
          if(res.data.conversations.length<20){
            historyList.value  = [...historyList.value,...res.data.conversations]
            fullscreenLoading.value = false 
            showLoad.value = true
            ElMessage.error('已无更多对话历史!')
          }else{
            historyList.value  = [...historyList.value,...res.data.conversations]
            fullscreenLoading.value = false 
          }
        
        }).catch(e=>{
          Httploading.value = false
        })
      }
     
    }

    // 时间格式化
    const formatTime = (time) => {
      const date = new Date(time);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };
    // 移动端交互
    const createNewChat = async () => {
      //初始化对话信息
      showNewchat.value = true
      clickStop.value = true
      showStop.value = false
      task_id.value = ''
      inputDisable.value = false
      inputValue.value = ''

      conversation_id.value = ''
      currentHistory.value = 0
      messages.value = [];
      guideQuestions.value = []
    };
    const openCreateProject = () => alert('创建项目功能');
    const openProfile = () => alert('个人中心');

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
      console.log(option)
      const container = document.getElementById(containerId);
      if (!container) return;
      
      // 销毁旧实例
      if (container._echarts_instance) {
        echarts.getInstanceByDom(container)?.dispose();
      }
      //兼容一下横坐标 
      const xAxisData = option.xAxis?.data || [];
      const hasLongLabel = xAxisData.some(label => typeof label === 'string' && label.length > 10);
      // 如果有长标签，调整容器和父容器高度
      if (hasLongLabel) {
        // 调整图表容器高度为 800px
        container.style.height = '800px';
        // 调整父容器高度为 850px（如果父容器存在）
        const parentContainer = container.parentNode;
        if (parentContainer) {
          parentContainer.style.height = '850px';
        }
      }
   
      const chart =  echarts.init(container);
      // 增强默认配置
      const baseOptions = {
        animation: false, // 关闭全局默认动画
        color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
          textStyle: {
          fontFamily: 'inherit',
          fontSize: 14
        },
        title:{
          textStyle: {
            color: '#000',        // 主标题颜色
            fontSize: 18,         // 主标题字体大小
            fontWeight: 'bold',   // 主标题字体粗细
            fontFamily: 'Arial'   // 主标题字体
          },
          left:11,
          top:11,
        },
        grid: {
          containLabel: true,
          left: '3%',
          right: '4%',
          bottom: '12%',
          top: '15%'
        },
        xAxis: {
          axisLabel: {
            interval: 0, // 显示所有标签
            rotate: 45, // 旋转45度以避免重叠
            formatter: function(value) {
              // 如果标签超过5个字符，显示前5个字符加省略号
              return value.length > 20 ? value.substring(0, 20) + '...' : value;
            },
            tooltip: {
              show: true // 鼠标悬停时显示完整内容
            }
          }
        },
        yAxis: {
          axisLabel: {
            formatter: function(value) {
              // Y轴标签也做同样处理
              if (typeof value === 'string' && value.length > 5) {
                return value.substring(0, 10) + '...';
              }
              return value;
            }
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: function(params) {
            // 确保 tooltip 中显示完整的类别名称
            if (Array.isArray(params) && params.length > 0) {
              let result = params[0].name + '<br/>';
              params.forEach(param => {
                result += param.marker + ' ' + param.seriesName + ': ' + param.value + '<br/>';
              });
              return result;
            }
            return params.name + '<br/>' + params.marker + ' ' + params.seriesName + ': ' + params.value;
          }
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

      // 1. 获取图表类型（检查第一个 series 的类型）
        const isPie = option.series && 
                      (Array.isArray(option.series) ? option.series[0].type === 'pie' : option.series.type === 'pie');

        // 2. 如果是饼图，强制隐藏 baseOptions 中的坐标轴
        if (isPie) {
         
          baseOptions.xAxis = { show: false };
          baseOptions.yAxis = { show: false };
          baseOptions.grid = { show: false }; // 也可以隐藏网格区域
          baseOptions.tooltip = {  trigger: 'item'}
        } else {
          // 如果是柱状图或折线图，恢复默认坐标轴配置（确保 baseOptions 逻辑正常）
          baseOptions.xAxis = { 
            show: true,
            axisLabel: { /* 您原来的配置 */ } 
          };
          baseOptions.yAxis = { 
            show: true,
            axisLabel: { /* 您原来的配置 */ } 
          };
        }
      // 合并配置
      const finalOption = buildChartOption(option, { mobile: window.innerWidth < 768 });
      // 设置图表
      chart.setOption(finalOption);

      // 设置自适应
      // const resizeHandler = () => chart.resize();
      // window.addEventListener('resize', resizeHandler);


      // 错误处理
      chart.on('error', (error) => {
        console.error('图表渲染错误:', error);
        container.innerHTML = `<div class="chart-error">图表渲染失败：${error.message}</div>`;
      });
    };
    // 调整输入框高度
    const adjustTextareaHeight = () => {
      if (textarea.value) {
        textarea.value.style.height = 'auto';
        const height = Math.min(textarea.value.scrollHeight, 120);
        textarea.value.style.height = `${height}px`;
        nextTick(adjustInputPosition);
      }
    };

    watch(inputValue, adjustTextareaHeight);
   
    const copyMessageCLCIK = (array,copyLink)=>{
      let text = ''
      array.forEach(item=>{
        text = text+item.text
      })
      copyMessage(text,'')
    }
    //分享复制都在下方
    const copyMessage = (content,copyLink) =>{
      let infoText = '内容已复制!'
      if(copyLink == '复制链接'){
        infoText = '链接已复制!'
      }
      let cleanText = content
    
      // 兼容性写法
      const copyFallback = () => {
        const textarea = document.createElement('textarea');
        textarea.value = cleanText;
        textarea.style.position = 'fixed'; // 避免滚动条跳动
        document.body.appendChild(textarea);
        textarea.select();
        try {
          document.execCommand('copy');
          ElMessage({message: infoText,type: 'success',})
        } catch (err) {
          ElMessage({message: '复制失败，请手动复制!',type: 'error',})
        } finally {
          document.body.removeChild(textarea);
        }
      };
      // 优先使用现代API
      if (navigator.clipboard) {
        navigator.clipboard.writeText(cleanText)
        .then(() => ElMessage({message: infoText,type: 'success',}))
        .catch(() => copyFallback()); // 现代API失败时降级
      } else {
        copyFallback();
      }
    }
    //停止方法
    const stopMessage = () =>{
      try {
        if (audioPlayingAny.value&&audioState.value==='1') {
          // 停止所有音频播放
          Object.keys(audioInstances.value).forEach(messageId => {
            pauseAudio(messageId);
          });
          ElMessage.success('已停止所有音频播放');
        }
      } catch (error) {
        
      }
      
      stopstate.value = true
      currentStreamController?.abort()
      axios.post('/stop',{task_id:task_id.value}).then(res=>{
        showStop.value = false
        inputDisable.value = false
        task_id.value = ''
        ElMessage({
          message: '已经成功停止当前对话!',
          type: 'error',
        })
      
      })
    }
    //分享方法
    const showSelectClick = (index)=>{
       // 1. 先清空之前的选中项，防止叠加
      selectMessage.value = [] 
      
      // 2. 【关键修改】强制重置全选按钮状态为未选中
      checkallvalue.value = false 

      // 3. 添加当前点击的消息和上一条消息（通常是一问一答）
      selectMessage.value.push(index)
      // 加个判断防止数组越界（比如第一条就是AI回答的情况）
      if(index - 1 >= 0){
        selectMessage.value.push(index-1)
      }
      
      showSelect.value = true
    }
    const shareClick = ()=>{
      if(selectMessage.value.length<1){
        ElMessage.error('请先选中您要分享的对话信息!')
        return false
      }
      let id = []
      selectMessage.value.forEach(item=>{
        if(messages.value[item].messageId&&messages.value[item].messageId!=''&&messages.value[item].messageId!=null){
          id.push(messages.value[item].messageId)
        }
        
      })
      id = [...new Set(id)]
      Httploading.value = true
      axios.post('/share',{messageIds:[...id]}).then(res=>{
        Httploading.value = false
        showSelect.value = false 
        selectMessage.value = []
        // 【关键修改】分享成功后重置全选状态
        checkallvalue.value = false
        const link =  window.location.origin+'/#/share?share_code='+res.data
        copyMessage(link,'复制链接')
      }).catch(e=>{
          Httploading.value = false
        })
    }
    const cancelSelect = ()=>{
      showSelect.value = false
      selectMessage.value = []
      // 【关键修改】重置全选状态
      checkallvalue.value = false 
    }
    const changeCheck = (msg,index)=>{
    //等于-1是取消 等于1是增加
    if(selectMessage.value.indexOf(index)<0){
      if(!msg.isUser){
        if(index - 1>=0){
          const shju = selectMessage.value.indexOf(index - 1);
          selectMessage.value.splice(shju, 1);
        }
      }else{
        if(messages.value.length>index){
          const shju = selectMessage.value.indexOf(index + 1);
          selectMessage.value.splice(shju, 1);
        }
      }
    }else{
      if(!msg.isUser){
        if(index - 1>=0){
          if(selectMessage.value.indexOf(index-1)<0){
            if(selectMessage.value.indexOf(index-1)<0){
              selectMessage.value.push(index-1)
            }
          }
        }
      }else{
        if(messages.value.length>index){
          if(selectMessage.value.indexOf(index+1)<0){
            selectMessage.value.push(index+1)
          }
        }
      }
    }
    const totalNeedSelect = messages.value.length;
    checkallvalue.value = selectMessage.value.length === totalNeedSelect;
    }
    //选择方法
    const checkallvalue = ref(false)
    const checkall = (value)=>{
      console.log(value)
      if(value){
        messages.value.forEach((item,index)=>{
          if(selectMessage.value.indexOf(index)<0){
            selectMessage.value.push(index)
          }
        })
      }else{
        console.log('false')
        selectMessage.value = []
      }
  
    }
    const handleLoginExpired = ()=>{
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
          localStorage.removeItem('usern')
          localStorage.removeItem('menu')
          // 跳转到登录页，记录当前路径以便登录后返回
          router.replace({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          })
        }).finally(() => {
          window.isLoginExpiredDialogShow = false
        })
    }
    // 初始化
    const isMenuExpanded = ref(false); // 控制菜单展开/收起
    const toggleMenuExpand = () => {
      isMenuExpanded.value = !isMenuExpanded.value;
    };
    const loginmodel = ref('') 
    const menuButton =ref([]) 
    const audioFlag = ref('')
    const thirdLogin = ref('')

    onMounted(async () => {
      try {
        const response = await axios.get('/globalConfig');
        const globalConfig = response.data;
        const aFlag = globalConfig.data.audioFlag + ''
        window.localStorage.setItem('audioFlag', aFlag);
      } catch (error) {
        
      }
   
      showNewchat.value = true
      loginmodel.value =  window.localStorage.getItem('loginMode')
      thirdLogin.value =  window.localStorage.getItem('thirdLogin')
      try {
        //自定义菜单是否存在
        menuButton.value = JSON.parse( window.localStorage.getItem('menu'))
        //音频能否播放判断
        audioFlag.value =  window.localStorage.getItem('audioFlag')
        if(audioFlag.value&&audioFlag.value!=1){
          audioState.value = '2'
        }
        // 添加移动端触摸事件监听
        if (chatContent.value) {
          chatContent.value.addEventListener('touchstart', handleTouchStart);
          chatContent.value.addEventListener('touchmove', handleTouchMove);
          chatContent.value.addEventListener('touchend', handleTouchEnd);
        }
      } catch (error) {
        menuButton.value = []
      }

      window.addEventListener('resize', adjustInputPosition);
      username.value = window.localStorage.getItem('user')
      if( window.localStorage.getItem('loginMode')=='noLogin'){
        try {
          const token = await axios.post('/autoLogin')
          window.localStorage.setItem('_token',token.data._token)
        } catch (error) {
          const token = await axios.post('/autoLogin')
          window.localStorage.setItem('_token',token.data._token)
        }
      }
   
      adjustTextareaHeight();
      nextTick(adjustInputPosition);
      //获取历史对话列表
      if(!isMobileFc()){
        axios.get('/conversations').then(res=>{
          historyList.value  = res.data.conversations
        })
      }

      //获取预设对话列表
      axios.get('/parameters').then(res=>{
        presetQuestions.value = res.data.suggested_questions
        title.value = res.data.opening_statement
        showTitle.value = res.data.name
        showTitledescribe.value = res.data.chatdescribe
        if(res.data.inputForms&&res.data.inputForms.length&&res.data.inputForms.length>0){
          
          res.data.inputForms[0].options.forEach(item=>{
            let obj = {
              value :item,
              label:item
            }
            options.value.push(obj)
          })
          selectrequired.value = res.data.inputForms[0].required
          selectTitle.value =  res.data.inputForms[0].label
          selectOption.value = res.data.inputForms[0].variable
        }
      })

     
      // 初始化音频状态对象
      // 新增：监听用户首次交互，解锁自动播放
      const unlockAudioPlay = async () => {
        // 创建空音频实例触发交互
        const tempAudio = new Audio();
        try {
          await tempAudio.play();
        } catch (e) {
          // 静默失败
        }
        document.removeEventListener('click', unlockAudioPlay);
        document.removeEventListener('touchstart', unlockAudioPlay);
      };
      document.addEventListener('click', unlockAudioPlay);
      document.addEventListener('touchstart', unlockAudioPlay);
    });

    // 组件卸载
    onUnmounted(() => {
      window.removeEventListener('resize', adjustInputPosition);
      try {
        //音频监听事件移除
        if (audioInstances.value[messageId]) {
          audioInstances.value[messageId].pause();
          audioInstances.value[messageId].currentTime = 0;
          audioPlaying.value[messageId] = false;
        }
        // 移除触摸事件监听
        if (chatContent.value) {
          chatContent.value.removeEventListener('touchstart', handleTouchStart);
          chatContent.value.removeEventListener('touchmove', handleTouchMove);
          chatContent.value.removeEventListener('touchend', handleTouchEnd);
        }
      } catch (e) {
        // 静默失败
      }
     
    });
    //退出
    const outLogin = ()=>{
      ElMessageBox.confirm(
          '是否要退出当前登录账号',
          '登录退出提醒',
          {
           
            closeOnClickModal: false,
            showClose: false,
            confirmButtonText: '退出',
            cancelButtonText: '取消',
            closeOnPressEscape: false,
            type: 'warning'
          }
        ).then(() => {
          axios.post('/logout').then(res=>{
            localStorage.removeItem('_token')
            localStorage.removeItem('usern')
            localStorage.removeItem('menu')
            router.replace({
              path: '/login',
              query: { redirect: router.currentRoute.value.fullPath }
            })
          })
        }).finally(() => {
          
        })
    
    }

    const isMobileFc = ()=> {
      const userAgent = navigator.userAgent.toLowerCase();
      return /mobile|android|iphone|ipad|ipod/i.test(userAgent);
    }
      // 鼠标进入事件
    const handleMouseEnter = (index) => {
      historyList.value[index].hovered = true;
    };
    
    // 鼠标离开事件
    const handleMouseLeave = (index) => {
      historyList.value[index].hovered = false;
      historyList.value[index].showPopover = false;
    };
      // 重命名对话
    const renameHistory = (index) => {
     
      const history = historyList.value[index];
      ElMessageBox.prompt('', '重命名对话', {
        inputValue: history.name,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value.trim()) {
            return '对话名称不能为空';
          }
        }
      }).then(({ value }) => {
        history.name = value;
        axios.post('/renameConversation',{conversation_id:history.id,name:value}).then(res=>{
          ElMessage.success('重命名成功');
        })
        
      }).catch(() => {
        // 取消操作
      });
    };
    // 删除对话
    const deleteHistory = (index) => {
      const history = historyList.value[index];
      ElMessageBox.confirm(
        `确定要删除对话"${history.name}"吗？`,
        '删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerMode: true
        }
      ).then(() => {
        axios.post('/deleteConversation',{conversation_id:history.id}).then(res=>{
          historyList.value.splice(index, 1);
          if (currentHistory.value === (index+1) && historyList.value.length > 1) {
            ElMessage.success('对话已删除');
            selectHistory(0)
          }else{
            ElMessage.success('对话已删除');
          }
        })
      }).catch(() => {
        // 取消操作
      });
    };
    //打开文件上传弹框
    const modelState = ref('')
    const modelTitle = ref('')
    const showFileUploadclick = (str,title)=>{
      if(title.indexOf('价格查询')>=0){
        router.push({
          path: '/queryData',
          query:{
            titlevalue:title,
            modelState:str
          }
        })
        return false
      }
      modelTitle.value = title
      modelState.value = str
      showFileUpload.value = true
    }
    const handleDialogClose = (val)=>{
      showFileUpload.value = val
    }

    const handleVoiceResult = (text) => {
       inputValue.value = text;
       sendMessage();
    };

    // 音频相关
    const audioInstances = ref({}); // 存储每个messageId的音频实例 {messageId: Audio}
    const audioLoading = ref({});   // 音频加载状态 {messageId: boolean}
    const audioPlaying = ref({});   // 音频播放状态 {messageId: boolean}
    const audioUrls = ref({});      // 缓存音频链接 {messageId: url}
    const audioError = ref({});     // 音频错误信息 {messageId: string}
    const audioState = ref('1')     //自动播放状态 2为不自动播放 1为自动播放

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

    // 新增：判断是否有任何音频在播放
    const audioPlayingAny = computed(() => {
      return Object.values(audioPlaying.value).some(Boolean);
    });

    // 新增：切换所有音频播放/停止状态
    const toggleAllAudio = () => {
      if (audioPlayingAny.value&&audioState.value==='1') {
        audioState.value = '2'
        // 停止所有音频播放
        Object.keys(audioInstances.value).forEach(messageId => {
          pauseAudio(messageId);
        });
        ElMessage.success('已停止所有音频播放');
      } else {
         audioState.value = '1'
        // 自动播放最新的音频
        const latestAudioId = Object.keys(audioInstances.value).pop();
        if (latestAudioId) {
          playAudio(latestAudioId);
          ElMessage.success('已开始自动播放音频');
        } else {
          if(!isMobile.value){
            ElMessage.warning('暂无可播放的音频');
          }
          
        }
      }
    };
    // 新增：手机端切换所有音频播放/停止状态
    const mobileToggleAllAudio=()=>{
      if(audioState.value==='1'){
        audioState.value = '2'
        if (audioPlayingAny.value){
          // 停止所有音频播放
          Object.keys(audioInstances.value).forEach(messageId => {
            pauseAudio(messageId);
          });
          ElMessage.success('已停止所有音频播放');
        }
      }else{
         audioState.value = '1'
         // ===== 判断当前是否有流式输出 存在就播放当前的流式输出音频 没有则不播放 =====
        try{
          if(messages.value[messages.value.length-1].isStreaming){
            if (!messages.value[messages.value.length-1].audioFetched) {
              playAudio(newData.message_id);
              messages.value[messages.value.length-1].audioFetched = true; // 标记为已请求
            }else{
              // 自动播放最新的音频
              const latestAudioId = Object.keys(audioInstances.value).pop();
              if (latestAudioId) {
                playAudio(latestAudioId);
                ElMessage.success('已开始自动播放音频');
              } 
            }
          }
        }catch(e){
          console.log(e)
        }
        
      }
    }

    //新增切换路由缓存状态
    onBeforeRouteLeave((to, from, next) => {
      // 如果去往的目标路径是 /queryData
      if (to.path === '/queryData') {
        // 将当前页面 (home) 标记为需要缓存
        from.meta.keepAlive = true
      } else {
        // 如果去往其他页面（比如退出登录去 login，或者去 share），不缓存
        from.meta.keepAlive = false
      }
      // 继续跳转
      next()
    })
    // ==========================================
    // 生成长图分享功能
    // ==========================================
  const generateImage = async () => {
    if (selectMessage.value.length < 1) {
      ElMessage.error('请先选中您要分享的对话信息!');
      return false;
    }

    Httploading.value = true;

    try {
      const messageElements = document.querySelectorAll('.message-item');
      const sortedIndices = [...selectMessage.value].sort((a, b) => a - b);
      const selectedElements = sortedIndices
        .map((index) => messageElements[index])
        .filter(Boolean);
      const selectedWidth = selectedElements.reduce((maxWidth, element) => {
        const rect = element.getBoundingClientRect();
        return Math.max(maxWidth, rect.width || element.offsetWidth || 0);
      }, 0);
      const fallbackWidth = window.innerWidth < 768 ? Math.min(window.innerWidth, 430) : 980;
      const contentWidth = Math.round(Math.min(Math.max(selectedWidth || fallbackWidth, 360), 1200));
      const wrapperPaddingX = window.innerWidth < 768 ? 12 : 20;
      const captureWidth = contentWidth + wrapperPaddingX * 2;
      
      // 1. 创建内存中的截图容器
      const captureWrapper = document.createElement('div');
      Object.assign(captureWrapper.style, {
        position: 'absolute',
        left: '-9999px',
        top: '0',
        width: `${captureWidth}px`,
        backgroundColor: '#f5f7fa', // 轻微底色增加质感
        padding: `40px ${wrapperPaddingX}px`,
        display: 'flex',
        flexDirection: 'column',
        boxSizing: 'border-box'
      });

      // 2. 插入美化的顶部标题栏
      const header = document.createElement('div');
  header.style.cssText = `
        text-align: center;
        margin-bottom: 50px;
        padding-bottom: 15px;
        border-bottom: 1px solid #f0f0f0; /* 极细分割线 */
      `;
      const title = showTitle.value || '对话记录';
      header.innerHTML = `
        <div style="font-size: 20px; color: #333; font-weight: 500; letter-spacing: 1px;">${title}</div>
        <div style="font-size: 12px; color: #999; margin-top: 8px; font-family: sans-serif;">${new Date().toLocaleString()}</div>
      `;
      captureWrapper.appendChild(header);

      // 3. 按照选择顺序处理消息
      const chartImagePromises = [];
      
      for (const index of sortedIndices) {
        const originalItem = messageElements[index];
        if (!originalItem) continue;

        // 克隆消息块
        const clone = originalItem.cloneNode(true);
        clone.classList.remove('showSelect');
        
        // --- 布局优化：判断用户身份并设置右侧留白 ---
        // 这里根据你的代码逻辑判断是否为用户（通常类名包含 user 或判断 role）
        const isUser = originalItem.classList.contains('user-msg') || originalItem.classList.contains('user-item') || originalItem.querySelector('.user-avatar');
        
        Object.assign(clone.style, {
          width: '100%',
          marginBottom: '20px',
          opacity: '1',
          display: 'flex',
          flexDirection: 'column',
          position: 'relative',
          transform: 'none',
          animation: 'none'
        });

        if (isUser) {
          // 【关键点】给用户对话右侧留白 (留出 15% 的空间)
          clone.style.alignItems = 'flex-end';
          clone.style.paddingRight = '0px'; 
          clone.style.paddingLeft = '110px';
          const userBox = clone.querySelector('.text-box');
          if (userBox) {
            userBox.style.width = 'max-content';
            userBox.style.maxWidth = '100%';
            userBox.style.alignSelf = 'flex-end';
          }
        } else {
          // 助手对话左右平衡，或左侧留白小一点
          clone.style.alignItems = 'stretch';
          clone.style.paddingRight = '20px';
          clone.style.paddingLeft = '0px';
        }

        // --- 关键优化：处理图表压盖 ---
        const originalCharts = originalItem.querySelectorAll('canvas');
        const clonedChartsPlaceholders = clone.querySelectorAll('.chart-container'); // 确保这里的类名与你模板一致

        originalCharts.forEach((origCanvas) => {
          // 1. 找到当前 Canvas 所属的图表容器 ID
          const chartContainer = origCanvas.closest('[id^="chart-"]');
          if (!chartContainer) return;
          const chartId = chartContainer.id;

          // 2. 在克隆出的节点中，通过 ID 精确查找对应的位置
          const target = clone.querySelector(`#${chartId}`);
          
          if (target) {
            // 获取原图表实际尺寸
            const realHeight = origCanvas.offsetHeight;
            const realWidth = origCanvas.offsetWidth;
            
            // Canvas 转图片
            const img = new Image();
            img.src = origCanvas.toDataURL('image/png');
            img.style.width = '100%';
            img.style.display = 'block';

            // 填充内容并强制锁定高度，防止 html2canvas 渲染时容器塌陷
            target.innerHTML = ''; 
            target.style.height = `${realHeight}px`;
            target.style.minHeight = `${realHeight}px`;
            target.style.width = `${realWidth}px`;
            target.style.overflow = 'hidden';
            target.appendChild(img);
          }
        });

        // 移除不必要的交互 UI（如复制按钮、复选框、操作栏）
        const originalChartBlocks = originalItem.querySelectorAll('.chart-block');
        const clonedChartBlocks = clone.querySelectorAll('.chart-block');

        originalChartBlocks.forEach((originalChartBlock, chartIndex) => {
          const origCanvas = originalChartBlock.querySelector('canvas');
          const clonedChartBlock = clonedChartBlocks[chartIndex];
          const target = clonedChartBlock?.querySelector('.chart-canvas') || clonedChartBlock;
          if (!origCanvas || !target) return;

          const canvasRect = origCanvas.getBoundingClientRect();
          const realWidth = Math.max(origCanvas.offsetWidth, canvasRect.width, 1);
          const realHeight = Math.max(origCanvas.offsetHeight, canvasRect.height, 420);
          const aspectRatio = realWidth / realHeight;
          const img = new Image();
          img.src = origCanvas.toDataURL('image/png');
          img.style.width = '100%';
          img.style.height = 'auto';
          img.style.aspectRatio = `${aspectRatio}`;
          img.style.objectFit = 'contain';
          img.style.display = 'block';
          img.style.background = '#ffffff';

          target.innerHTML = '';
          target.style.height = `${realHeight}px`;
          target.style.minHeight = `${realHeight}px`;
          target.style.width = '100%';
          target.style.maxWidth = '100%';
          target.style.overflow = 'hidden';
          target.style.background = '#ffffff';
          target.style.display = 'flex';
          target.style.alignItems = 'center';
          target.style.justifyContent = 'center';
          target.appendChild(img);

          chartImagePromises.push(new Promise((resolve) => {
            img.onload = resolve;
            img.onerror = resolve;
            if (img.complete) resolve();
          }));
        });

        const ignoreElements = clone.querySelectorAll('.selectBox, .options, .copy-btn, .operations, .el-checkbox');
        ignoreElements.forEach(el => el.remove());

        captureWrapper.appendChild(clone);
      }

      document.body.appendChild(captureWrapper);

      // 4. 等待图片和样式计算
      await Promise.all(chartImagePromises);
      await new Promise(resolve => setTimeout(resolve, 800));

      // 5. 执行截图
      const canvas = await html2canvas(captureWrapper, {
        scale: 2, // 清晰度
        useCORS: true,
        backgroundColor: '#f5f7fa',
        scrollY: 0,
        width: captureWidth,
        windowWidth: captureWidth,
        onclone: (clonedDoc) => {
          // 确保克隆后的 DOM 中所有图片已显示
          const imgs = clonedDoc.querySelectorAll('img');
          imgs.forEach(img => img.style.visibility = 'visible');
        }
      });

      // 6. 下载
      const imgUrl = canvas.toDataURL('image/jpeg', 0.92);
      const link = document.createElement('a');
      link.href = imgUrl;
      link.download = `ChatReport_${Date.now()}.jpg`;
      link.click();

    } catch (error) {
      console.error('截图生成失败:', error);
      ElMessage.error('图片生成失败，请重试');
    } finally {
      // 移除临时容器
      const temp = document.querySelector('[style*="-9999px"]');
      if (temp) temp.remove();
      Httploading.value = false;
    }
  };
    return {
      isMobile,
      showSidebar,
      toggleSidebar,
      showHistoryDrawer,
      openHistory,
      inputValue,
      isLoading,
      messages,
      sendMessage,
      handleEnter,
      renderMarkdown,
      historyList,
      currentHistory,
      selectHistory,
      formatTime,
      createNewChat,
      openCreateProject,
      openProfile,
      presetQuestions,
      sendPresetQuestion,
      title,
      copyMessage,
      copyMessageCLCIK,
      stopMessage,
      chatContent,
      inputContainer,
      contentWrapper,
      historySection,
      isMenuExpanded,
      toggleMenuExpand,
      selectMessage,
      showSelect,
      shareClick,
      cancelSelect,
      showSelectClick,
      fullscreenLoading,
      loadHistory,
      showStop,
      changeCheck,
      clickStop,
      inputDisable,
      Httploading,
      showNewchat,
      handleWheel,
      options,
      selectValue,
      selectTitle,
      showTitle,
      showTitledescribe,
      guideQuestions,
      handleMouseEnter,
      handleMouseLeave,
      deleteHistory,
      renameHistory,
      username,
      outLogin,
      loginmodel,
      showFileUpload,
      showFileUploadclick,
      menuButton,
      handleDialogClose,
      modelState,
      modelTitle,
      checkall,
      checkallvalue,
      handleVoiceResult,
  
      generateImage, // 👈 新增导出
      //播放相关
      playAudio,
      pauseAudio,
      audioLoading,
      audioPlaying,
      audioError, 
      audioInstances,
      audioFlag,
      hasPlayingAudio,
      audioPlayingAny,
      toggleAllAudio,
      audioState,
      mobileToggleAllAudio,
      thirdLogin
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
  padding-top: calc(56px + 24px); /* 移动端导航高度 + 间距 */
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
  padding: 32px 0;
  position: relative;
  max-height: 100vh;
}
.chat-content {
  width: 100%;
  max-width: 1200px; 
  padding: 0 200px;
  overflow-y: auto;
  padding-bottom: 80px;

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

.message-item.showSelect {
  display: grid;
  align-items: center;
  column-gap: 8px;
}

.ai-msg.showSelect {
  grid-template-columns: 30px minmax(0, 1fr);
}

.user-msg.showSelect {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.message-item.showSelect > div:first-child {
  min-width: 0;
  grid-row: 1;
}

.ai-msg.showSelect > div:first-child {
  grid-column: 2;
}

.user-msg.showSelect > div:first-child {
  order: 2;
  min-width: 0;
}

.message-item.showSelect .selectBox {
  position: static;
  grid-row: 1;
  transform: none;
}

.ai-msg.showSelect .selectBox {
  grid-column: 1;
}

.user-msg.showSelect .selectBox {
  order: 1;
}

.message-item.showSelect .options {
  display: none;
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
  .showSelect{
    margin-left: 30px;
  }
  .message-item.showSelect {
    margin-left: 0;
  }
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
  .main-content {
    padding-top: 24px; /* 恢复桌面端默认间距 */
   
  }
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
    margin-top: 30px;
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
  .chat-content-wrapper{padding: 24px 0;}
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
    overflow-wrap: break-word; /* 确保长单词或连续字符换行 */
    position: relative;
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
    overflow-wrap: break-word; /* 确保长单词或连续字符换行 */
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
    overflow-wrap: break-word; /* 确保长单词或连续字符换行 */
  }
}

// ========== 有序列表：数字对齐到“第一行” ==========
ol {
  counter-reset: top-level !important;

  > li {
    counter-increment: top-level !important;

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

/* =========================================================
   移动端分享模式深度优化 (APP质感)
   ========================================================= */
@media (max-width: 800px) {

  /* 1. 布局核心修复：利用 padding 撑开上下空间，防止遮挡 */
  .main-content.share-mode-padding {
    padding-top: 0px !important;   
     /* 顶部留白，对应 Header 高度 */
    padding-bottom: 90px !important; /* 底部留白，对应 Footer 高度 */
    transition: all 0.3s ease;       
    margin-top: 80 !important;        /* 覆盖掉可能的 margin */
  }

  /* 2. 顶部导航：极简白底 + 居中标题 */
  .selete-header {
    position: fixed !important;
    top: 0;
    left: 0;
    width: 100%;
    height: 54px; /* 标准 iOS 导航栏高度 */
    background: rgba(255, 255, 255, 0.98); /* 纯净白底，微透 */
    backdrop-filter: blur(10px);
    border-bottom: 0.5px solid rgba(0, 0, 0, 0.05); /* 极细分割线 */
    z-index: 2000;
    padding: 0;
    
    .selete-content {
      width: 100% !important;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center; /* 标题绝对居中 */
      padding: 0 16px;
      position: relative;

      /* 标题样式 */
      .selete-title {
        font-size: 17px;
        font-weight: 600;
        color: #1c1c1e;
        letter-spacing: -0.3px;
      }

      /* 取消按钮 - 右侧纯文字 */
      .selete-button {
        position: absolute;
        right: 16px;
        top: 0;
        height: 100%;
        display: flex;
        align-items: center;
        font-size: 15px;
        color: #8E8E93; /* iOS 次级文字灰 */
        background: transparent;
        border: none;
        padding: 0 5px;
        font-weight: 400;
        
        &:active {
          opacity: 0.6;
        }
      }
    }
  }

  /* 3. 底部操作栏：悬浮通透感 + 强力引导按钮 */
  .selete-bottom {
    position: fixed !important;
    bottom: 0;
    left: 0;
    width: 100%;
    height: auto;
    background: rgba(255, 255, 255, 0.98);
    border-top: 0.5px solid rgba(0,0,0,0.05);
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.02);
    z-index: 2000;
    padding-bottom: env(safe-area-inset-bottom); /* 适配 iPhone 底部条 */
    
    .selete-bottom-content {
      width: 100% !important;
      height: 70px; /* 增加高度，操作更舒适 */
      padding: 0 24px; /* 增加左右内边距 */
      display: flex;
      align-items: center;
      justify-content: space-between;

      /* 左侧：全选复选框美化 */
      .el-checkbox {
        display: flex;
        align-items: center;
        height: 100%;
        margin-right: 0;
        
        /* 强制覆盖 Element Plus 样式为圆形 */
        :deep(.el-checkbox__inner) {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          border: 1.5px solid #C7C7CC;
          background-color: transparent;
          transition: all 0.2s;
          
          &::after {
            height: 10px;
            left: 9px;
            top: 9px;
            border-width: 2px;
          }
        }

        /* 选中状态 */
        :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
          background-color: #007AFF;
          border-color: #007AFF;
        }
        
        /* 文字标签 */
        :deep(.el-checkbox__label) {
          font-size: 15px;
          color: #1c1c1e;
          font-weight: 500;
          padding-left: 8px;
        }
      }

      /* 右侧：复制链接按钮 (胶囊样式) */
      .selete-button {
        background: #007AFF; /* 鲜艳的蓝色 */
        color: #fff;
        font-size: 15px;
        font-weight: 600;
        padding: 0 20px;
        height: 40px;
        min-width: 110px;
        line-height: 40px;
        border-radius: 20px; /* 胶囊圆角 */
        box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25); /* 柔和投影 */
        text-align: center;
        border: none;
        
        &:active {
          transform: scale(0.96);
          background: #0062cc;
        }
      }
    }
  }

  
}
/* 深度思考总容器 */
@media (max-width: 800px) {
  .main-content.share-mode-padding .chat-content {
    padding-left: 14px;
    padding-right: 10px;
  }

  .main-content.share-mode-padding .message-item.showSelect {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    column-gap: 0;
    overflow: visible;
  }

  .main-content.share-mode-padding .ai-msg.showSelect > div:first-child {
    width: 100%;
    min-width: 0;
    grid-column: auto;
  }

  .main-content.share-mode-padding .ai-msg.showSelect .selectBox {
    position: absolute;
    left: -2px;
    top: 50%;
    width: 24px;
    height: 24px;
    transform: translate(-50%, -50%);
    z-index: 3;
    background: rgba(255, 255, 255, 0.92);
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  }

  .main-content.share-mode-padding .user-msg.showSelect {
    flex-direction: row;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
  }

  .main-content.share-mode-padding .user-msg.showSelect > div:first-child {
    order: 2;
    width: max-content;
    max-width: calc(100% - 32px);
    min-width: 0;
  }

  .main-content.share-mode-padding .user-msg.showSelect .selectBox {
    position: static;
    order: 1;
    flex: 0 0 24px;
    width: 24px;
    height: 24px;
    transform: none;
    background: transparent;
    box-shadow: none;
  }

  .main-content.share-mode-padding .selectBox input {
    width: 16px;
    height: 16px;
  }
}

.thought-section {
  margin: -4px -4px 16px -4px; /* 稍微负边距让它在 text-box 内更紧凑 */
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.02); /* 极浅的底色 */
  border: 1px solid rgba(0, 0, 0, 0.04);
  overflow: hidden;
  margin-top: 16px;
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
    pre {
      max-width: 100%;
      overflow-x: auto; /* 代码过长时允许内部横向滚动，而不是撑开整个页面 */
      box-sizing: border-box;
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

/* =========================================================
   新增：智能查询流程 (Transition) 样式
   ========================================================= */
.transitions-wrapper {
  padding: 10px 14px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.05);
  border: 1px solid rgba(64, 158, 255, 0.1);
  font-size: 13px;
  color: #409EFF;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transition-step {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: -25px;
}

.transition-step .el-icon {
  font-size: 15px;
}

.success-icon {
  color: #67C23A; /* 完成呈现绿色 */
}

.mobile-top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 54px; 
  background: rgba(255, 255, 255, 0.85); /* 半透明底 */
  backdrop-filter: blur(10px); /* 苹果级毛玻璃效果 */
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06); /* 极细精致下边框 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  z-index: 1000;
  box-sizing: border-box;
}

/* 左侧布局 */
.left-section {
  display: flex;
  align-items: center;
  gap: 10px; /* 元素间距 */
}

/* 侧边栏按钮 */
.menu-btn {
  background: none;
  border: none;
  padding: 6px;
  color: #4c4d4f;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.menu-btn:active {
  background: rgba(0, 0, 0, 0.05);
}

/* 用户姓名+头像胶囊舱 */
.mobile-user-profile {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 4px;
  background: rgba(0, 0, 0, 0.03); 
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 20px; 
  max-width: 105px;
  transition: all 0.2s;
  margin-left: -10px; /* 轻微向左偏移，贴近边缘 */
  cursor: pointer;
}
.mobile-user-profile:active {
  transform: scale(0.97);
  background: rgba(0, 0, 0, 0.06);
}

.mobile-avatar {
  background: linear-gradient(135deg, #409eff, #007aff); /* 渐变蓝头像 */
  color: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(64, 159, 255, 0.15);
}

.mobile-username {
  font-size: 12px;
  font-weight: 500;
  color: #2f3033;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 长名字自动省略，防止撑爆导航栏 */
}

/* 右侧布局 */
.right-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* “新对话”药丸按钮 */
.new-chat-btn-mobile {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f0f2f5; 
  color: #303133;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  cursor: pointer;
}
.new-chat-btn-mobile:active {
  background: #e4e7ed;
  transform: scale(0.96);
}
.new-chat-btn-mobile .el-icon {
  font-size: 14px;
  color: #409eff; 
}

/* 音频控制按钮间距修正 */
.audio-control-btn {
  background: #f0f2f5;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s;
  cursor: pointer;
}
.audio-control-btn:active {
  transform: scale(0.95);
}
.audio-control-btn-danger {
  background: rgba(245, 108, 108, 0.1) !important;
  color: #f56c6c !important;
}

/* ==========================================================================
   ✨ 手机端顶部导航栏高级感整套样式（包含完美适配的语音按钮）
   ========================================================================== */
.mobile-top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 54px; 
  background: rgba(255, 255, 255, 0.85); /* 半透明底色 */
  backdrop-filter: blur(10px); /* 🌟 苹果级毛玻璃效果 */
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06); /* 极细精致下边框 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  z-index: 1000;
  box-sizing: border-box;
}

/* 左侧布局 */
.left-section {
  display: flex;
  align-items: center;
  gap: 10px; 
}

/* 侧边栏按钮 */
.menu-btn {
  background: none;
  border: none;
  padding: 6px;
  color: #4c4d4f;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.menu-btn:active {
  background: rgba(0, 0, 0, 0.05);
}

/* 用户姓名+头像胶囊舱 */
.mobile-user-profile {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 4px;
  background: rgba(0, 0, 0, 0.03); 
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 20px; 
  max-width: 105px;
  transition: all 0.2s;
  cursor: pointer;
}
.mobile-user-profile:active {
  transform: scale(0.97);
  background: rgba(0, 0, 0, 0.06);
}

.mobile-avatar {
  background: linear-gradient(135deg, #409eff, #007aff); 
  color: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(64, 159, 255, 0.15);
}

.mobile-username {
  font-size: 12px;
  font-weight: 500;
  color: #2f3033;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; 
}

/* 右侧布局 */
.right-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* “新对话”药丸按钮 */
.new-chat-btn-mobile {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f0f2f5; 
  color: #303133;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  cursor: pointer;
}
.new-chat-btn-mobile:active {
  background: #e4e7ed;
  transform: scale(0.96);
}
.new-chat-btn-mobile .el-icon {
  font-size: 14px;
  color: #409eff; 
}

/* 🌟 全新优化的移动端语音控制按钮（与头部组件完美契合） */
.mobile-audio-btn {
  background: #f0f2f5; /* 与新对话药丸颜色高度统一 */
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%; /* 正圆形按钮 */
  display: flex;
  align-items: center;
  justify-content: center;
  color: #505256;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  padding: 0;
}
.mobile-audio-btn .el-icon {
  font-size: 16px; /* 适当缩小图标尺寸，看起来更精致紧凑 */
}
.mobile-audio-btn:active {
  transform: scale(0.92); /* 灵敏的物理点击缩放手感 */
  background: #e4e7ed;
}

/* 🌟 语音关闭（静音）状态的质感警示色方案 */
.mobile-audio-btn-danger {
  background: rgba(245, 108, 108, 0.1) !important; /* 柔和的浅红晕底，不再是土气的纯红 */
  color: #f56c6c !important;
}
.mobile-audio-btn-danger:active {
  background: rgba(245, 108, 108, 0.18) !important;
}
</style>
