<template>
  <div class="voice-recorder-wrapper">
    <div
      class="voice-btn"
      :class="{ 
        'is-recording': isRecording,
        'no-microphone': hasMicrophone === false,
        'is-loading': isLoading
      }"
      @mousedown="handleRecordStart"
      @mouseup="handleRecordStop"
      @mouseleave="handleRecordStop"
      @touchstart="handleRecordStart"
      @touchend="handleRecordStop"
      @click="handleButtonClick"
    >
      <el-icon class="voice-icon">
        <Microphone v-if="hasMicrophone !== false" />
        <Warning v-else />
      </el-icon>
      <div class="loading" v-if="isLoading">
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
      </div>
      <div class="wave-container" v-if="isRecording">
        <div class="wave wave-1"></div>
        <div class="wave wave-2"></div>
      </div>
      <div class="bg-wave-container" v-if="isRecording">
        <div class="bg-wave bg-wave-1"></div>
        <div class="bg-wave bg-wave-2"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'


// 百度云 ASR 配置
const BAIDU_API_KEY = 'WNuIKuYDqzon1dz8Haps447p'
const BAIDU_SECRET_KEY = 'XoU3egEOBM3gCduVuuIBeBgdqviuxpwE'

// 定义一个自定义事件，用于向父组件传递结果
const emit = defineEmits(['result', 'error'])

// 录音状态
const isRecording = ref(false)
// 麦克风硬件/权限状态 (null: 未检测, true: 可用, false: 不可用)
const hasMicrophone = ref(null)
// 按钮加载状态
const isLoading = ref(false)
// 音频录制实例
let mediaRecorder = null
// 录制的音频片段
let audioChunks = []
// 百度云 Access Token
let accessToken = ''

// 用于防止在一个提示消失前弹出另一个提示
const isMessageBeingShown = ref(false)

// 封装一个带锁的消息提示函数
const showMessage = (options) => {
  if (isMessageBeingShown.value) {
    return
  }
  isMessageBeingShown.value = true
  ElMessage({
    ...options,
    onClose: () => {
      isMessageBeingShown.value = false
      if (options.onClose) {
        options.onClose()
      }
    }
  })
}

// 重置页面到初始状态
const resetToInitialState = () => {
  isRecording.value = false
  isLoading.value = false
  audioChunks = []
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    if (mediaRecorder.stream) {
      mediaRecorder.stream.getTracks().forEach(track => track.stop())
    }
    mediaRecorder = null
  }
}

// 获取百度云 Access Token
const getBaiduAccessToken = async () => {
  if (accessToken) return accessToken
  try {
    const response = await fetch(
      `https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=${BAIDU_API_KEY}&client_secret=${BAIDU_SECRET_KEY}`,
      { method: 'POST' }
    )
    const data = await response.json()
    if (data.access_token) {
      accessToken = data.access_token
      return accessToken
    } else {
      throw new Error('获取 Access Token 失败')
    }
  } catch (error) {
    console.error('百度云 Access Token 获取失败:', error)
    showMessage({
      message: '百度云语音识别授权失败，请检查 API Key 和 Secret Key',
      type: 'error'
    })
    return null
  }
}

// 简化的麦克风检测逻辑
const checkAndRequestMicrophone = async () => {
  if (hasMicrophone.value === false) {
    return false
  }

  isLoading.value = true

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    hasMicrophone.value = true
    // 注意：这里不再自动停止流，因为后续录音可能需要复用，但为了清晰，每次录音都会重新获取
    stream.getTracks().forEach(track => track.stop())
    isLoading.value = false
    return true
  } catch (error) {
    console.warn('麦克风访问失败:', error)
    hasMicrophone.value = false

    let errorMsg = '无法访问麦克风。请确保已授予浏览器麦克风权限，并检查设备是否有可用的麦克风。'
    if (error.name === 'NotAllowedError') {
      errorMsg = '麦克风权限已被拒绝。请在浏览器设置中重新授予权限。'
    } else if (error.name === 'NotFoundError') {
      errorMsg = '未检测到麦克风设备。'
    }

    showMessage({
      message: errorMsg,
      type: 'error'
    })

    isLoading.value = false
    return false
  }
}

// 初始化音频录制
const initMediaRecorder = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      isLoading.value = true
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
      console.log(audioBlob)
      await recognizeAudio(audioBlob)
      stream.getTracks().forEach(track => track.stop())
    }

    mediaRecorder.onerror = (error) => {
      console.error('录音出错:', error)
      showMessage({ message: '录音过程出错，请重试', type: 'error' })
      resetToInitialState()
    }

    return mediaRecorder
  } catch (error) {
    console.error('音频录制初始化失败:', error)
    showMessage({
      message: '录音初始化失败，请检查麦克风权限',
      type: 'error'
    })
    return null
  }
}

// 百度云语音识别
const recognizeAudio = async (audioBlob) => {
  try {
    const token = await getBaiduAccessToken()
    if (!token) {
      isLoading.value = false
      return
    }

    const formData = new FormData()
    formData.append('audio', audioBlob, 'recording.webm')

    const response = await fetch(
      `https://vop.baidu.com/server_api?dev_pid=1537&cuid=${Math.random().toString(36).substr(2, 15)}&token=${token}`,
      {
        method: 'POST',
        body: formData
      }
    )

    const data = await response.json()
    if (data.result) {
      const result = data.result[0].trim()
      emit('result', result)
      showMessage({
        message: '识别成功！',
        type: 'success'
      })
    } else {
      throw new Error(data.err_msg || '识别失败')
    }
  } catch (error) {
    console.error('语音识别失败:', error)
    const errorMsg = `识别失败: ${error.message}`
    emit('error', errorMsg)
    showMessage({
      message: errorMsg,
      type: 'error'
    })
  } finally {
    isLoading.value = false
    audioChunks = []
  }
}

