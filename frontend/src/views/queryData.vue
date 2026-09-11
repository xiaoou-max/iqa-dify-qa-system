<template>
  <div class="query-page" v-loading.fullscreen.lock="Httploading">
    <!-- 页面标题和返回按钮 -->
    <div class="page-header">
      <el-button type="default" @click="goHome" class="home-btn">
        <el-icon class="icon-left"><ArrowLeft /></el-icon>
        返回首页
      </el-button>
      <h2 class="page-title">料品信息查询 - {{ currentTitle }}</h2>
    </div>

    <!-- 搜索区域（固定定位） -->
    <div class="sticky-wrapper">
      <div class="query-container">
        <el-card class="query-card">
          <el-form 
            ref="queryFormRef"
            :model="queryForm" 
            class="query-form"
            label-width="110px"
          >
            <!-- 动态生成查询条件 -->
            <el-row :gutter="10">
              <el-col 
                v-for="field in currentSearchFields" 
                :key="field.prop" 
                :xs="24" :sm="12" :md="8" :lg="6" :xl="6"
              >
                <el-form-item :label="field.label" :prop="field.prop">
                  <!-- 特殊处理：料品分类下拉框 -->
                  <el-select 
                    v-if="field.prop === 'lpfl'"
                    v-model="queryForm.lpfl" 
                    :placeholder="'请选择' + field.label"
                    clearable
                    filterable
                    class="query-select"
                    :disabled="categoryLoading"
                  >
                    <el-option 
                      v-for="category in allCategories" 
                      :key="category" 
                      :label="category" 
                      :value="category"
                    ></el-option>
                  </el-select>
                  
                  <!-- 默认文本框 -->
                  <el-input 
                    v-else
                    v-model="queryForm[field.prop]" 
                    :placeholder="'请输入' + field.label" 
                    clearable
                    class="query-input"
                  ></el-input>
                </el-form-item>
              </el-col>
            </el-row>
            
            <div class="button-group">
              <el-button type="primary" @click="handleQuery" class="query-btn">
                <el-icon class="icon-search"><Search /></el-icon>
                查询
              </el-button>
              <el-button @click="resetForm" class="reset-btn">
                <el-icon class="icon-refresh"><Refresh /></el-icon>
                重置
              </el-button>
            </div>
          </el-form>
        </el-card>
      </div>
    </div>

    <!-- 查询结果区域 -->
    <div class="result-container">
      <el-card class="result-card">
        <div class="result-header">
          <h3 class="result-title">查询结果</h3>
          <div class="result-count">共 <span class="count-number">{{ totalCount }}</span> 条记录</div>
        </div>
        
        <!-- 操作提示条 -->
        <div 
          class="operation-hint" 
          :class="{ 'select-hint': isSelecting.value, 'drag-hint': isDragging }"
          v-if="showHint"
        >
          <template v-if="isSelecting.value">
            <el-icon class="hint-icon"><DocumentCopy /></el-icon>
            选择模式：可复制文本 | 按ESC退出
          </template>
          <template v-if="isDragging">
            <el-icon class="hint-icon"><Mouse /></el-icon>
            拖曳模式：拖动可滚动表格 | 松开退出
          </template>
        </div>
        
        <div class="table-wrapper">
          <el-table 
            :data="tableData" 
            border 
            stripe 
            :header-cell-style="headerCellStyle"
            :cell-style="cellStyle"
            :row-class-name="rowClassName"
            v-loading="loading"
            element-loading-text="正在加载数据..."
            @cell-dblclick="handleCellDblClick"
            @click="handleTableClick"
            :class="{ 'select-mode': isSelecting.value }"
          >
            <!-- 动态生成表格列 -->
             <el-table-column 
              v-for="col in currentTableColumns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              :min-width="col.width || 110"
              :align="col.align || 'center'"
              show-overflow-tooltip
            >
              <template #default="scope">
                <!-- 金额格式化 -->
                <span v-if="col.type === 'money'">{{ formatNumber(scope.row[col.prop]) }}</span>
                <!-- 百分比格式化 -->
                <!-- <span v-else-if="col.type === 'percent'">{{ formatPercent(scope.row[col.prop]) }}</span> -->
                <!-- 默认显示 -->
                <span v-else>{{ scope.row[col.prop] }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div class="no-data" v-if="totalCount === 0 && !loading">
          <el-empty description="暂无查询结果"></el-empty>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import axios from '@/apis/request';
import { ElMessage, ElLoading } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Search, Refresh, DocumentCopy, Mouse } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();

