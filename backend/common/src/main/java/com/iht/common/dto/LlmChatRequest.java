package com.iht.common.dto;

/**
 * chat-service가 llm-gateway-service에 답변 생성을 요청할 때 보내는 요청 본문.
 *
 * provider : 사용할 LLM. "claude" 또는 "openai" (대소문자 구분)
 * context  : 약관 전체 텍스트. LLM이 답변의 근거로 삼는 자료
 * question : 사용자가 입력한 자연어 질문
 */
public record LlmChatRequest(
        String provider,
        String context,
        String question
) {}
