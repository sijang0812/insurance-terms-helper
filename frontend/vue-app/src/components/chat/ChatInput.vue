<script setup>
import { ref, computed } from 'vue'

const MAX_BYTES = 220
// 이 글자 수 이하면 짧은 질문 힌트를 표시한다 (한국어 약 5~6단어 이하)
const SHORT_QUERY_CHARS = 10

const props = defineProps({
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['send'])

const text = ref('')

const byteLength = computed(() => new TextEncoder().encode(text.value).length)
const isOverLimit = computed(() => byteLength.value > MAX_BYTES)
const isShortQuery = computed(() => {
  const trimmed = text.value.trim()
  return trimmed.length > 0 && trimmed.length <= SHORT_QUERY_CHARS
})

function submit() {
  const value = text.value.trim()
  if (!value || props.disabled || isOverLimit.value) return
  emit('send', value)
  text.value = ''
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <div class="input-bar">
    <div class="input-wrap">
      <textarea
        v-model="text"
        class="input"
        rows="1"
        placeholder="약관에 대해 무엇이든 물어보세요"
        :disabled="disabled"
        @keydown="onKeydown"
      ></textarea>
      <span class="byte-counter" :class="{ over: isOverLimit }">
        {{ byteLength }} / {{ MAX_BYTES }}B
      </span>
    </div>
    <button class="send-btn" type="button" :disabled="disabled || !text.trim() || isOverLimit" @click="submit">
      보내기
    </button>
  </div>
  <p v-if="isShortQuery" class="short-query-hint">
    짧은 질문은 검색이 잘 안 될 수 있어요. 예: <em>'뇌졸중 진단비 얼마야?'</em>처럼 구체적으로 물어보세요.
  </p>
</template>

<style scoped>
.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.input-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.input {
  resize: none;
  border: none;
  outline: none;
  font-family: var(--font-body);
  font-size: 14.5px;
  line-height: 1.6;
  max-height: 120px;
  padding: 8px 6px;
  color: var(--color-ink);
}

.byte-counter {
  font-size: 11px;
  color: var(--color-ink-faint);
  text-align: right;
  padding: 0 6px 2px;
}

.byte-counter.over {
  color: #e53e3e;
  font-weight: 600;
}

.input::placeholder {
  color: var(--color-ink-faint);
}

.send-btn {
  flex: none;
  background: var(--color-brand);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.send-btn:disabled {
  background: var(--color-border);
  color: var(--color-ink-faint);
  cursor: not-allowed;
}

.short-query-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-ink-soft);
  padding: 0 4px;
}

.short-query-hint em {
  font-style: normal;
  font-weight: 500;
  color: var(--color-ink);
}
</style>
