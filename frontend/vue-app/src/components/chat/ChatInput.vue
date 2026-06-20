<script setup>
import { ref } from 'vue'

/**
 * 메시지 입력창.
 * emits:
 *  - 'send' (text) : 사용자가 Enter(또는 전송 버튼)로 메시지를 보낼 때, 입력된 텍스트를 전달
 *
 * props:
 *  - disabled : true면 입력/전송을 막는다 (답변 대기 중일 때 사용)
 */
const props = defineProps({
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['send'])

const text = ref('')

function submit() {
  const value = text.value.trim()
  if (!value || props.disabled) return
  emit('send', value)
  text.value = ''
}

/**
 * Enter는 전송, Shift+Enter는 줄바꿈으로 동작하게 한다 (채팅 UI의 일반적인 관례).
 */
function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <div class="input-bar">
    <textarea
      v-model="text"
      class="input"
      rows="1"
      placeholder="약관에 대해 무엇이든 물어보세요"
      :disabled="disabled"
      @keydown="onKeydown"
    ></textarea>
    <button class="send-btn" type="button" :disabled="disabled || !text.trim()" @click="submit">
      보내기
    </button>
  </div>
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

.input {
  flex: 1;
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
</style>
