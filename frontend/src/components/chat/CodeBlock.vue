<template>
  <div class="code-block">
    <div class="code-header">
      <span>{{ langLabel }}</span>
      <button type="button" class="copy-btn" @click="copyCode">复制</button>
    </div>
    <pre><code :class="`language-${language}`" v-html="highlighted"></code></pre>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js/lib/core';
import sql from 'highlight.js/lib/languages/sql';
import plaintext from 'highlight.js/lib/languages/plaintext';

if (!hljs.getLanguage('sql')) hljs.registerLanguage('sql', sql);
if (!hljs.getLanguage('plaintext')) hljs.registerLanguage('plaintext', plaintext);

const props = defineProps({
  code: {
    type: String,
    default: ''
  },
  lang: {
    type: String,
    default: ''
  }
});

const langLabel = computed(() => props.lang || 'text');

const language = computed(() => (props.lang && hljs.getLanguage(props.lang) ? props.lang : 'plaintext'));

const highlighted = computed(() => {
  const html = hljs.highlight(props.code || '', { language: language.value }).value;
  return DOMPurify.sanitize(html);
});

const copyCode = async () => {
  const text = props.code || '';
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
};
</script>

<style scoped>
.code-block {
  overflow: hidden;
  margin: 0.85rem 0;
  border: 1px solid #cfe3ff;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
  padding: 7px 10px 7px 12px;
  background: #eef6ff;
  color: #102a43;
  font-size: 12px;
  font-weight: 600;
}

.copy-btn {
  border: 1px solid #bdd7ff;
  border-radius: 6px;
  background: #ffffff;
  color: #1d4ed8;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  padding: 6px 8px;
}

.copy-btn:hover {
  border-color: #409eff;
  background: #e6f4ff;
  color: #0f4eb3;
}

pre {
  margin: 0;
  overflow-x: auto !important;
  background: #f8fbff !important;
  padding: 16px !important;
  border-radius: 0 !important;
  scrollbar-color: #94a3b8 #eaf2ff;
  scrollbar-width: thin;
}

pre::-webkit-scrollbar {
  height: 10px;
}

pre::-webkit-scrollbar-track {
  background: #eaf2ff;
}

pre::-webkit-scrollbar-thumb {
  border: 2px solid #eaf2ff;
  border-radius: 999px;
  background: #94a3b8;
}

code {
  display: block;
  color: #1f2937 !important;
  font-family: "JetBrains Mono", "Cascadia Code", Consolas, Monaco, "Courier New", monospace;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.72;
  tab-size: 2;
  white-space: pre;
  background: transparent !important;
  border: 0 !important;
  padding: 0 !important;
  text-shadow: none;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

code :deep(.hljs-keyword),
code :deep(.hljs-built_in),
code :deep(.hljs-type) {
  color: #1d4ed8;
  font-weight: 700;
}

code :deep(.hljs-string),
code :deep(.hljs-symbol) {
  color: #15803d;
  font-weight: 600;
}

code :deep(.hljs-number),
code :deep(.hljs-literal) {
  color: #c2410c;
  font-weight: 600;
}

code :deep(.hljs-title),
code :deep(.hljs-function),
code :deep(.hljs-name) {
  color: #7c3aed;
  font-weight: 700;
}

code :deep(.hljs-params),
code :deep(.hljs-variable),
code :deep(.hljs-attr),
code :deep(.hljs-attribute) {
  color: #0369a1;
  font-weight: 600;
}

code :deep(.hljs-operator),
code :deep(.hljs-punctuation) {
  color: #475569;
}

code :deep(.hljs-comment) {
  color: #64748b;
  font-style: italic;
}
</style>