// 开始录音
const startRecord = async () => {
  if (isRecording.value || isLoading.value) return

  isLoading.value = true
  try {
    const recorder = await initMediaRecorder()
    if (!recorder) {
      isLoading.value = false
      return
    }

    recorder.start()
    isRecording.value = true
    showMessage({
      message: '开始录音...',
      type: 'info',
      duration: 1500
    })
  } catch (error) {
    console.error('开始录音失败:', error)
    showMessage({
      message: '录音启动失败，请重试',
      type: 'error'
    })
  } finally {
    isLoading.value = false
  }
}

// 停止录音
const stopRecord = () => {
  if (!isRecording.value || !mediaRecorder || isLoading.value) return

  mediaRecorder.stop()
  isRecording.value = false
  showMessage({
    message: '录音已停止，正在识别...',
    type: 'success',
    duration: 1500
  })
}

// 【核心修改】处理鼠标/触摸开始事件
const handleRecordStart = async (e) => {
  e.preventDefault()
  e.stopPropagation()

  resetToInitialState()

  if (hasMicrophone.value === null) {
    // 首次按下：请求权限
    try {
      await ElMessageBox.confirm(
        '需要访问您的麦克风以进行语音识别，请在后续弹窗中点击「允许」授予权限',
        '麦克风权限请求',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'info',
          showCancelButton: true,
          closeOnClickModal: false,
          closeOnPressEscape: false
        }
      )

      const isAuthorized = await checkAndRequestMicrophone()
      if (isAuthorized) {
        // 权限获取成功后，仅提示，不自动录音
        showMessage({
          message: '麦克风权限已获取，请再次按住按钮开始录音。',
          type: 'success'
        });
      }
    } catch (error) {
      resetToInitialState()
      // 用户点击取消按钮
      if (error === 'cancel') {
        showMessage({
          message: '已取消麦克风权限请求，无法进行语音识别。',
          type: 'warning'
        });
      } else {
        // 其他错误
        showMessage({
          message: '请求麦克风权限失败。',
          type: 'error'
        });
      }
    }
  } else if (hasMicrophone.value === true) {
    // 已授权：直接开始录音
    await startRecord()
  } else {
    // 未授权
    resetToInitialState()
    showMessage({
      message: '麦克风未授权，请在浏览器设置中重新授予权限。',
      type: 'error'
    })
  }
}

// 处理鼠标/触摸结束事件
const handleRecordStop = (e) => {
  e.preventDefault()
  e.stopPropagation()

  if (isRecording.value && !isLoading.value) {
    stopRecord()
  }
}

// 处理按钮 click 事件
const handleButtonClick = async (e) => {
  e.preventDefault()
  await handleRecordStart(e)
}

// 组件卸载时，清理资源
onUnmounted(() => {
  ElMessage.closeAll()
  resetToInitialState()
})
</script>

<style scoped>
/* 外层容器 */
.voice-recorder-wrapper {
  position: relative;
  display: inline-block;
  margin-top: 2px;
}

/* 原按钮样式 - 已调整为 36px */
.voice-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  overflow: hidden;
  -webkit-tap-highlight-color: transparent;
}
.voice-btn:not(.is-recording):not(.no-microphone):not(.is-loading):hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}
.voice-btn.is-recording {
  background-color: #f56c6c;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
}

/* 麦克风不可用时的按钮样式 */
.voice-btn.no-microphone {
  background-color: #c0c4cc;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: not-allowed;
}
.voice-btn.no-microphone:hover {
  transform: none;
}
.voice-btn.no-microphone .voice-icon {
  color: #909399;
}

/* 按钮加载状态 */
.voice-btn.is-loading {
  background-color: #c0c4cc;
  cursor: wait;
}
.voice-btn.is-loading .voice-icon {
  display: none;
}

/* 图标样式 - 已调整为 18px */
.voice-icon {
  font-size: 18px;
  z-index: 3;
  position: relative;
}

/* 加载动画 */
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}
.loading-dot {
  width: 7px;
  height: 7px;
  margin: 0 1.5px;
  background-color: #fff;
  border-radius: 50%;
  animation: loading-pulse 1.4s infinite ease-in-out both;
}
.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes loading-pulse {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 波浪动画样式 */
.wave-container, .bg-wave-container, .wave, .bg-wave {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wave {
  width: 70%;
  height: 70%;
  background: radial-gradient(circle, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 70%);
  animation: wave-pulse 2s infinite ease-out;
  opacity: 0;
}
.wave-1 { animation-delay: 0s; }
.wave-2 { animation-delay: 0.8s; }
@keyframes wave-pulse {
  0% { transform: scale(0.9); opacity: 0; }
  40% { opacity: 1; }
  80% { transform: scale(1.8); opacity: 0; }
  100% { transform: scale(2.0); opacity: 0; }
}
.bg-wave {
  background-color: rgba(255,255,255,0.1);
  animation: bg-wave-pulse 4s infinite ease-in-out;
}
.bg-wave-1 { animation-delay: 0s; }
.bg-wave-2 { animation-delay: 2s; }
@keyframes bg-wave-pulse {
  0% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.15); opacity: 0.7; }
  100% { transform: scale(1); opacity: 0.3; }
}
</style>