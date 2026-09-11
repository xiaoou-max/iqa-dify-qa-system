<template>
  <div class="markdown-block" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js/lib/core';
import sql from 'highlight.js/lib/languages/sql';
import plaintext from 'highlight.js/lib/languages/plaintext';

if (!hljs.getLanguage('sql')) {
  hljs.registerLanguage('sql', sql);
}

if (!hljs.getLanguage('plaintext')) {
  hljs.registerLanguage('plaintext', plaintext);
}

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
});

const renderer = new marked.Renderer();

renderer.code = (code) => {
  const lang = code.lang && hljs.getLanguage(code.lang) ? code.lang : 'plaintext';
  const highlighted = hljs.highlight(code.text || '', { language: lang }).value;
  return `
    <div class="code-block">
      <div class="code-header">${DOMPurify.sanitize(code.lang || 'text')}</div>
      <pre><code class="language-${lang}">${DOMPurify.sanitize(highlighted)}</code></pre>
    </div>
  `;
};

renderer.link = (link) => {
  const href = DOMPurify.sanitize(link.href || '#');
  const title = link.title ? ` title="${DOMPurify.sanitize(link.title)}"` : '';
  const text = DOMPurify.sanitize(link.text || href);
  return `<a href="${href}"${title} target="_blank" rel="noopener noreferrer">${text}</a>`;
};

const html = computed(() => {
  const parsed = marked.parse(props.content || '', {
    breaks: true,
    gfm: true,
    renderer
  });
  return DOMPurify.sanitize(parsed);
});
</script>

<style scoped>
.markdown-block {
  color: #1f2937;
  line-height: 1.78;
  overflow-wrap: anywhere;
}

.markdown-block :deep(p) {
  margin: 0.45rem 0;
}

.markdown-block :deep(p:first-child) {
  margin-top: 0;
}

.markdown-block :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-block :deep(ul),
.markdown-block :deep(ol) {
  padding-left: 1.25rem;
  margin: 0.55rem 0;
}

.markdown-block :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8rem 0;
  font-size: 0.92rem;
}

.markdown-block :deep(th),
.markdown-block :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  text-align: left;
}

.markdown-block :deep(th) {
  background: #f8fafc;
  color: #111827;
  font-weight: 700;
}

.markdown-block :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.markdown-block :deep(a:hover) {
  text-decoration: underline;
}

.markdown-block :deep(.code-block) {
  overflow: hidden;
  margin: 0.8rem 0;
  border: 1px solid #cfe3ff;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.markdown-block :deep(.code-header) {
  padding: 8px 12px;
  background: #eef6ff;
  color: #102a43;
  font-size: 12px;
  font-weight: 600;
}

.markdown-block :deep(pre) {
  margin: 0;
  overflow-x: auto !important;
  background: #f8fbff !important;
  padding: 16px !important;
  border-radius: 0 !important;
  scrollbar-color: #94a3b8 #eaf2ff;
  scrollbar-width: thin;
}

.markdown-block :deep(code) {
  color: #1f2937 !important;
  font-family: "JetBrains Mono", "Cascadia Code", Consolas, Monaco, "Courier New", monospace;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.72;
  white-space: pre;
  background: transparent !important;
  border: 0 !important;
  padding: 0 !important;
  text-shadow: none;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

.markdown-block :deep(.hljs-keyword),
.markdown-block :deep(.hljs-built_in),
.markdown-block :deep(.hljs-type) {
  color: #1d4ed8;
  font-weight: 700;
}

.markdown-block :deep(.hljs-string),
.markdown-block :deep(.hljs-symbol) {
  color: #15803d;
  font-weight: 600;
}

.markdown-block :deep(.hljs-number),
.markdown-block :deep(.hljs-literal) {
  color: #c2410c;
  font-weight: 600;
}

.markdown-block :deep(.hljs-title),
.markdown-block :deep(.hljs-function),
.markdown-block :deep(.hljs-name) {
  color: #7c3aed;
  font-weight: 700;
}

.markdown-block :deep(.hljs-params),
.markdown-block :deep(.hljs-variable),
.markdown-block :deep(.hljs-attr),
.markdown-block :deep(.hljs-attribute) {
  color: #0369a1;
  font-weight: 600;
}

.markdown-block :deep(.hljs-operator),
.markdown-block :deep(.hljs-punctuation) {
  color: #475569;
}

.markdown-block :deep(.hljs-comment) {
  color: #64748b;
  font-style: italic;
}
</style>
