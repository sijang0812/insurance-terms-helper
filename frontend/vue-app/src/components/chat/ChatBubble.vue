<script setup>
/**
 * 메시지 한 건을 말풍선으로 렌더링한다.
 * props:
 *  - role    : 'user' | 'bot' - user는 오른쪽, bot은 왼쪽에 정렬된다
 *  - text    : 표시할 텍스트
 *  - pending : true면 "답변 작성 중" 인디케이터를 보여준다 (bot 메시지에만 의미 있음)
 *  - error   : true면 에러 스타일(빨간 텍스트)로 표시한다
 */
defineProps({
  role: { type: String, required: true },
  text: { type: String, default: '' },
  pending: { type: Boolean, default: false },
  error: { type: Boolean, default: false },
})
</script>

<template>
  <div class="bubble-row" :class="role">
    <div class="bubble" :class="{ user: role === 'user', error }">
      <span v-if="pending" class="typing">
        <span class="dot"></span><span class="dot"></span><span class="dot"></span>
      </span>
      <span v-else>{{ text }}</span>
    </div>
  </div>
</template>

<style scoped>
.bubble-row {
  display: flex;
  margin: 6px 0;
}

.bubble-row.user {
  justify-content: flex-end;
}

.bubble-row.bot {
  justify-content: flex-start;
}

.bubble {
  max-width: 72%;
  padding: 11px 16px;
  border-radius: 16px;
  font-size: 14.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-ink);
}

.bubble.user {
  background: var(--color-brand);
  border-color: var(--color-brand);
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.bubble-row.bot .bubble {
  border-bottom-left-radius: 4px;
}

.bubble.error {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 14px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-ink-faint);
  animation: blink 1.1s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.15s;
}
.dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
  }
  40% {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .dot {
    animation: none;
  }
}
</style>
