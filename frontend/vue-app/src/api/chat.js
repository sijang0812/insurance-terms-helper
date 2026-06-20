import apiClient from './client'

/**
 * 약관에 대한 질문을 API Gateway를 통해 chat-service로 전송한다.
 *
 * IN  : documentId - 질문 대상 문서 ID
 *       question   - 사용자가 입력한 질문
 *       provider   - 사용할 LLM ("claude" | "openai"), 생략 시 백엔드 기본값(claude) 사용
 * OUT : Promise<{answer, provider}>
 */
export async function sendChatMessage(documentId, question, provider) {
  const response = await apiClient.post('/api/chat', {
    documentId,
    question,
    provider,
  })

  return response.data.data
}
