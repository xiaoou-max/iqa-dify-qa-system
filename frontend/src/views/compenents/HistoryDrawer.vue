<template>
    <el-dialog 
      v-model="visible" 
      :title="modelTitle" 
      :width="isMobile ? '90%' : '480px'"
      :close-on-click-modal="false"
      class="doubao-upload-dialog"
      :destroy-on-close="true"
    >
      <div 
        class="file-select-wrapper"
        @click="triggerFileSelect"
        :style="{ height: isMobile ? '180px' : '220px' }"
      >
        <input
          ref="fileInput"
          type="file"
          accept=".xls,.xlsx"
          class="file-input"
          @change="handleFileSelect"
        >
        <div class="select-content" :style="{ padding: isMobile ? '0 12px' : '0 20px' }">
          <!-- 未选文件状态 -->
          <div v-if="!selectedFile" class="unselected-state">
            <el-icon size="16" class="upload-icon"><Upload /></el-icon>
            <p class="hint-text" :style="{ fontSize: isMobile ? '15px' : '17px' }">点击选择Excel文件</p>
            <p class="format-text" :style="{ fontSize: isMobile ? '12px' : '14px' }">支持 .xls 和 .xlsx 格式</p>
          </div>
          
          <!-- 已选文件状态（添加动画） -->
          <div 
            v-if="selectedFile" 
            class="selected-state" 
            :style="{ 
              padding: isMobile ? '10px 12px' : '16px 20px',
              maxWidth: isMobile ? '95%' : '100%'
            }"
          >
            <el-icon size="16" class="doc-icon"><FileFilled /></el-icon>
            <div class="file-info">
              <p class="file-name" :style="{ fontSize: isMobile ? '12px' : '15px' }">{{ selectedFile.name }}</p>
              <p class="file-size" :style="{ fontSize: isMobile ? '11px' : '13px' }">{{ formatFileSize(selectedFile.size) }}</p>
            </div>
            <button 
              class="remove-btn" 
              @click.stop="clearFile" 
              :style="{ width: isMobile ? '28px' : '34px', height: isMobile ? '28px' : '34px' }"
            >
              <el-icon size="16" class="close-icon"><CircleClose /></el-icon>
            </button>
          </div>
        </div>
      </div>
  
      <template #footer>
        <div class="btn-group" :class="{ 'mobile-btn-group': isMobile }">
          <el-button 
            @click="visible = false"
            class="cancel-btn"
            :style="{ 
              padding: isMobile ? '7px 16px' : '9px 20px',
              fontSize: isMobile ? '12px' : '14px' 
            }"
          >
            取消
          </el-button>
          <el-button 
            type="primary" 
            @click="changeState" 
            :disabled="!selectedFile"
            class="confirm-btn"
            :style="{ 
              padding: isMobile ? '7px 22px' : '9px 28px',
              fontSize: isMobile ? '12px' : '14px' 
            }"
          >
            上传
          </el-button>
        </div>
      </template>
    </el-dialog>
  </template>
  
  <script setup>
  import { ref, watch,computed } from 'vue';
  import axios from '@/apis/request'
  import { ElLoading, ElMessage } from 'element-plus'
  
  const isMobile = ref(window.innerWidth < 768);
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth < 768;
  });
  
  const props = defineProps({
    show: {
      type: Boolean,
      default: false
    },
    modelState: { type: String, default: 'jwgk_fillPriceDatas' },
    modelTitle: { type: String, default: '' },
  });
  
  const emit = defineEmits(['update:show', 'upload-success', 'upload-fail']);
  
  const visible = computed({
    get: () => props.show,
    set: (val) => {
      clearFile()
      emit('update:show', val)
    }
  });

  const fileInput = ref(null);
  const selectedFile = ref(null);
  const errorMsg = ref('')
  const isUploading = ref(false)
  
  const triggerFileSelect = () => {
    fileInput.value?.click();
  };
  
  const handleFileSelect = (e) => {
    if (e.target.files.length && e.target.files.length > 0) {
      selectedFile.value = e.target.files[0];
      e.target.value = ''; 
    }
  };
  


  const clearFile = () => {
    selectedFile.value = null;
    if (fileInput.value) {
      fileInput.value.value = '';
    }
  };
  
  const formatFileSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };
  
  const changeState = () => {
    if (props.modelState.indexOf('importPriceDatas')>=0) {
      handleTestUpload()
    }
    if (props.modelState.indexOf('fillPriceDatas')>=0) {
      handleTestUploadAndDown()
    }
  }
  
  const handleTestUpload = async () => {
    if (!selectedFile.value) return;
    const loading = ElLoading.service({
      lock: true,
      text: '正在上传文件...',
      background: 'rgba(255, 255, 255, 0.7)'
    });
    errorMsg.value = '';
    isUploading.value = true;
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    if(props.modelState == 'jwgk_importPriceDatas_gdgj'){
      formData.append('tablename', 'cglpjgxx_gdgj');
    }
    if(props.modelState == 'jwgk_importPriceDatas_fm'){
      formData.append('tablename', 'cglpjgxx_fm');
    }
    if(props.modelState == 'jwgk_importPriceDatas_yb'){
      formData.append('tablename', 'cglpjgxx_yb');
    }
    if(props.modelState == 'jwgk_importPriceDatas_dqkz'){
      formData.append('tablename', 'cglpjgxx_dqkz');
    }
    if(props.modelState == 'jwgk_importPriceDatas'){
      formData.append('tablename', 'cglpjgxx');
    }
    const hasTablename = formData.has('tablename');
    if (!hasTablename) {
      ElMessage.error('缺少必填参数（请检查上传场景是否合法）');
      loading.close();
      isUploading.value = false;
      return; // 终止上传流程
    }
    axios.post('jwgk/importPriceDatas', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(res => {
      ElMessage({ message: res.data, type: 'success' })
      visible.value = false
    }).catch(err => {
      
    }).finally(() => {
      loading.close();
      isUploading.value = false;
    });
  };
  
  const handleTestUploadAndDown = () => {
    if (!selectedFile.value) return;
    const loading = ElLoading.service({
      lock: true,
      text: '正在处理文件...',
      background: 'rgba(255, 255, 255, 0.7)'
    });
    errorMsg.value = '';
    isUploading.value = true;

    const formData = new FormData();
    formData.append('file', selectedFile.value);
    if(props.modelState == 'jwgk_fillPriceDatas_gdgj'){
      formData.append('tablename', 'cglpjgxx_gdgj');
    }
    if(props.modelState == 'jwgk_fillPriceDatas_fm'){
      formData.append('tablename', 'cglpjgxx_fm');
    }
    if(props.modelState == 'jwgk_fillPriceDatas_yb'){
      formData.append('tablename', 'cglpjgxx_yb');
    }
    if(props.modelState == 'jwgk_fillPriceDatas_dqkz'){
      formData.append('tablename', 'cglpjgxx_dqkz');
    }
    if(props.modelState == 'jwgk_fillPriceDatas'){
      formData.append('tablename', 'cglpjgxx');
    }
    const hasTablename = formData.has('tablename');
    if (!hasTablename) {
      ElMessage.error('缺少必填参数（请检查上传场景是否合法）');
      loading.close();
      isUploading.value = false;
      return; // 终止上传流程
    }
    axios.post('jwgk/fillPriceDatas', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      responseType: 'blob'
    }).then(res => {
      
      ElMessage({
        message: '文件处理完成，正在下载...',
        type: 'success'
      });
      const blob = new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      let fileName = '样本匹配.xlsx';
      const contentDisposition = res.headers['content-disposition'];
      if (contentDisposition) {
        // 从响应头解析文件名（兼容不同后端格式）
        fileName = parseFileName(contentDisposition)
       
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      visible.value = false;
      clearFile();
      emit('upload-success', { fileName });
    }).catch(err => {
      try{
        const { data, headers } = err.response;
        const reader = new FileReader();
        reader.onload = () => {
          const errorData = JSON.parse(reader.result); // 转换为 JSON
          console.log('Blob 解析后的错误信息：', errorData);
          ElMessage.error(errorData.data)
        };
        reader.readAsText(data); // 读取 Blob 内容
      }catch(e){
        ElMessage.error('文件处理失败且解析错误信息失败，请重试！')
      }
       
  
    }).finally(() => {
      loading.close();
      isUploading.value = false;
    });
  };

  const parseFileName = (contentDisposition) => {
    if (!contentDisposition) return '未知文件.xlsx';
    
    // 正则匹配：提取filename*=utf-8''后面的URL编码部分
    const regex = /filename\*=utf-8''(.+)/;
    const match = contentDisposition.match(regex);
    
    if (match && match[1]) {
      // 对URL编码的字符串进行解码（核心步骤）
      return decodeURIComponent(match[1]);
    }
    
    // 兼容其他格式（如果正则匹配失败）
    return '未知文件.xlsx';
  };
  </script>
  
  <style scoped>
  .doubao-upload-dialog {
    --el-dialog-border-radius: 16px;
    --el-dialog-bg-color: #fff;
  }
  .doubao-upload-dialog.el-dialog--small {
    margin: 10px auto !important;
  }
  
  .file-select-wrapper {
    width: 100%;
    border: 2px dashed #d0d3d9;
    border-radius: 16px;
    background-color: #f0f2f7;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    margin-bottom: 24px;
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  }
  .file-select-wrapper:hover {
    border-color: #99c8ff;
    background-color: #ecf5ff;
    box-shadow: 0 5px 15px rgba(64, 158, 255, 0.15);
    transform: translateY(-2px);
  }
  
  .file-input {
    display: none;
  }
  
  .select-content {
    width: 100%;
    max-width: 340px;
  }
  
  .unselected-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }
  .upload-icon {
    font-size: 52px;
    color: #409eff;
    padding: 14px;
    background-color: rgba(64, 158, 255, 0.15);
    border-radius: 50%;
  }
  .hint-text {
    margin: 0;
    color: #303133;
    font-weight: 500;
  }
  .format-text {
    margin: 0;
    color: #606266;
  }
  
  /* 已选文件动画反馈 */
  .selected-state {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
    /* 选择后的入场动画 */
    animation: fileFadeIn 0.4s ease-out forwards;
    opacity: 0;
    transform: translateY(10px);
  }
  @keyframes fileFadeIn {
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  .selected-state:hover {
    transform: translateY(-2px);
    transition: transform 0.2s ease;
  }
  
  .doc-icon {
    font-size: 28px;
    color: #409eff;
    margin-right: 10px;
  }
  .file-info {
    flex: 1;
    text-align: left;
    overflow: hidden;
  }
  .file-name {
    margin: 0 0 4px 0;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .file-size {
    margin: 0;
    color: #606266;
  }
  
  /* 移除按钮动画 */
  .remove-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    color: #606266;
    cursor: pointer;
    border-radius: 50%;
    transition: all 0.2s ease;
  }
  .remove-btn:hover {
    background-color: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.15);
    transform: scale(1.1);
  }
  .close-icon {
    font-size: 16px;
  }
  
  .btn-group {
    display: flex;
    justify-content: flex-end;
    gap: 14px;
    width: 100%;
  }
  .mobile-btn-group {
    justify-content: space-between;
    gap: 8px;
  }
  
  .cancel-btn {
    color: #606266;
    background-color: #f5f7fa;
    border: 1px solid #e5e6eb;
    border-radius: 8px;
    transition: all 0.2s;
    flex: 1;
  }
  .cancel-btn:hover {
    color: #303133;
    background-color: #e9ebf0;
    border-color: #dcdfe6;
  }
  
  .confirm-btn {
    background-color: #409eff;
    border-color: #409eff;
    border-radius: 8px;
    font-weight: 500;
    box-shadow: 0 3px 8px rgba(64, 158, 255, 0.25);
    transition: all 0.2s;
    flex: 1;
  }
  .confirm-btn:hover {
    background-color: #3391ff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    transform: translateY(-1px);
  }
  .confirm-btn:disabled {
    background-color: #e6f4ff;
    border-color: #b3d8ff;
    color: #8cc5ff;
    cursor: not-allowed;
    box-shadow: none;
  }
  </style>