// ---------------- 1. 字段定义字典 (Field Definitions) ----------------
// search: true 代表参与搜索，false 代表不参与
const fieldDefs = {
  // --- 通用/基础信息 ---
  lpfl: { label: '料品分类', search: true, width: 120 },
  mc:   { label: '名称', search: true, width: 150 },
  ggxh: { label: '规格型号', search: true, width: 180 },
  cz:   { label: '材质', search: true, width: 100 },
  pp:   { label: '品牌', search: true, width: 100 },
  lh:   { label: '料号', search: true, width: 120 },
  pm:   { label: '品名', search: true, width: 150 },
  
  // --- 价格与供应商 (不参与搜索) ---
  wsdj: { label: '未税单价', search: false, width: 100, type: 'money', align: 'right' },
  hsdj: { label: '含税单价', search: false, width: 100, type: 'money', align: 'right' },
  zxsl: { label: '执行税率', search: false, width: 80, type: 'percent' },
  ywrq: { label: '业务日期', search: false, width: 120 },
  gys:  { label: '供应商', search: false, width: 180 },

  // --- 管道管件特有 (gdgj) ---
  yldj:  { label: '压力等级', search: true, width: 100 },
  ljfs:  { label: '连接方式', search: true, width: 110 },
  bmcl:  { label: '表面处理', search: true, width: 110 },
  jgcc:  { label: '结构尺寸', search: true, width: 110 },
  cdbj:  { label: '长短半径/直段', search: true, width: 130 },
  jd:    { label: '角度', search: true, width: 80 },
  fllx:  { label: '法兰类型', search: true, width: 110 },
  mfmxs: { label: '密封面形式', search: true, width: 120 }, // 管道中用 mfmxs
  spgbh: { label: '适配管壁厚', search: true, width: 120 },
  lwxs:  { label: '螺纹形式', search: true, width: 110 },
  zxbz:  { label: '执行标准', search: true, width: 120 },

  // --- 阀门特有 (fm) ---
  mfmlx:  { label: '密封面类型', search: true, width: 120 }, // 阀门中用 mfmlx
  gzwd:   { label: '工作温度', search: true, width: 100 },
  zdgbyc: { label: '最大关闭压差', search: true, width: 120 },
  xldj:   { label: '泄露等级', search: true, width: 100 },
  jz:     { label: '介质', search: true, width: 100 },
  ftcz:   { label: '阀体材质', search: true, width: 110 },
  fbxcz:  { label: '阀板/芯材质', search: true, width: 120 },
  mfcz:   { label: '密封材质', search: true, width: 110 },
  zyfs:   { label: '作用方式', search: true, width: 110 },
  sffb:   { label: '是否防爆', search: true, width: 100 },
  zxqyq:  { label: '执行器要求', search: true, width: 120 },

  // --- 仪表特有 (yb) ---
  md:     { label: '密度', search: true, width: 100 },
  gzyl:   { label: '工作压力', search: true, width: 100 },
  lcfw:   { label: '量程范围', search: true, width: 120 },
  jycz:   { label: '接液材质', search: true, width: 110 },
  ljxs:   { label: '连接型式', search: true, width: 110 },
  gddy:   { label: '供电电压', search: true, width: 100 },
  hartxy: { label: 'HART协议', search: true, width: 100 },
  jdxs:   { label: '就地显示', search: true, width: 100 },
  fbdj:   { label: '防爆等级', search: true, width: 100 },
  fhdj:   { label: '防护等级', search: true, width: 100 },

  // --- 电气控制特有 (dqkz - 保留旧有/推测字段) ---
  gl:   { label: '功率', search: true, width: 80 },
  sj1:  { label: '数据1', search: true, width: 80 },
  sj2:  { label: '数据2', search: true, width: 80 },
};

