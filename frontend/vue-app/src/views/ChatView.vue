<script setup>
import { nextTick, ref, watch } from 'vue'
import { useDocumentStore } from '../store/documentStore'
import { useChatStore } from '../store/chatStore'
import ChatBubble from '../components/chat/ChatBubble.vue'
import ChatInput from '../components/chat/ChatInput.vue'

/**
 * 라우터로부터 documentId를 prop으로 받는다 (router/index.js에서 props: true로 설정해둠).
 */
const props = defineProps({
  documentId: { type: String, required: true },
})

const documentStore = useDocumentStore()
const chatStore = useChatStore()
const messageListEl = ref(null)

/**
 * 메시지를 전송한다. ChatInput에서 'send' 이벤트가 올라오면 호출된다.
 * IN  : question - 사용자가 입력한 질문
 * OUT : 없음
 */
function handleSend(question) {
  chatStore.sendMessage(props.documentId, question)
}

/** 새 메시지가 추가될 때마다 스크롤을 맨 아래로 내린다. */
watch(
  () => chatStore.messages.length,
  async () => {
    await nextTick()
    if (messageListEl.value) {
      messageListEl.value.scrollTop = messageListEl.value.scrollHeight
    }
  },
)
</script>

<template>
  <div class="page">
    <header class="topbar">
      <div class="doc-info">
        <span class="doc-name">{{ documentStore.fileName ?? '업로드한 약관' }}</span>
        <span v-if="documentStore.pageCount" class="doc-meta"
          >총 {{ documentStore.pageCount }}페이지</span
        >
      </div>
      <router-link to="/" class="new-upload-link">다른 약관 올리기</router-link>
    </header>

    <div ref="messageListEl" class="message-list">
      <div v-if="chatStore.messages.length === 0" class="empty-state">
        <p class="empty-title">무엇이든 물어보세요</p>
        <p class="empty-sub">
          예) "면책기간이 뭐예요?", "이 약관에서 보험금이 안 나오는 경우는 언제인가요?"
        </p>
      </div>

      <ChatBubble
        v-for="message in chatStore.messages"
        :key="message.id"
        :role="message.role"
        :text="message.text"
        :pending="message.pending"
        :error="message.error"
      />
    </div>

    <div class="input-area">
      <ChatInput :disabled="chatStore.sending" @send="handleSend" />
    </div>
  </div>
</template>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  max-width: 720px;
  margin: 0 auto;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--color-border);
}

.doc-info {
  display: flex;
  flex-direction: column;
}

.doc-name {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 15.5px;
  color: var(--color-ink);
}

.doc-meta {
  font-size: 12.5px;
  color: var(--color-ink-faint);
  margin-top: 2px;
}

.new-upload-link {
  font-size: 13px;
  color: var(--color-brand);
  text-decoration: none;
  font-weight: 500;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
}

.empty-state {
  text-align: center;
  margin-top: 64px;
  color: var(--color-ink-faint);
}

.empty-title {
  font-family: var(--font-display);
  font-size: 17px;
  color: var(--color-ink-soft);
  margin: 0 0 8px;
}

.empty-sub {
  font-size: 13.5px;
  margin: 0;
}

.input-area {
  padding: 16px 20px 22px;
}
</style>
