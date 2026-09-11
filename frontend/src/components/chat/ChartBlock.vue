<template>
  <section class="chart-block" :class="{ 'is-loading': isStreaming, 'has-error': parseError }">
    <header class="chart-header">
      <div>
        <div class="chart-title">{{ chartTitle }}</div>
      </div>
      <button type="button" class="config-btn" @click="showConfig = !showConfig">
        {{ showConfig ? '隐藏配置' : '查看配置' }}
      </button>
    </header>

    <div v-if="isStreaming" class="chart-loading">
      <div class="loading-bar"></div>
      <span>正在生成图表配置...</span>
    </div>

    <div v-else-if="parseError" class="chart-error">
      <strong>图表配置解析失败</strong>
      <span>{{ parseError }}</span>
    </div>

    <VChart
      v-else
      class="chart-canvas"
      :class="{ 'chart-canvas-tall': tall, 'chart-canvas-pie': pieLike }"
      :option="finalOption"
      autoresize
    />

    <pre v-if="showConfig" class="chart-config"><code>{{ formattedConfig }}</code></pre>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import VChart from 'vue-echarts';
import '@/utils/chartRuntime';
import { buildChartOption, hasLongAxisLabel, isPieLikeChart, parseChartOption } from '@/utils/chartOptions';

const props = defineProps({
  raw: {
    type: String,
    default: ''
  },
  status: {
    type: String,
    default: 'complete'
  }
});

const showConfig = ref(false);

const isStreaming = computed(() => props.status === 'streaming');

const parsedOption = computed(() => {
  if (isStreaming.value) return null;
  try {
    return parseChartOption(props.raw);
  } catch (error) {
    return { __error: error.message };
  }
});

const parseError = computed(() => parsedOption.value?.__error || '');

const mobile = computed(() => window.innerWidth < 768);

const finalOption = computed(() => {
  if (!parsedOption.value || parseError.value) return {};
  return buildChartOption(parsedOption.value, { mobile: mobile.value });
});

const pieLike = computed(() => parsedOption.value && !parseError.value && isPieLikeChart(parsedOption.value));

const formattedConfig = computed(() => {
  if (parsedOption.value && !parseError.value) {
    return JSON.stringify(parsedOption.value, null, 2);
  }
  return props.raw || '';
});

const tall = computed(() => parsedOption.value && !parseError.value && hasLongAxisLabel(parsedOption.value));

const chartTitle = computed(() => {
  const title = parsedOption.value?.title;
  if (typeof title?.text === 'string' && title.text.trim()) return title.text.trim();
  return 'ECharts 图表';
});

const chartSubtitle = computed(() => {
  if (isStreaming.value) return '配置生成中';
  if (parseError.value) return '请检查 ```echarts 代码块是否为合法 JSON';
  return '可交互预览';
});
</script>

<style scoped>
.chart-block {
  overflow: hidden;
  margin: 1rem 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
  padding: 11px 14px;
}

.chart-title {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.config-btn {
  flex: 0 0 auto;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  font-size: 12px;
  padding: 6px 9px;
}

.config-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.chart-canvas {
  width: 100%;
  height: 500px;
}

.chart-canvas-tall {
  height: 720px;
}

.chart-canvas-pie {
  height: 440px;
}

.chart-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 360px;
  color: #475569;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.loading-bar {
  width: 38px;
  height: 38px;
  border: 4px solid #dbeafe;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.chart-error {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 180px;
  justify-content: center;
  padding: 20px;
  color: #991b1b;
  background: #fef2f2;
}

.chart-config {
  max-height: 260px;
  overflow: auto;
  margin: 0;
  border-top: 1px solid #dbeafe;
  background: #ffffff;
  padding: 14px 16px;
}

.chart-config,
.chart-config code {
  color: #0f172a !important;
  font-family: Consolas, Monaco, 'Courier New', monospace !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  line-height: 1.72 !important;
  text-shadow: none !important;
  -webkit-font-smoothing: antialiased;
  text-rendering: geometricPrecision;
}

.chart-config code {
  display: block;
  white-space: pre;
  background: transparent !important;
  border: 0 !important;
  padding: 0 !important;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .chart-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .chart-canvas {
    height: 460px;
  }

  .chart-canvas-pie {
    height: 340px;
  }

  .chart-canvas-tall {
    height: 620px;
  }
}
</style>