// ---------------- 2. 模块配置 (Module Configs) ----------------
// 定义每个模块显示的字段及其顺序（与截图一致）
const moduleConfigs = {
  // 管道管件 (图1)
  gdgj: [
    'lpfl', 'mc', 'pm', 'ggxh', 'cz', 'yldj', 'ljfs', 'bmcl', 'jgcc', 'cdbj', 'jd', 
    'fllx', 'mfmxs', 'spgbh', 'lwxs', 'zxbz', // 属性字段
    'wsdj', 'hsdj', 'zxsl', 'ywrq', 'pp', 'gys', 'lh' // 公共底部字段
  ],
  
  // 阀门 (图2)
  fm: [
    'lpfl', 'mc', 'pm', 'ggxh', 'ljfs', 'mfmlx', 'yldj', 'gzwd', 'zdgbyc', 'xldj', 'jz', 
    'ftcz', 'fbxcz', 'mfcz', 'zyfs', 'sffb', 'zxqyq', 
    'wsdj', 'hsdj', 'zxsl', 'ywrq', 'pp', 'gys', 'lh'
  ],
  
  // 仪表 (图3)
  yb: [
    'lpfl', 'mc', 'pm', 'ggxh', 'jz', 'md', 'gzwd', 'gzyl', 'lcfw', 'jycz', 'ljxs', 
    'gddy', 'hartxy', 'jdxs', 'fbdj', 'fhdj', 
    'wsdj', 'hsdj', 'zxsl', 'ywrq', 'pp', 'gys', 'lh'
  ],
  
  // 电气控制 (dqkz) - 根据需求补充，包含执行标准
  dqkz: [
    'lpfl', 'mc', 'pm', 'ggxh', 'cz', 'zxbz', 'yldj', 'gl', // 假设的电气字段
    'wsdj', 'hsdj', 'zxsl', 'ywrq', 'pp', 'gys', 'lh'
  ],

  // 默认回退
  default: [
    'lpfl', 'lh', 'pm', 'mc', 'ggxh', 'cz', 'yldj', 
    'wsdj', 'hsdj', 'zxsl', 'ywrq', 'pp', 'gys'
  ]
};

// ---------------- 状态与逻辑 ----------------

const titlevalue = String(route.query.titlevalue || '');
const modelstate = String(route.query.modelState || '');
const currentTitle = ref(titlevalue || '通用');

// 计算当前使用的配置键值 (Key)
const currentConfigKey = computed(() => {
  if (titlevalue.includes('管道管件') || modelstate === 'jwgk_queryPriceDatas_gdgj') return 'gdgj';
  if (titlevalue.includes('阀门') || modelstate === 'jwgk_queryPriceDatas_fm') return 'fm';
  if (titlevalue.includes('仪表') || modelstate === 'jwgk_queryPriceDatas_yb') return 'yb';
  if (titlevalue.includes('电气控制') || modelstate === 'jwgk_queryPriceDatas_dqkz') return 'dqkz';
  return 'default';
});

// 计算当前所有表格列 (Columns)
const currentTableColumns = computed(() => {
  const keys = moduleConfigs[currentConfigKey.value] || moduleConfigs['default'];
  return keys.map(key => ({
    prop: key,
    ...fieldDefs[key] || { label: key, search: true } // 防止undefined
  }));
});

// 计算当前搜索字段 (Search Fields)
const currentSearchFields = computed(() => {
  return currentTableColumns.value.filter(col => col.search);
});

// 动态构建表单对象
const queryForm = reactive({});

// 当搜索字段变化时，初始化表单Model，防止v-model绑定失败
watch(currentSearchFields, (fields) => {
  fields.forEach(f => {
    if (!(f.prop in queryForm)) {
      queryForm[f.prop] = '';
    }
  });
}, { immediate: true });

// ---------------- 常规逻辑 ----------------

const queryFormRef = ref(null);
const allCategories = ref([]);
const categoryLoading = ref(false);
const tableData = ref([]);
const totalCount = ref(0);
const loading = ref(false);
const Httploading = ref(false);

const headerCellStyle = computed(() => ({
  'background-color': '#f5f7fa', 
  'color': '#303133', 
  'font-weight': '600', 
  'text-align': 'center',
  'border-bottom': '1px solid #e4e7ed',
  'height': '50px'
}));

const cellStyle = computed(() => ({ 
  'text-align': 'center', 
  'padding': '10px 0',
  'font-size': '13px',
  'transition': 'background-color 0.2s ease',
  ...(isSelecting.value ? { 'background-color': '#f0f7ff50' } : {})
}));

const rowClassName = ({ rowIndex }) => rowIndex % 2 === 0 ? 'even-row' : 'odd-row';

// 获取料品分类
const getCategories = async () => {
  categoryLoading.value = true;
  let tablename = 'cglpjgxx'; // 默认
  
  if(currentConfigKey.value === 'gdgj') tablename = 'cglpjgxx_gdgj';
  if(currentConfigKey.value === 'fm') tablename = 'cglpjgxx_fm';
  if(currentConfigKey.value === 'yb') tablename = 'cglpjgxx_yb';
  if(currentConfigKey.value === 'dqkz') tablename = 'cglpjgxx_dqkz';

  try {
    const response = await axios.get('/jwgk/getLpfls?tablename='+tablename);
    if (response.type === 'success' && Array.isArray(response.data)) {
      allCategories.value = response.data;
    } else {
      ElMessage.warning('获取料品分类失败');
    }
  } catch (error) {
    console.error('获取料品分类错误:', error);
  } finally {
    categoryLoading.value = false;
  }
};

