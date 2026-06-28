package com.iht.common.dto;

import java.util.List;

/**
 * chat-service가 llm-gateway-service에 답변 생성을 요청할 때 보내는 요청 본문.
 *
 * provider : 사용할 LLM. "claude" 또는 "openai" (대소문자 구분)
 * context  : 약관 전체 텍스트. LLM이 답변의 근거로 삼는 자료
 * question : 사용자가 입력한 자연어 질문 (이번 turn)
 * history  : 이전까지의 대화 turn 목록. LLM이 무상태이기 때문에, 이전 대화 맥락을
 *            이해시키려면 매번 이 목록을 함께 보내야 한다. 첫 질문이면 빈 리스트.
 */
public record LlmChatRequest(
        String provider,
        String context,
        String question,
        List<ChatTurn> history
) {}
