import { defineStore } from 'pinia'
import { sendChatMessage } from '../api/chat'

let nextId = 1

/**
 * 채팅 메시지 목록과 전송 로직을 담당하는 스토어.
 * messages 배열의 각 항목: { id, role: 'user' | 'bot', text, pending? }
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    messages: [],
    sending: false,
  }),

  actions: {
    /**
     * 사용자 질문을 보내고 답변을 받아와 메시지 목록에 추가한다.
     * IN  : documentId - 질문 대상 문서 ID
     *       question   - 사용자가 입력한 질문
     * OUT : 없음 (messages 배열이 갱신된다)
     */
    async sendMessage(documentId, question) {
      // LLM은 무상태(stateless)라 매번 이전 대화 전체를 같이 보내야 맥락을 이해한다.
      // 화면에 표시 중인 메시지 목록을 백엔드가 요구하는 {role, content} 형태로 변환한다.
      // (반드시 사용자 메시지를 messages에 push 하기 "전"에 만들어야 이번 질문이 중복으로 안 들어간다)
      const history = this.messages
        .filter((m) => !m.pending && m.text)
        .map((m) => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text }))

      // 1) 사용자 메시지를 먼저 화면에 즉시 반영 (오른쪽에 표시될 메시지)
      this.messages.push({ id: nextId++, role: 'user', text: question })

      // 2) 답변이 오는 동안 보여줄 "입력 중" placeholder
      const pendingId = nextId++
      this.messages.push({ id: pendingId, role: 'bot', text: '', pending: true })
      this.sending = true

      try {
        const result = await sendChatMessage(documentId, question, history)
        console.log('[RAG] 선택된 청크 id:', result.chunkIds)
        const target = this.messages.find((m) => m.id === pendingId)
        target.text = result.answer
        target.pending = false
      } catch (error) {
        console.error('[chat error]', error?.response?.status, error?.response?.data, error?.message)
        const target = this.messages.find((m) => m.id === pendingId)
        target.text = '답변을 가져오지 못했습니다. 잠시 후 다시 시도해주세요.'
        target.pending = false
        target.error = true
      } finally {
        this.sending = false
      }
    },

    /** 새 문서로 넘어갈 때 이전 대화 내용을 초기화한다. */
    reset() {
      this.messages = []
      this.sending = false
    },
  },
})