// 执行查询
const handleQuery = async () => {
  try {
    loading.value = true;
    Httploading.value = true;
    
    // 确定表名
    let tablename = 'cglpjgxx';
    if(titlevalue.indexOf('管道管件')!=-1) tablename = 'cglpjgxx_gdgj';
    if(titlevalue.indexOf('阀门')!=-1) tablename = 'cglpjgxx_fm';
    if(titlevalue.indexOf('仪表')!=-1) tablename = 'cglpjgxx_yb';
    if(titlevalue.indexOf('电气控制')!=-1) tablename = 'cglpjgxx_dqkz';

    const url = '/jwgk/queryPriceDatas?tablename=' + tablename;
    
    // 构建参数：过滤空值
    const params = {};
    currentSearchFields.value.forEach(field => {
        if(queryForm[field.prop]) {
            params[field.prop] = queryForm[field.prop];
        }
    });

    // 校验必填 (可选：如果业务要求料品分类必选)
    // if (!params.lpfl && currentSearchFields.value.some(f => f.prop === 'lpfl')) {
    //     ElMessage.warning('请选择料品分类');
    //     return;
    // }

    const response = await axios.post(url, params);
    
    if (response.type === 'success' && response.data?.result) {
      tableData.value = response.data.result;
      totalCount.value = response.data.result.length;
      ElMessage.success('查询成功');
    } else {
      tableData.value = [];
      totalCount.value = 0;
      ElMessage.info('未查询到符合条件的数据');
    }
  } catch (error) {
    ElMessage.error('查询失败，请重试');
    console.error(error);
  } finally {
    loading.value = false;
    Httploading.value = false;
  }
};

const resetForm = () => {
  Object.keys(queryForm).forEach(key => queryForm[key] = '');
  tableData.value = [];
  totalCount.value = 0;
};

const goHome = () => router.replace('/home');
const formatNumber = (num) => (num == null || num === '') ? '-' : Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const formatPercent = (num) => (num == null || num === '') ? '-' : (Number(num) * 100).toFixed(0) + '%';


// -------------------------- 核心交互优化逻辑 (拖曳/复制) --------------------------
let scrollWrap = null;
let isMouseDown = false;
let isDragging = false;
let isSelecting = ref(false);
let startX = 0;
let startScrollLeft = 0;
let hintTimer = null;
const DRAG_THRESHOLD = 4;
const showHint = ref(false);

const showOperationHint = (duration = 3000) => {
  showHint.value = true;
  if (hintTimer) clearTimeout(hintTimer);
  hintTimer = setTimeout(() => { showHint.value = false; }, duration);
};

const exitSelectMode = () => {
  if (isSelecting.value) {
    isSelecting.value = false;
    window.getSelection().removeAllRanges();
    if (scrollWrap) scrollWrap.style.cursor = 'grab';
    showHint.value = false;
  }
};

const handleMouseDown = (e) => {
  if (!e.target.closest('.el-table__cell .cell') && !e.target.closest('.el-table__body')) return;
  if (!scrollWrap) return;
  if (isSelecting.value) { exitSelectMode(); return; }
  isMouseDown = true;
  isDragging = false;
  startX = e.clientX;
  startScrollLeft = scrollWrap.scrollLeft;
  scrollWrap.style.cursor = 'grab';
};

const handleMouseMove = (e) => {
  if (!scrollWrap || !isMouseDown) return;
  const deltaX = e.clientX - startX;
  if (Math.abs(deltaX) > DRAG_THRESHOLD) {
    isDragging = true;
    isSelecting.value = false;
    scrollWrap.style.cursor = 'grabbing';
    e.preventDefault();
    scrollWrap.scrollLeft = startScrollLeft - (deltaX * 1.2);
    showOperationHint();
  } else if (Math.abs(deltaX) > 0) {
    isDragging = false;
    scrollWrap.style.cursor = 'text';
  }
};

