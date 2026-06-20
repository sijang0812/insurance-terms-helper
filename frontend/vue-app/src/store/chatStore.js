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
      // 1) 사용자 메시지를 먼저 화면에 즉시 반영 (오른쪽에 표시될 메시지)
      this.messages.push({ id: nextId++, role: 'user', text: question })

      // 2) 답변이 오는 동안 보여줄 "입력 중" placeholder
      const pendingId = nextId++
      this.messages.push({ id: pendingId, role: 'bot', text: '', pending: true })
      this.sending = true

      try {
        const result = await sendChatMessage(documentId, question)
        const target = this.messages.find((m) => m.id === pendingId)
        target.text = result.answer
        target.pending = false
      } catch (error) {
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
