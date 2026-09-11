<template>
  <section class="thought-block">
    <button type="button" class="thought-header" @click="collapsed = !collapsed">
      <span class="thought-title">{{ status === 'streaming' ? '正在深度思考...' : '深度思考过程' }}</span>
      <span class="thought-state">{{ collapsed ? '展开' : '收起' }}</span>
    </button>
    <div v-show="!collapsed" class="thought-body">
      <MarkdownBlock :content="content" />
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue';
import MarkdownBlock from './MarkdownBlock.vue';

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  collapsed: {
    type: Boolean,
    default: false
  },
  status: {
    type: String,
    default: 'complete'
  }
});

const collapsed = ref(props.collapsed);

watch(() => props.collapsed, (value) => {
  collapsed.value = value;
});
</script>

<style scoped>
.thought-block {
  overflow: hidden;
  margin: 0.75rem 0;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #f8fafc;
}

.thought-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: 0;
  background: transparent;
  color: #334155;
  cursor: pointer;
  padding: 10px 12px;
  text-align: left;
}

.thought-title {
  font-size: 13px;
  font-weight: 700;
}

.thought-state {
  color: #64748b;
  font-size: 12px;
}

.thought-body {
  border-top: 1px solid #e2e8f0;
  padding: 12px;
}
</style>
