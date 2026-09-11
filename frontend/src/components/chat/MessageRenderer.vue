<template>
  <div class="message-renderer">
    <template v-for="segment in segments" :key="segment.id">
      <ThoughtBlock
        v-if="segment.type === 'thought'"
        :content="segment.text"
        :collapsed="segment.collapsed"
        :status="segment.status"
      />
      <ChartBlock
        v-else-if="segment.type === 'chart'"
        :raw="segment.raw || segment.text"
        :status="segment.status"
      />
      <CodeBlock
        v-else-if="segment.type === 'code'"
        :code="segment.text"
        :lang="segment.lang"
      />
      <MarkdownBlock
        v-else
        :content="segment.text"
      />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { segmentMessageItems } from '@/utils/messageSegments';
import ChartBlock from './ChartBlock.vue';
import CodeBlock from './CodeBlock.vue';
import MarkdownBlock from './MarkdownBlock.vue';
import ThoughtBlock from './ThoughtBlock.vue';

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
});

const segments = computed(() => segmentMessageItems(props.items, { isStreaming: props.isStreaming }));
</script>

<style scoped>
.message-renderer {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  width: 100%;
}
</style>
