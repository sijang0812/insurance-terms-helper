import apiClient from './client'

/**
 * 약관에 대한 질문을 API Gateway를 통해 chat-service로 전송한다.
 *
 * IN  : documentId - 질문 대상 문서 ID
 *       question   - 사용자가 입력한 질문 (이번 turn)
 *       history    - 이전까지의 대화 turn 목록. [{role: 'user'|'assistant', content}, ...]
 *                    LLM은 무상태이므로 이전 맥락을 이해시키려면 매번 함께 보내야 한다.
 *       provider   - 사용할 LLM ("claude" | "openai"), 생략 시 백엔드 기본값(claude) 사용
 * OUT : Promise<{answer, provider}>
 */
export async function sendChatMessage(documentId, question, history = [], provider) {
  const response = await apiClient.post('/api/chat', {
    documentId,
    question,
    provider,
    history,
  })

  return response.data.data
}
