export const CHART_LANGS = new Set(['echarts', 'ech']);

const SUGGESTED_QUESTION_RE = /<suggested_start>[\s\S]*?<suggested_end>/g;
export const TRANSITION_LINE_RE = /^\s*Transition\s*[:：].*(?:\r?\n|$)/gm;

export const cleanAssistantText = (text = '') => String(text)
  .replace(SUGGESTED_QUESTION_RE, '')
  .replace(TRANSITION_LINE_RE, '')
  .trim();

export const stripThinkBlocks = (text = '') => {
  return splitTextByThinkBlocks(text)
    .filter((segment) => segment.type === 'text')
    .map((segment) => segment.content)
    .join('');
};

export const createMessageItem = ({
  text = '',
  uuid = '',
  isThought = false,
  thoughtCollapsed = false,
  hasFinishedThinking = false
} = {}) => ({
  text,
  uuid,
  isThought,
  thoughtCollapsed,
  hasFinishedThinking
});

export const splitTextByThinkBlocks = (text = '') => {
  const segments = [];
  let cursor = 0;
  const source = String(text);

  while (cursor < source.length) {
    const start = source.indexOf('<think>', cursor);
    if (start === -1) {
      const content = source.slice(cursor);
      if (content) segments.push({ type: 'text', content });
      break;
    }

    if (start > cursor) {
      segments.push({ type: 'text', content: source.slice(cursor, start) });
    }

    const end = source.indexOf('</think>', start + 7);
    if (end === -1) {
      segments.push({ type: 'think', content: source.slice(start + 7), isClosed: false });
      break;
    }

    segments.push({ type: 'think', content: source.slice(start + 7, end), isClosed: true });
    cursor = end + 8;
  }

  return segments;
};

export const splitTextByCodeFences = (text = '') => {
  const segments = [];
  const source = String(text);
  const fenceRe = /(```|~~~)([^\n\r`]*)?[\r\n]?/g;
  let cursor = 0;

  while (cursor < source.length) {
    fenceRe.lastIndex = cursor;
    const open = fenceRe.exec(source);
    if (!open) {
      const content = source.slice(cursor);
      if (content) segments.push({ type: 'markdown', text: content });
      break;
    }

    if (open.index > cursor) {
      segments.push({ type: 'markdown', text: source.slice(cursor, open.index) });
    }

    const fence = open[1];
    const lang = (open[2] || '').trim().split(/\s+/)[0].toLowerCase();
    const closeRe = new RegExp(`(?:\\r?\\n)?${fence}`);
    const rest = source.slice(fenceRe.lastIndex);
    const close = closeRe.exec(rest);

    if (!close) {
      segments.push({
        type: CHART_LANGS.has(lang) ? 'chart' : 'code',
        lang,
        text: rest,
        raw: rest,
        status: 'streaming'
      });
      break;
    }

    const body = rest.slice(0, close.index);
    segments.push({
      type: CHART_LANGS.has(lang) ? 'chart' : 'code',
      lang,
      text: body,
      raw: body,
      status: 'complete'
    });
    cursor = fenceRe.lastIndex + close.index + close[0].length;
  }

  return segments;
};

export const segmentMessageItems = (items = [], { isStreaming = false } = {}) => {
  return items.flatMap((item, itemIndex) => {
    const idBase = item?.uuid || `item-${itemIndex}`;
    if (item?.isThought) {
      return [];
    }

    const text = cleanAssistantText(stripThinkBlocks(item?.text || ''));
    if (!text || text === '```') return [];

    return splitTextByCodeFences(text).map((segment, segmentIndex) => ({
      id: `${idBase}-${segmentIndex}`,
      ...segment
    }));
  });
};

export const buildMessageItemsFromRawText = (text = [], uuidFactory = () => '') => {
  const raw = String(text || '').replace(TRANSITION_LINE_RE, '');
  const blocks = splitTextByThinkBlocks(raw);

  return blocks.flatMap((block) => {
    if (block.type === 'think') {
      return [];
    }

    return splitTextByCodeFences(block.content).map((segment) => {
      const fence = segment.type === 'markdown'
        ? segment.text
        : `\`\`\`${segment.lang || ''}\n${segment.text}${segment.status === 'complete' ? '\n```' : ''}`;
      return createMessageItem({
        text: fence,
        uuid: uuidFactory(),
        isThought: false
      });
    });
  });
};