const handleMouseUp = () => {
  if (!scrollWrap) return;
  isMouseDown = false;
  if (isDragging) {
    isDragging = false;
    scrollWrap.style.cursor = 'grab';
    showHint.value = false;
    return;
  }
  const selection = window.getSelection();
  if (selection.toString().trim().length > 0 && selection.anchorNode?.closest('.el-table__cell .cell')) {
    isSelecting.value = true;
    scrollWrap.style.cursor = 'text';
    showOperationHint();
  } else {
    isSelecting.value = false;
    scrollWrap.style.cursor = 'grab';
  }
};

const handleCellDblClick = (row, column, cell) => {
  if (!cell) return;
  const textContainer = cell.querySelector('.cell');
  if (!textContainer) return;
  const selection = window.getSelection();
  const range = document.createRange();
  range.selectNodeContents(textContainer);
  selection.removeAllRanges();
  selection.addRange(range);
  isSelecting.value = true;
  scrollWrap.style.cursor = 'text';
  showOperationHint();
};

const handleTableClick = (e) => {
  if (isSelecting.value && e.target.classList.contains('el-table__body')) {
    exitSelectMode();
  }
};

const handleKeydown = (e) => {
  if (e.key === 'Escape' && isSelecting.value) exitSelectMode();
};

const bindEvents = () => {
  if (!scrollWrap) return;
  scrollWrap.addEventListener('mousedown', handleMouseDown);
  scrollWrap.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
  document.addEventListener('mouseleave', () => {
    isMouseDown = false; isDragging = false; if (scrollWrap) scrollWrap.style.cursor = 'grab'; showHint.value = false;
  });
  document.addEventListener('keydown', handleKeydown);
};

const unbindEvents = () => {
  if (!scrollWrap) return;
  scrollWrap.removeEventListener('mousedown', handleMouseDown);
  scrollWrap.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
  document.removeEventListener('keydown', handleKeydown);
  if (hintTimer) clearTimeout(hintTimer);
};

onMounted(() => {
  getCategories();
  nextTick(() => {
    scrollWrap = document.querySelector('.el-table .el-table__body-wrapper .el-scrollbar__wrap');
    if (scrollWrap) {
      scrollWrap.style.userSelect = 'text';
      scrollWrap.style.webkitUserSelect = 'text';
      scrollWrap.style.cursor = 'grab';
      bindEvents();
    }
  });
});

onUnmounted(() => { unbindEvents(); });
</script>

<style scoped>
/* 基础样式 */
.query-page {
  padding: 20px;
  background-color: #f9fafb;
  min-height: 100vh;
}
.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.home-btn {
  padding: 6px 12px;
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  transition: all 0.2s ease;
}
.home-btn:hover {
  background-color: #e9ecef;
  transform: translateY(-1px);
}
.icon-left { margin-right: 4px; }
.page-title {
  margin: 0;
  color: #1d2129;
  font-size: 20px;
  font-weight: 600;
}
.sticky-wrapper {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f9fafb;
  padding-bottom: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.query-card, .result-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
}
.query-form {
  padding: 24px;
}
/* 优化表单项样式 */
.el-form-item {
  margin-bottom: 18px; 
}
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}
.query-select, .query-input {
  width: 100%;
}
.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}
.query-btn, .reset-btn {
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s ease;
}
.icon-search, .icon-refresh { margin-right: 4px; }
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}
.result-title {
  margin: 0;
  color: #1d2129;
  font-size: 16px;
  font-weight: 500;
}
.result-count {
  color: #606266;
  font-size: 14px;
}
.count-number {
  color: #409eff;
  font-weight: 500;
  margin: 0 4px;
}
.table-wrapper {
  overflow-x: auto;
  padding: 16px 24px;
  max-width: 100%;
}
.el-table {
  border-radius: 8px;
  overflow: hidden;
  min-width: 100%;
}
.el-table.select-mode {
  border: 2px solid #409eff !important;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.08);
}
.el-table tr.even-row { background-color: #f9fafb; }
.el-table tr.odd-row { background-color: #fff; }
:deep(.el-table__cell .cell ::selection) {
  background-color: #e6f4ff;
  color: #096dd9;
}
.operation-hint {
  position: absolute;
  top: 16px;
  right: 24px;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  display: flex;
  align-items: center;
  z-index: 5;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.select-hint {
  background-color: #ecf5ff;
  color: #409eff;
  border: 1px solid #d9ecff;
}
.drag-hint {
  background-color: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}
.hint-icon { margin-right: 6px; }
.no-data {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .query-page { padding: 10px; }
  .query-form { padding: 16px; }
  .el-form-item { margin-bottom: 12px; }
  .button-group { justify-content: center; }
}
</style>